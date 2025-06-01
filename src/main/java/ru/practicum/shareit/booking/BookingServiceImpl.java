package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.SelfBookingException;
import ru.practicum.shareit.exception.UnavailableItemException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final Map<Long, Booking> bookings = new HashMap<>();
    private Long idCounter = 1L;
    private final UserService userService;
    private final ItemService itemService;

    @Override
    public BookingDto createBooking(BookingDto bookingDto, Long bookerId) {
        log.info("Creating booking for user {} on item {}", bookerId, bookingDto.getItemId());

        User booker = userService.getUserEntityById(bookerId);
        Item item = itemService.getItemEntityById(bookingDto.getItemId());

        // Проверка end > start
        if (bookingDto.getEnd().isBefore(bookingDto.getStart())) {
            throw new ValidationException("End time must be after start time");
        }

        // Проверка доступности вещи
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            log.warn("Item {} is not available for booking", item.getId());
            throw new UnavailableItemException("Item is not available for booking");
        }

        // Проверка, что владелец не бронирует свою вещь
        if (item.getOwner().getId().equals(bookerId)) {
            log.warn("User {} tried to book their own item {}", bookerId, item.getId());
            throw new SelfBookingException("Owner cannot book their own item");
        }

        // Проверка дат
        if (bookingDto.getStart().isAfter(bookingDto.getEnd()) ||
                bookingDto.getStart().isEqual(bookingDto.getEnd())) {
            log.warn("Invalid booking dates: start={}, end={}",
                    bookingDto.getStart(), bookingDto.getEnd());
            throw new ValidationException("Invalid booking dates");
        }

        Booking booking = BookingMapper.toBooking(bookingDto, item, booker);
        booking.setId(idCounter);
        booking.setStatus(BookingStatus.WAITING);
        bookings.put(booking.getId(), booking);
        idCounter++;

        log.debug("Created booking: ID={}, Item={}, Booker={}, Status={}, Start={}, End={}",
                booking.getId(), item.getId(), bookerId,
                booking.getStatus(), booking.getStart(), booking.getEnd());

        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public BookingDto approveBooking(Long bookingId, Long ownerId, boolean approved) {
        log.info("{} booking ID: {} by owner ID: {}",
                approved ? "Approving" : "Rejecting", bookingId, ownerId);

        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            log.warn("Booking not found: ID={}", bookingId);
            throw new NotFoundException("Booking not found with id: " + bookingId);
        }

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            log.warn("User {} is not owner of item {} for booking {}",
                    ownerId, booking.getItem().getId(), bookingId);
            throw new ValidationException("User is not the owner of the item");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            log.warn("Booking {} is not waiting for approval. Current status: {}",
                    bookingId, booking.getStatus());
            throw new ValidationException("Booking is not in waiting status");
        }

        BookingStatus newStatus = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
        booking.setStatus(newStatus);

        log.debug("Booking {} set to status: {}", bookingId, newStatus);
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public BookingDto getBookingById(Long bookingId, Long userId) {
        log.debug("Fetching booking ID: {} for user ID: {}", bookingId, userId);

        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            log.warn("Booking not found: ID={}", bookingId);
            throw new NotFoundException("Booking not found with id: " + bookingId);
        }

        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            log.warn("User {} not authorized to view booking {}", userId, bookingId);
            throw new NotFoundException("User not authorized to view this booking");
        }

        log.debug("Fetched booking: ID={}, Item={}, Status={}",
                bookingId, booking.getItem().getId(), booking.getStatus());

        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllBookingsForUser(Long userId, String state) {
        log.debug("Fetching all bookings for user ID: {} with state: {}", userId, state);

        userService.getUserById(userId); // Проверка существования пользователя

        List<Booking> userBookings = bookings.values().stream()
                .filter(b -> b.getBooker().getId().equals(userId))
                .sorted(Comparator.comparing(Booking::getStart).reversed())
                .collect(Collectors.toList());

        List<BookingDto> result = filterBookingsByState(userBookings, state);
        log.debug("Found {} bookings for user {} with state {}",
                result.size(), userId, state);
        return result;
    }

    @Override
    public List<BookingDto> getAllBookingsForOwner(Long ownerId, String state) {
        log.debug("Fetching all bookings for owner ID: {} with state: {}", ownerId, state);

        userService.getUserById(ownerId); // Проверка существования пользователя

        List<Booking> ownerBookings = bookings.values().stream()
                .filter(b -> b.getItem().getOwner().getId().equals(ownerId))
                .sorted(Comparator.comparing(Booking::getStart).reversed())
                .collect(Collectors.toList());

        List<BookingDto> result = filterBookingsByState(ownerBookings, state);
        log.debug("Found {} bookings for owner {} with state {}",
                result.size(), ownerId, state);
        return result;
    }

    private List<BookingDto> filterBookingsByState(List<Booking> bookings, String state) {
        BookingState bookingState;
        try {
            bookingState = BookingState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown booking state: {}", state);
            throw new ValidationException("Unknown state: " + state);
        }

        LocalDateTime now = LocalDateTime.now();
        log.debug("Filtering bookings by state: {}", bookingState);

        List<BookingDto> result = bookings.stream()
                .filter(booking -> {
                    switch (bookingState) {
                        case ALL:
                            return true;
                        case CURRENT:
                            return booking.getStart().isBefore(now) && booking.getEnd().isAfter(now);
                        case PAST:
                            return booking.getEnd().isBefore(now);
                        case FUTURE:
                            return booking.getStart().isAfter(now);
                        case WAITING:
                            return booking.getStatus() == BookingStatus.WAITING;
                        case REJECTED:
                            return booking.getStatus() == BookingStatus.REJECTED;
                        default:
                            return true;
                    }
                })
                .map(BookingMapper::toBookingDto)
                .toList();

        log.debug("Filtered {} bookings for state {}", result.size(), state);
        return result;
    }

    private enum BookingState {
        ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED
    }
}

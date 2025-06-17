package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ItemService itemService;

    @Override
    @Transactional
    public BookingDto createBooking(BookingDto bookingDto, Long bookerId) {
        log.info("Creating booking for user {} on item {}", bookerId, bookingDto.getItemId());

        User booker = userService.getUserEntityById(bookerId);
        Item item = itemService.getItemEntityById(bookingDto.getItemId());

        validateBooking(bookingDto, bookerId, item);

        Booking booking = BookingMapper.toBooking(bookingDto, item, booker);
        booking.setStatus(BookingStatus.WAITING);
        Booking savedBooking = bookingRepository.save(booking);

        log.debug("Created booking: ID={}, Item={}, Booker={}, Status={}, Start={}, End={}",
                savedBooking.getId(), item.getId(), bookerId,
                savedBooking.getStatus(), savedBooking.getStart(), savedBooking.getEnd());

        return BookingMapper.toBookingDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingDto approveBooking(Long bookingId, Long ownerId, boolean approved) {
        log.info("{} booking ID: {} by owner ID: {}",
                approved ? "Approving" : "Rejecting", bookingId, ownerId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found: ID={}", bookingId);
                    return new NotFoundException("Booking not found with id: " + bookingId);
                });

        validateApproval(booking, ownerId);

        BookingStatus newStatus = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
        booking.setStatus(newStatus);
        Booking updatedBooking = bookingRepository.save(booking);

        log.debug("Booking {} set to status: {}", bookingId, newStatus);
        return BookingMapper.toBookingDto(updatedBooking);
    }

    @Override
    public BookingDto getBookingById(Long bookingId, Long userId) {
        log.debug("Fetching booking ID: {} for user ID: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking not found: ID={}", bookingId);
                    return new NotFoundException("Booking not found with id: " + bookingId);
                });

        validateBookingAccess(booking, userId);

        log.debug("Fetched booking: ID={}, Item={}, Status={}",
                bookingId, booking.getItem().getId(), booking.getStatus());

        return BookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllBookingsForUser(Long userId, String state, int from, int size) {
        log.debug("Fetching all bookings for user ID: {} with state: {}", userId, state);
        validatePagination(from, size);
        userService.getUserEntityById(userId);

        PageRequest page = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        Page<Booking> bookingPage;

        switch (BookingState.valueOf(state.toUpperCase())) {
            case ALL:
                bookingPage = bookingRepository.findByBookerId(userId, page);
                break;
            case CURRENT:
                bookingPage = bookingRepository.findByBookerIdAndStartBeforeAndEndAfter(
                        userId, LocalDateTime.now(), LocalDateTime.now(), page);
                break;
            case PAST:
                bookingPage = bookingRepository.findByBookerIdAndEndBefore(
                        userId, LocalDateTime.now(), page);
                break;
            case FUTURE:
                bookingPage = bookingRepository.findByBookerIdAndStartAfter(
                        userId, LocalDateTime.now(), page);
                break;
            case WAITING:
                bookingPage = bookingRepository.findByBookerIdAndStatus(
                        userId, BookingStatus.WAITING, page);
                break;
            case REJECTED:
                bookingPage = bookingRepository.findByBookerIdAndStatus(
                        userId, BookingStatus.REJECTED, page);
                break;
            default:
                throw new ValidationException("Unknown state: " + state);
        }

        return bookingPage.getContent().stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getAllBookingsForOwner(Long ownerId, String state, int from, int size) {
        log.debug("Fetching all bookings for owner ID: {} with state: {}", ownerId, state);
        validatePagination(from, size);
        userService.getUserEntityById(ownerId);

        PageRequest page = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        Page<Booking> bookingPage;

        switch (BookingState.valueOf(state.toUpperCase())) {
            case ALL:
                bookingPage = bookingRepository.findByItemOwnerId(ownerId, page);
                break;
            case CURRENT:
                bookingPage = bookingRepository.findByItemOwnerIdAndStartBeforeAndEndAfter(
                        ownerId, LocalDateTime.now(), LocalDateTime.now(), page);
                break;
            case PAST:
                bookingPage = bookingRepository.findByItemOwnerIdAndEndBefore(
                        ownerId, LocalDateTime.now(), page);
                break;
            case FUTURE:
                bookingPage = bookingRepository.findByItemOwnerIdAndStartAfter(
                        ownerId, LocalDateTime.now(), page);
                break;
            case WAITING:
                bookingPage = bookingRepository.findByItemOwnerIdAndStatus(
                        ownerId, BookingStatus.WAITING, page);
                break;
            case REJECTED:
                bookingPage = bookingRepository.findByItemOwnerIdAndStatus(
                        ownerId, BookingStatus.REJECTED, page);
                break;
            default:
                throw new ValidationException("Unknown state: " + state);
        }

        return bookingPage.getContent().stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }

    private void validateBooking(BookingDto bookingDto, Long bookerId, Item item) {
        if (bookingDto.getEnd().isBefore(bookingDto.getStart())) {
            throw new ValidationException("End time must be after start time");
        }

        if (!item.getAvailable()) {
            log.warn("Item {} is not available for booking", item.getId());
            throw new UnavailableItemException("Item is not available for booking");
        }

        if (item.getOwner().getId().equals(bookerId)) {
            log.warn("User {} tried to book their own item {}", bookerId, item.getId());
            throw new SelfBookingException("Owner cannot book their own item");
        }
    }

    private void validateApproval(Booking booking, Long ownerId) {
        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            log.warn("User {} is not owner of item {} for booking {}",
                    ownerId, booking.getItem().getId(), booking.getId());
            throw new ValidationException("User is not the owner of the item");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            log.warn("Booking {} is not waiting for approval. Current status: {}",
                    booking.getId(), booking.getStatus());
            throw new ValidationException("Booking is not in waiting status");
        }
    }

    private void validateBookingAccess(Booking booking, Long userId) {
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            log.warn("User {} not authorized to view booking {}", userId, booking.getId());
            throw new NotFoundException("User not authorized to view this booking");
        }
    }

    private void validatePagination(int from, int size) {
        if (from < 0) {
            throw new ValidationException("'from' must be positive or zero");
        }
        if (size <= 0) {
            throw new ValidationException("'size' must be positive");
        }
    }

    private enum BookingState {
        ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED
    }
}

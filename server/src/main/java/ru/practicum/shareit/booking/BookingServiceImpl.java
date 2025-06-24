package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.SelfBookingException;
import ru.practicum.shareit.exception.UnavailableItemException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ItemService itemService;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto bookingRequestDto, Long bookerId) {
        log.info("Creating booking for user {} on item {}", bookerId, bookingRequestDto.getItemId());

        User booker = userService.getUserEntityById(bookerId);
        Item item = itemService.getItemEntityById(bookingRequestDto.getItemId());

        validateBooking(bookingRequestDto, bookerId, item);

        Booking booking = bookingMapper.toBooking(bookingRequestDto);
        booking.setBooker(booker);
        booking.setItem(item);
        booking.setStatus(BookingStatus.WAITING);

        Booking savedBooking = bookingRepository.save(booking);
        log.debug("Created booking: ID={}", savedBooking.getId());

        return bookingMapper.toBookingResponseDto(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponseDto approveBooking(Long bookingId, Long ownerId, boolean approved) {
        log.info("{} booking ID: {} by owner ID: {}",
                approved ? "Approving" : "Rejecting", bookingId, ownerId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        validateApproval(booking, ownerId);

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking updatedBooking = bookingRepository.save(booking);
        log.debug("Booking {} status updated", bookingId);

        return bookingMapper.toBookingResponseDto(updatedBooking);
    }

    @Override
    public BookingResponseDto getBookingById(Long bookingId, Long userId) {
        log.debug("Fetching booking ID: {} for user ID: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        validateBookingAccess(booking, userId);
        log.debug("Fetched booking: ID={}", bookingId);

        return bookingMapper.toBookingResponseDto(booking);
    }

    @Override
    public List<BookingResponseDto> getAllBookingsForUser(
            Long userId,
            String state,
            int from,
            int size) {

        log.debug("Fetching all bookings for user ID: {} with state: {}", userId, state);
        userService.getUserEntityById(userId);

        Page<Booking> bookingPage = getBookingsPageForUser(userId, state, from, size);
        return bookingPage.map(bookingMapper::toBookingResponseDto).getContent();
    }

    @Override
    public List<BookingResponseDto> getAllBookingsForOwner(
            Long ownerId,
            String state,
            int from,
            int size) {

        log.debug("Fetching all bookings for owner ID: {} with state: {}", ownerId, state);
        userService.getUserEntityById(ownerId);

        Page<Booking> bookingPage = getBookingsPageForOwner(ownerId, state, from, size);
        return bookingPage.map(bookingMapper::toBookingResponseDto).getContent();
    }

    private Page<Booking> getBookingsPageForUser(Long userId, String state, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        BookingState bookingState = BookingState.valueOf(state.toUpperCase());

        return switch (bookingState) {
            case ALL -> bookingRepository.findByBookerId(userId, page);
            case CURRENT -> bookingRepository.findByBookerIdAndStartBeforeAndEndAfter(
                    userId, LocalDateTime.now(), LocalDateTime.now(), page);
            case PAST -> bookingRepository.findByBookerIdAndEndBefore(userId, LocalDateTime.now(), page);
            case FUTURE -> bookingRepository.findByBookerIdAndStartAfter(userId, LocalDateTime.now(), page);
            case WAITING -> bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.WAITING, page);
            case REJECTED -> bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.REJECTED, page);
        };
    }

    private Page<Booking> getBookingsPageForOwner(Long ownerId, String state, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        BookingState bookingState = BookingState.valueOf(state.toUpperCase());

        return switch (bookingState) {
            case ALL -> bookingRepository.findByItemOwnerId(ownerId, page);
            case CURRENT -> bookingRepository.findByItemOwnerIdAndStartBeforeAndEndAfter(
                    ownerId, LocalDateTime.now(), LocalDateTime.now(), page);
            case PAST -> bookingRepository.findByItemOwnerIdAndEndBefore(ownerId, LocalDateTime.now(), page);
            case FUTURE -> bookingRepository.findByItemOwnerIdAndStartAfter(ownerId, LocalDateTime.now(), page);
            case WAITING -> bookingRepository.findByItemOwnerIdAndStatus(ownerId, BookingStatus.WAITING, page);
            case REJECTED -> bookingRepository.findByItemOwnerIdAndStatus(ownerId, BookingStatus.REJECTED, page);
        };
    }

    private void validateBooking(BookingRequestDto bookingDto, Long bookerId, Item item) {
        if (bookingDto.getEnd().isBefore(bookingDto.getStart())) {
            throw new ValidationException("End time must be after start time");
        }
        if (!item.getAvailable()) {
            throw new UnavailableItemException("Item is not available for booking");
        }
        if (item.getOwner().getId().equals(bookerId)) {
            throw new SelfBookingException("Owner cannot book their own item");
        }
    }

    private void validateApproval(Booking booking, Long ownerId) {
        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ValidationException("User is not the owner of the item");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Booking is not in waiting status");
        }
    }

    private void validateBookingAccess(Booking booking, Long userId) {
        if (!booking.getBooker().getId().equals(userId) && !booking.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("User not authorized to view this booking");
        }
    }

    private enum BookingState {
        ALL, CURRENT, PAST, FUTURE, WAITING, REJECTED
    }
}

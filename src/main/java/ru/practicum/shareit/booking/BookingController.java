package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto createBooking(
            @RequestBody BookingDto bookingDto,
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("POST /bookings - User {} creating booking for item {}",
                userId, bookingDto.getItemId());
        BookingDto createdBooking = bookingService.createBooking(bookingDto, userId);
        log.debug("Created booking: ID={}, Item={}, Start={}, End={}",
                createdBooking.getId(), createdBooking.getItemId(),
                createdBooking.getStart(), createdBooking.getEnd());
        return createdBooking;
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approveBooking(
            @PathVariable Long bookingId,
            @RequestParam boolean approved,
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("PATCH /bookings/{} - User {} {} booking",
                bookingId, userId, approved ? "approving" : "rejecting");
        BookingDto updatedBooking = bookingService.approveBooking(bookingId, userId, approved);
        log.debug("Booking {} {} by user {}",
                bookingId, approved ? "approved" : "rejected", userId);
        return updatedBooking;
    }

    @GetMapping("/{bookingId}")
    public BookingDto getBookingById(
            @PathVariable Long bookingId,
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("GET /bookings/{} - Fetching booking by user {}", bookingId, userId);
        BookingDto booking = bookingService.getBookingById(bookingId, userId);
        log.debug("Fetched booking: ID={}, Item={}, Status={}",
                bookingId, booking.getItemId(), booking.getStatus());
        return booking;
    }

    @GetMapping
    public List<BookingDto> getAllBookingsForUser(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @RequestParam(defaultValue = "ALL") String state
    ) {
        log.info("GET /bookings?state={} - Fetching bookings for user {}", state, userId);
        List<BookingDto> bookings = bookingService.getAllBookingsForUser(userId, state);
        log.debug("Fetched {} bookings for user {} with state {}",
                bookings.size(), userId, state);
        return bookings;
    }

    @GetMapping("/owner")
    public List<BookingDto> getAllBookingsForOwner(
            @RequestHeader("X-Sharer-User-Id") Long ownerId,
            @RequestParam(defaultValue = "ALL") String state
    ) {
        log.info("GET /bookings/owner?state={} - Fetching bookings for owner {}", state, ownerId);
        List<BookingDto> bookings = bookingService.getAllBookingsForOwner(ownerId, state);
        log.debug("Fetched {} bookings for owner {} with state {}",
                bookings.size(), ownerId, state);
        return bookings;
    }

    @ExceptionHandler({NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex) {
        log.warn("Booking not found: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler({ValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler({ForbiddenException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbidden(Exception ex) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleInternalError(Exception ex) {
        log.error("Internal server error", ex);
        return "Internal server error";
    }
}

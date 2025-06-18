package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDto createBooking(
            @Valid @RequestBody BookingRequestDto bookingRequestDto,
            @RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("POST /bookings - User {} creating booking for item {}",
                userId, bookingRequestDto.getItemId());
        BookingResponseDto response = bookingService.createBooking(bookingRequestDto, userId);
        log.debug("Created booking: ID={}", response.getId());
        return response;
    }

    @PatchMapping("/{bookingId}")
    public BookingResponseDto approveBooking(
            @PathVariable Long bookingId,
            @RequestParam boolean approved,
            @RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("PATCH /bookings/{} - User {} {} booking",
                bookingId, userId, approved ? "approving" : "rejecting");
        BookingResponseDto response = bookingService.approveBooking(bookingId, userId, approved);
        log.debug("Booking {} status updated to {}", bookingId, response.getStatus());
        return response;
    }

    @GetMapping("/{bookingId}")
    public BookingResponseDto getBookingById(
            @PathVariable Long bookingId,
            @RequestHeader("X-Sharer-User-Id") Long userId) {
        log.info("GET /bookings/{} - Fetching booking by user {}", bookingId, userId);
        BookingResponseDto response = bookingService.getBookingById(bookingId, userId);
        log.debug("Fetched booking: ID={}", bookingId);
        return response;
    }

    @GetMapping
    public List<BookingResponseDto> getAllBookingsForUser(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @RequestParam(defaultValue = "ALL") String state,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /bookings?state={} - Fetching bookings for user {}", state, userId);
        List<BookingResponseDto> response = bookingService.getAllBookingsForUser(userId, state, from, size);
        log.debug("Fetched {} bookings for user {}", response.size(), userId);
        return response;
    }

    @GetMapping("/owner")
    public List<BookingResponseDto> getAllBookingsForOwner(
            @RequestHeader("X-Sharer-User-Id") Long ownerId,
            @RequestParam(defaultValue = "ALL") String state,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /bookings/owner?state={} - Fetching bookings for owner {}", state, ownerId);
        List<BookingResponseDto> response = bookingService.getAllBookingsForOwner(ownerId, state, from, size);
        log.debug("Fetched {} bookings for owner {}", response.size(), ownerId);
        return response;
    }
}

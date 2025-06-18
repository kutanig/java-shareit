package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;


@Component
@RequiredArgsConstructor
public class BookingMapper {
    public BookingResponseDto toBookingResponseDto(Booking booking) {
        return BookingResponseDto.builder()
                .id(booking.getId())
                .start(booking.getStart())
                .end(booking.getEnd())
                .status(booking.getStatus())
                .booker(new BookingResponseDto.BookerDto(
                        booking.getBooker().getId(),
                        booking.getBooker().getName()))
                .item(new BookingResponseDto.ItemDto(
                        booking.getItem().getId(),
                        booking.getItem().getName()))
                .build();
    }

    public Booking toBooking(BookingRequestDto bookingRequestDto) {
        return Booking.builder()
                .start(bookingRequestDto.getStart())
                .end(bookingRequestDto.getEnd())
                .status(BookingStatus.WAITING)
                .build();
    }
}

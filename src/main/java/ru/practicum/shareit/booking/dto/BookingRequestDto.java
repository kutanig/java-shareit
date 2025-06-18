package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDto {
    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be in present or future")
    private LocalDateTime start;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in future")
    private LocalDateTime end;

    @NotNull(message = "Item ID is required")
    private Long itemId;
}

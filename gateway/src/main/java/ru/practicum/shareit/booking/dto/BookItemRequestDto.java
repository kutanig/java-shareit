package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookItemRequestDto {
    @NotNull(message = "Item ID is required")
    private long itemId;

    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be in present or future")
    private LocalDateTime start;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in future")
    private LocalDateTime end;
}

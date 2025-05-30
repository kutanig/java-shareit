package ru.practicum.shareit.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Booking {
    private Long id;

    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be in present or future")
    private LocalDateTime start;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in future")
    private LocalDateTime end;

    @NotNull(message = "Item is required")
    private Item item;

    @NotNull(message = "Booker is required")
    private User booker;

    @NotNull(message = "Status is required")
    private BookingStatus status;
}
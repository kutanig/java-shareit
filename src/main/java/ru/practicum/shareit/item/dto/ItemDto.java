package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private BookingDateDto lastBooking;  // Последнее бронирование
    private BookingDateDto nextBooking;  // Следующее бронирование
    private List<CommentDto> comments;  // Комментарии
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemShortDto {
    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private BookingDateDto lastBooking;  // Последнее бронирование
    private BookingDateDto nextBooking;  // Следующее бронирование
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingDateDto {
    private Long id;            // ID бронирования
    private Long bookerId;      // ID пользователя, который забронировал
    private LocalDateTime start; // Дата начала
    private LocalDateTime end;   // Дата окончания
}

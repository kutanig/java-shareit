package ru.practicum.shareit.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ItemRequest {
    private Long id;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotNull(message = "Requestor is required")
    private User requestor;

    private LocalDateTime created = LocalDateTime.now();
}

package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserUpdateDto {

    private String name;  // Поле необязательное для обновления

    @Email(message = "Invalid email format")
    private String email; // Поле необязательное для обновления
}

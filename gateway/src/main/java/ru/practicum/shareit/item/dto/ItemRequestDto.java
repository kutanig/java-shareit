package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequestDto {
    @NotBlank(message = "The name of the item cannot be empty")
    private String name;

    @NotBlank(message = "The description of a thing cannot be empty")
    private String description;

    @NotNull(message = "The item's availability status is required")
    private Boolean available;

    private Long requestId;
}
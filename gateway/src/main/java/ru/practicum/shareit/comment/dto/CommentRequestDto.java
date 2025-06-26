package ru.practicum.shareit.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {
    //@NotBlank(message = "The comment text cannot be empty.")
    //@Size(max = 1000, message = "The maximum length of a comment is 1000 characters.")
    private String text;
}

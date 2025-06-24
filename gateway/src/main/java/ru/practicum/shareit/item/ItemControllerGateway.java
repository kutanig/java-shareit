package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.comment.dto.CommentRequestDto;
import ru.practicum.shareit.item.dto.ItemRequestDto;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ItemControllerGateway {
    private final ItemClient itemClient;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @PostMapping
    public ResponseEntity<Object> addItem(
            @RequestHeader(USER_ID_HEADER) @Positive Long userId,
            @RequestBody @Valid ItemRequestDto itemRequestDto) {
        log.info("Gateway: Добавление вещи {} пользователем ID={}", itemRequestDto.getName(), userId);
        return itemClient.addItem(userId, itemRequestDto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Object> updateItem(
            @PathVariable @Positive Long itemId,
            @RequestHeader(USER_ID_HEADER) @Positive Long userId,
            @RequestBody @Valid ItemRequestDto itemRequestDto) {
        log.info("Gateway: Обновление вещи ID={} пользователем ID={}", itemId, userId);
        return itemClient.updateItem(itemId, userId, itemRequestDto);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Object> getItemById(
            @PathVariable @Positive Long itemId,
            @RequestHeader(USER_ID_HEADER) @Positive Long userId) {
        log.info("Gateway: Получение вещи ID={} пользователем ID={}", itemId, userId);
        return itemClient.getItem(itemId, userId);
    }

    @GetMapping
    public ResponseEntity<Object> getAllItemsByOwner(
            @RequestHeader(USER_ID_HEADER) @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Gateway: Получение всех вещей владельца ID={}, from={}, size={}", userId, from, size);
        return itemClient.getAllItems(userId, from, size);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchItems(
            @RequestParam @NotBlank String text,
            @RequestHeader(USER_ID_HEADER) @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Gateway: Поиск вещей по тексту '{}' для пользователя ID={}", text, userId);
        return itemClient.searchItems(text, userId, from, size);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> addComment(
            @PathVariable @Positive Long itemId,
            @RequestHeader(USER_ID_HEADER) @Positive Long userId,
            @RequestBody @Valid CommentRequestDto commentRequestDto) {
        log.info("Gateway: Добавление комментария к вещи ID={} пользователем ID={}", itemId, userId);
        return itemClient.addComment(itemId, userId, commentRequestDto);
    }
}

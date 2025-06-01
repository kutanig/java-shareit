package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/items")
public class ItemController {
    private static final Logger log = LoggerFactory.getLogger(ItemController.class);
    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto addItem(
            @Valid
            @RequestBody ItemDto itemDto,
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("POST /items - User {} adding new item: {}", userId, itemDto.getName());
        ItemDto createdItem = itemService.addItem(itemDto, userId);
        log.debug("Created item: ID={}, Name={}, Owner={}",
                createdItem.getId(), createdItem.getName(), userId);
        return createdItem;
    }

    @PatchMapping("/{itemId}")
    public ItemDto updateItem(
            @PathVariable Long itemId,
            @RequestBody ItemDto itemDto,
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("PATCH /items/{} - User {} updating item", itemId, userId);
        ItemDto updatedItem = itemService.updateItem(itemId, itemDto, userId);
        log.debug("Updated item: ID={}, Name={}", itemId, updatedItem.getName());
        return updatedItem;
    }

    @GetMapping("/{itemId}")
    public ItemDto getItemById(@PathVariable Long itemId) {
        log.info("GET /items/{} - Fetching item", itemId);
        ItemDto item = itemService.getItemById(itemId);
        log.debug("Fetched item: ID={}, Name={}", itemId, item.getName());
        return item;
    }

    @GetMapping
    public List<ItemDto> getAllItemsByOwner(
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("GET /items - Fetching all items for owner {}", userId);
        List<ItemDto> items = itemService.getAllItemsByOwner(userId);
        log.debug("Fetched {} items for owner {}", items.size(), userId);
        return items;
    }

    @GetMapping("/search")
    public List<ItemDto> searchItems(
            @RequestParam String text
    ) {
        log.info("GET /items/search?text={} - Searching items", text);
        List<ItemDto> result = itemService.searchItems(text);
        log.debug("Found {} items for search: '{}'", result.size(), text);
        return result;
    }

    @ExceptionHandler({NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex) {
        log.warn("Item not found: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler({ValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler({ForbiddenException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleForbidden(Exception ex) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleInternalError(Exception ex) {
        log.error("Internal server error", ex);
        return "Internal server error";
    }
}

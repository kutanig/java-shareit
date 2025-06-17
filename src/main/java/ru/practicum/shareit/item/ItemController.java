package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/items")
public class ItemController {
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
}

package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestMapper;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final Map<Long, Item> items = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final UserService userService;
    private final ItemRequestService itemRequestService;

    @Override
    public ItemDto addItem(ItemDto itemDto, Long ownerId) {
        log.info("Adding new item '{}' for owner ID: {}", itemDto.getName(), ownerId);

        User owner = UserMapper.toUser(userService.getUserById(ownerId));
        ItemRequest request = null;

        if (itemDto.getRequestId() != null) {
            log.debug("Item has request ID: {}", itemDto.getRequestId());
            ItemRequestDto requestDto = itemRequestService.getRequestById(itemDto.getRequestId());
            User requestor = UserMapper.toUser(userService.getUserById(requestDto.getRequestorId()));
            request = ItemRequestMapper.toItemRequest(requestDto, requestor);
        }

        Item item = ItemMapper.toItem(itemDto, owner, request);
        item.setId(idCounter.getAndIncrement());
        items.put(item.getId(), item);

        log.debug("Added item: ID={}, Name={}, Owner={}, Request={}",
                item.getId(), item.getName(), ownerId, itemDto.getRequestId());

        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto updateItem(Long itemId, ItemDto itemDto, Long ownerId) {
        log.info("Updating item ID: {} for owner ID: {}", itemId, ownerId);

        Item existingItem = items.get(itemId);
        if (existingItem == null) {
            log.warn("Item not found for update: ID={}", itemId);
            throw new NotFoundException("Item not found with id: " + itemId);
        }

        if (!existingItem.getOwner().getId().equals(ownerId)) {
            log.warn("User {} is not owner of item {}", ownerId, itemId);
            throw new NotFoundException("User is not the owner of the item");
        }

        String originalName = existingItem.getName();
        String originalDesc = existingItem.getDescription();
        Boolean originalAvailable = existingItem.getAvailable();

        if (itemDto.getName() != null) existingItem.setName(itemDto.getName());
        if (itemDto.getDescription() != null) existingItem.setDescription(itemDto.getDescription());
        if (itemDto.getAvailable() != null) existingItem.setAvailable(itemDto.getAvailable());

        log.debug("Updated item: ID={}, Name: {} -> {}, Description: {} -> {}, Available: {} -> {}",
                itemId, originalName, existingItem.getName(),
                originalDesc != null ? originalDesc.substring(0, Math.min(20, originalDesc.length())) + "..." : "null",
                existingItem.getDescription() != null ? existingItem.getDescription().substring(0, Math.min(20, existingItem.getDescription().length())) + "..." : "null",
                originalAvailable, existingItem.getAvailable());

        return ItemMapper.toItemDto(existingItem);
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        log.debug("Fetching item by ID: {}", itemId);

        Item item = items.get(itemId);
        if (item == null) {
            log.warn("Item not found: ID={}", itemId);
            throw new NotFoundException("Item not found with id: " + itemId);
        }

        return ItemMapper.toItemDto(item);
    }

    @Override
    public List<ItemDto> getAllItemsByOwner(Long ownerId) {
        log.debug("Fetching all items for owner ID: {}", ownerId);

        List<ItemDto> result = items.values().stream()
                .filter(item -> item.getOwner().getId().equals(ownerId))
                .map(ItemMapper::toItemDto)
                .toList();

        log.debug("Found {} items for owner ID: {}", result.size(), ownerId);
        return result;
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        log.debug("Searching items by text: '{}'", text);

        String searchText = text.toLowerCase();
        List<ItemDto> result = items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .filter(item ->
                        (item.getName() != null && item.getName().toLowerCase().contains(searchText)) ||
                                (item.getDescription() != null && item.getDescription().toLowerCase().contains(searchText))
                )
                .map(ItemMapper::toItemDto)
                .toList();

        log.debug("Found {} items for search: '{}'", result.size(), text);
        return result;
    }

    @Override
    public Item getItemEntityById(Long itemId) {
        log.debug("Fetching item entity by ID: {}", itemId);

        Item item = items.get(itemId);
        if (item == null) {
            log.warn("Item entity not found: ID={}", itemId);
            throw new NotFoundException("Item not found with id: " + itemId);
        }

        return item;
    }
}

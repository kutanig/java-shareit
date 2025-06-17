package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestMapper;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemRequestService itemRequestService;

    @Override
    @Transactional
    public ItemDto addItem(ItemDto itemDto, Long ownerId) {
        log.info("Adding new item '{}' for owner ID: {}", itemDto.getName(), ownerId);

        User owner = userService.getUserEntityById(ownerId);
        ItemRequest request = null;

        if (itemDto.getRequestId() != null) {
            log.debug("Item has request ID: {}", itemDto.getRequestId());
            ItemRequestDto requestDto = itemRequestService.getRequestById(itemDto.getRequestId());
            request = ItemRequestMapper.toItemRequest(requestDto,
                    userService.getUserEntityById(requestDto.getRequestorId()));
        }

        Item item = ItemMapper.toItem(itemDto, owner, request);
        Item savedItem = itemRepository.save(item);

        log.debug("Added item: ID={}, Name={}, Owner={}, Request={}",
                savedItem.getId(), savedItem.getName(), ownerId, itemDto.getRequestId());

        return ItemMapper.toItemDto(savedItem);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemDto itemDto, Long ownerId) {
        log.info("Updating item ID: {} for owner ID: {}", itemId, ownerId);

        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("Item not found for update: ID={}", itemId);
                    return new NotFoundException("Item not found with id: " + itemId);
                });

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

        Item updatedItem = itemRepository.save(existingItem);

        log.debug("Updated item: ID={}, Name: {} -> {}, Description: {} -> {}, Available: {} -> {}",
                itemId, originalName, updatedItem.getName(),
                originalDesc != null ? originalDesc.substring(0, Math.min(20, originalDesc.length())) + "..." : "null",
                updatedItem.getDescription() != null ? updatedItem.getDescription().substring(0, Math.min(20, updatedItem.getDescription().length())) + "..." : "null",
                originalAvailable, updatedItem.getAvailable());

        return ItemMapper.toItemDto(updatedItem);
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        log.debug("Fetching item by ID: {}", itemId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("Item not found: ID={}", itemId);
                    return new NotFoundException("Item not found with id: " + itemId);
                });

        return ItemMapper.toItemDto(item);
    }

    @Override
    public List<ItemDto> getAllItemsByOwner(Long ownerId) {
        log.debug("Fetching all items for owner ID: {}", ownerId);

        List<Item> items = itemRepository.findByOwnerIdOrderById(ownerId);
        List<ItemDto> result = items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());

        log.debug("Found {} items for owner ID: {}", result.size(), ownerId);
        return result;
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        log.debug("Searching items by text: '{}'", text);

        if (text.isBlank()) {
            log.debug("Empty search text - returning empty list");
            return Collections.emptyList();
        }

        List<Item> foundItems = itemRepository.searchAvailableItems(text.toLowerCase());
        List<ItemDto> result = foundItems.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());

        log.debug("Found {} items for search: '{}'", result.size(), text);
        return result;
    }

    @Override
    public Item getItemEntityById(Long itemId) {
        log.debug("Fetching item entity by ID: {}", itemId);

        return itemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("Item entity not found: ID={}", itemId);
                    return new NotFoundException("Item not found with id: " + itemId);
                });
    }
}

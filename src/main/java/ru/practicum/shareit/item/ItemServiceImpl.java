package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.comment.Comment;
import ru.practicum.shareit.comment.CommentMapper;
import ru.practicum.shareit.comment.CommentRepository;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestMapper;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final ItemRequestService itemRequestService;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

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

        List<CommentDto> comments = commentRepository.findByItemId(itemId)
                .stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());

        ItemDto itemDto = ItemMapper.toItemDto(item);
        itemDto.setComments(comments);

        return itemDto;
    }

    @Override
    public List<ItemDto> getAllItemsByOwner(Long ownerId) {
        log.debug("Fetching all items for owner ID: {}", ownerId);

        List<Item> items = itemRepository.findByOwnerIdOrderById(ownerId);
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());

        List<Comment> comments = commentRepository.findByItemIdIn(itemIds);
        Map<Long, List<CommentDto>> commentsByItem = comments.stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())
                ));

        List<ItemDto> result = items.stream()
                .map(item -> {
                    ItemDto dto = ItemMapper.toItemDto(item);
                    dto.setComments(commentsByItem.getOrDefault(item.getId(), Collections.emptyList()));
                    return dto;
                })
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

    @Override
    @Transactional
    public CommentDto addComment(Long itemId, Long userId, CommentDto commentDto) {
        log.info("Adding comment to item ID: {} by user ID: {}", itemId, userId);

        if (commentDto.getText() == null || commentDto.getText().isBlank()) {
            log.warn("Comment text is empty");
            throw new ValidationException("Comment text cannot be empty");
        }

        User author = userService.getUserEntityById(userId);
        Item item = getItemEntityById(itemId);

        List<Booking> bookings = bookingRepository.findByItemIdAndBookerIdAndStatusAndEndBefore(
                itemId,
                userId,
                BookingStatus.APPROVED,
                LocalDateTime.now()
        );

        if (bookings.isEmpty()) {
            log.warn("User {} has not booked item {} or booking is not completed", userId, itemId);
            throw new ValidationException("User has not booked this item or booking is not completed yet");
        }

        Comment comment = CommentMapper.toComment(commentDto, item, author);
        Comment savedComment = commentRepository.save(comment);

        log.debug("Added comment: ID={}, Item={}, Author={}",
                savedComment.getId(), itemId, userId);

        return CommentMapper.toCommentDto(savedComment);
    }
}

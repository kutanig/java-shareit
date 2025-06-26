package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.comment.Comment;
import ru.practicum.shareit.comment.CommentMapper;
import ru.practicum.shareit.comment.CommentRepository;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
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
    private final ItemRequestRepository itemRequestRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemDto addItem(ItemDto itemDto, Long ownerId) {
        log.info("Adding new item '{}' for owner ID: {}", itemDto.getName(), ownerId);

        User owner = userService.getUserEntityById(ownerId);
        ItemRequest request = null;

        if (itemDto.getRequestId() != null) {
            log.debug("Processing request ID: {}", itemDto.getRequestId());
            request = itemRequestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Request with ID %d not found", itemDto.getRequestId())
                    ));

            if (request.getRequestor().getId().equals(ownerId)) {
                throw new ValidationException("You cannot create an item in response to your own request");
            }
        }

        Item item = itemMapper.toItem(itemDto, owner, request);
        Item savedItem = itemRepository.save(item);

        log.info("Successfully added item: ID={}, Name='{}', Owner={}, Request={}",
                savedItem.getId(), savedItem.getName(), ownerId, itemDto.getRequestId());

        return itemMapper.toItemDto(savedItem, ownerId);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemDto itemDto, Long ownerId) {
        log.info("Updating item ID: {} for owner ID: {}", itemId, ownerId);

        Item existingItem = getItemEntityById(itemId);
        validateOwner(existingItem, ownerId);

        updateItemFields(existingItem, itemDto);
        Item updatedItem = itemRepository.save(existingItem);

        log.debug("Updated item: ID={}", itemId);
        return itemMapper.toItemDto(updatedItem, ownerId);
    }

    @Override
    public ItemDto getItemById(Long itemId, Long userId) {
        log.debug("Fetching item by ID: {} for user ID: {}", itemId, userId);
        Item item = getItemEntityById(itemId);
        return itemMapper.toItemDto(item, userId);
    }

    @Override
    public List<ItemDto> getAllItemsByOwner(Long ownerId) {
        log.debug("Fetching all items for owner ID: {}", ownerId);

        List<Item> items = itemRepository.findByOwnerIdOrderById(ownerId);
        return items.stream()
                .map(item -> itemMapper.toItemDto(item, ownerId)) // ownerId для показа бронирований
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        log.debug("Searching items by text: '{}'", text);

        if (text.isBlank()) {
            return Collections.emptyList();
        }

        return itemRepository.searchAvailableItems(text.toLowerCase()).stream()
                .map(item -> itemMapper.toItemDto(item, null)) // null userId - не показываем бронирования
                .collect(Collectors.toList());
    }

    @Override
    public Item getItemEntityById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));
    }

    @Override
    @Transactional
    public CommentDto addComment(Long itemId, Long userId, CommentDto commentDto) {
        //validateCommentText(commentDto);

        User author = userService.getUserEntityById(userId);
        Item item = getItemEntityById(itemId);
        validateBookingForComment(itemId, userId);

        Comment comment = CommentMapper.toComment(commentDto, item, author);
        Comment savedComment = commentRepository.save(comment);

        log.debug("Added comment: ID={}, Item={}, Author={}",
                savedComment.getId(), itemId, userId);

        return CommentMapper.toCommentDto(savedComment);
    }

    private void validateOwner(Item item, Long ownerId) {
        if (!item.getOwner().getId().equals(ownerId)) {
            throw new NotFoundException("User is not the owner of the item");
        }
    }

    private void updateItemFields(Item item, ItemDto itemDto) {
        if (itemDto.getName() != null) item.setName(itemDto.getName());
        if (itemDto.getDescription() != null) item.setDescription(itemDto.getDescription());
        if (itemDto.getAvailable() != null) item.setAvailable(itemDto.getAvailable());
    }

    /*private void validateCommentText(CommentDto commentDto) {
        if (commentDto.getText() == null || commentDto.getText().isBlank()) {
            throw new ValidationException("Comment text cannot be empty");
        }
    }*/

    private void validateBookingForComment(Long itemId, Long userId) {
        if (bookingRepository.findCompletedBookingsForComment(itemId, userId).isEmpty()) {
            throw new ValidationException("User has not booked this item or booking is not completed yet");
        }
    }
}

package ru.practicum.shareit.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ItemRequestServiceImpl implements ItemRequestService {
    private static final Logger log = LoggerFactory.getLogger(ItemRequestServiceImpl.class);
    private final Map<Long, ItemRequest> requests = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final UserService userService;

    public ItemRequestServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ItemRequestDto createRequest(ItemRequestDto requestDto, Long userId) {
        log.info("Creating request for user ID: {}", userId);

        User requestor = UserMapper.toUser(userService.getUserById(userId));

        if (requestDto.getDescription() == null || requestDto.getDescription().isBlank()) {
            log.warn("Empty description in request from user {}", userId);
            throw new ValidationException("Request description cannot be empty");
        }

        ItemRequest request = ItemRequestMapper.toItemRequest(requestDto, requestor);
        request.setId(idCounter.getAndIncrement());
        request.setCreated(LocalDateTime.now());
        requests.put(request.getId(), request);

        log.debug("Created request: ID={}, User={}, Description='{}', Created={}",
                request.getId(), userId,
                truncate(request.getDescription(), 30),
                request.getCreated());

        return ItemRequestMapper.toItemRequestDto(request);
    }

    @Override
    public ItemRequestDto getRequestById(Long requestId) {
        log.debug("Fetching request by ID: {}", requestId);

        ItemRequest request = requests.get(requestId);
        if (request == null) {
            log.warn("Request not found: ID={}", requestId);
            throw new NotFoundException("Request not found with id: " + requestId);
        }

        return ItemRequestMapper.toItemRequestDto(request);
    }

    @Override
    public List<ItemRequestDto> getAllRequestsForUser(Long userId) {
        log.debug("Fetching all requests for user ID: {}", userId);

        userService.getUserById(userId); // Проверка существования пользователя

        List<ItemRequestDto> result = requests.values().stream()
                .filter(r -> r.getRequestor().getId().equals(userId))
                .sorted(Comparator.comparing(ItemRequest::getCreated).reversed())
                .map(ItemRequestMapper::toItemRequestDto)
                .toList();

        log.debug("Found {} requests for user ID: {}", result.size(), userId);
        return result;
    }

    @Override
    public List<ItemRequestDto> getAllRequests(Long userId, int from, int size) {
        log.debug("Fetching all requests (from={}, size={}) excluding user ID: {}", from, size, userId);

        if (from < 0) {
            log.warn("Invalid 'from' parameter: {}", from);
            throw new ValidationException("'from' must be positive or zero");
        }

        if (size <= 0) {
            log.warn("Invalid 'size' parameter: {}", size);
            throw new ValidationException("'size' must be positive");
        }

        List<ItemRequest> allRequests = requests.values().stream()
                .filter(r -> !r.getRequestor().getId().equals(userId))
                .sorted(Comparator.comparing(ItemRequest::getCreated).reversed())
                .toList();

        int total = allRequests.size();
        int start = Math.min(from, total);
        int end = Math.min(start + size, total);

        List<ItemRequestDto> result = allRequests.subList(start, end).stream()
                .map(ItemRequestMapper::toItemRequestDto)
                .toList();

        log.debug("Fetched {} requests (from={} to {}) of total {} excluding user {}",
                result.size(), start, end, total, userId);

        return result;
    }

    private String truncate(String text, int length) {
        if (text == null) return "null";
        return text.length() <= length ? text : text.substring(0, length) + "...";
    }
}

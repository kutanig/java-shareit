package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static io.micrometer.common.util.StringUtils.truncate;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final UserService userService;

    @Override
    @Transactional
    public ItemRequestDto createRequest(ItemRequestDto requestDto, Long userId) {
        log.info("Creating request for user ID: {}", userId);

        User requestor = userService.getUserEntityById(userId);

        if (requestDto.getDescription() == null || requestDto.getDescription().isBlank()) {
            log.warn("Empty description in request from user {}", userId);
            throw new ValidationException("Request description cannot be empty");
        }

        ItemRequest request = ItemRequestMapper.toItemRequest(requestDto, requestor);
        request.setCreated(LocalDateTime.now());
        ItemRequest savedRequest = requestRepository.save(request);

        log.debug("Created request: ID={}, User={}, Description='{}', Created={}",
                savedRequest.getId(), userId,
                truncate(savedRequest.getDescription(), 30),
                savedRequest.getCreated());

        return ItemRequestMapper.toItemRequestDto(savedRequest);
    }

    @Override
    public ItemRequestDto getRequestById(Long requestId) {
        log.debug("Fetching request by ID: {}", requestId);

        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("Request not found: ID={}", requestId);
                    return new NotFoundException("Request not found with id: " + requestId);
                });

        return ItemRequestMapper.toItemRequestDto(request);
    }

    @Override
    public List<ItemRequestDto> getAllRequestsForUser(Long userId) {
        log.debug("Fetching all requests for user ID: {}", userId);

        userService.getUserEntityById(userId);

        List<ItemRequest> requests = requestRepository
                .findByRequestorIdOrderByCreatedDesc(userId);

        List<ItemRequestDto> result = requests.stream()
                .map(ItemRequestMapper::toItemRequestDto)
                .collect(Collectors.toList());

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

        PageRequest page = PageRequest.of(
                from / size,
                size,
                Sort.by(Sort.Direction.DESC, "created"));

        Page<ItemRequest> requestPage = requestRepository.findByRequestorIdNot(userId, page);
        List<ItemRequest> requests = requestPage.getContent();

        List<ItemRequestDto> result = requests.stream()
                .map(ItemRequestMapper::toItemRequestDto)
                .collect(Collectors.toList());

        log.debug("Fetched {} requests for page {} (size {}) excluding user {}",
                result.size(), from / size, size, userId);

        return result;
    }
}

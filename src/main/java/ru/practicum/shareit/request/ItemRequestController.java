package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/requests")
public class ItemRequestController {
    private static final Logger log = LoggerFactory.getLogger(ItemRequestController.class);
    private final ItemRequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemRequestDto createRequest(
            @Valid
            @RequestBody ItemRequestDto requestDto,
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("POST /requests - User {} creating request: '{}'",
                userId, truncate(requestDto.getDescription(), 50));
        ItemRequestDto createdRequest = requestService.createRequest(requestDto, userId);
        log.debug("Created request: ID={}, Description='{}'",
                createdRequest.getId(), truncate(createdRequest.getDescription(), 50));
        return createdRequest;
    }

    @GetMapping
    public List<ItemRequestDto> getAllRequestsForUser(
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("GET /requests - Fetching all requests for user {}", userId);
        List<ItemRequestDto> requests = requestService.getAllRequestsForUser(userId);
        log.debug("Fetched {} requests for user {}", requests.size(), userId);
        return requests;
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getAllRequests(
            @RequestHeader("X-Sharer-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /requests/all?from={}&size={} - Fetching all requests for user {}", from, size, userId);
        List<ItemRequestDto> requests = requestService.getAllRequests(userId, from, size);
        log.debug("Fetched {} requests (from={}, size={}) for user {}",
                requests.size(), from, size, userId);
        return requests;
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getRequestById(
            @PathVariable Long requestId,
            @RequestHeader("X-Sharer-User-Id") Long userId
    ) {
        log.info("GET /requests/{} - Fetching request by user {}", requestId, userId);
        ItemRequestDto request = requestService.getRequestById(requestId);
        log.debug("Fetched request: ID={}, Description='{}'",
                requestId, truncate(request.getDescription(), 50));
        return request;
    }

    @ExceptionHandler({NotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex) {
        log.warn("Request not found: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler({ValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleInternalError(Exception ex) {
        log.error("Internal server error", ex);
        return "Internal server error";
    }

    private String truncate(String text, int length) {
        if (text == null) return "null";
        return text.length() <= length ? text : text.substring(0, length) + "...";
    }
}

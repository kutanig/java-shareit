package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto createRequest(ItemRequestDto requestDto, Long userId);

    ItemRequestDto getRequestById(Long requestId);

    List<ItemRequestDto> getAllRequestsForUser(Long userId);

    List<ItemRequestDto> getAllRequests(Long userId, int from, int size);
}

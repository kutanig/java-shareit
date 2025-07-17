package ru.practicum.shareit.request;


import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public class ItemRequestMapper {
    public static ItemRequestDto toItemRequestDto(ItemRequest request) {
        return new ItemRequestDto(
                request.getId(),
                request.getDescription(),
                request.getRequestor().getId(),
                request.getCreated()
        );
    }

    public static ItemRequest toItemRequest(ItemRequestDto requestDto, User requestor) {
        ItemRequest request = new ItemRequest();
        request.setId(requestDto.getId());
        request.setDescription(requestDto.getDescription());
        request.setRequestor(requestor);
        request.setCreated(requestDto.getCreated());
        return request;
    }

    public static ItemRequestResponseDto toResponseDto(ItemRequest request, List<ItemDto> items) {
        return new ItemRequestResponseDto(
                request.getId(),
                request.getDescription(),
                request.getRequestor().getId(),
                request.getCreated(),
                items
        );
    }

    public static ItemRequestResponseDto toItemRequestResponseDto(ItemRequest request, List<ItemDto> items) {
        return new ItemRequestResponseDto(
                request.getId(),
                request.getDescription(),
                request.getRequestor().getId(),
                request.getCreated(),
                items
        );
    }
}

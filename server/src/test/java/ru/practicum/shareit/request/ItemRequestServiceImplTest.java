package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository requestRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemRequestServiceImpl requestService;

    private User requestor;
    private ItemRequest request;
    private ItemRequestDto requestDto;
    private ItemRequestResponseDto responseDto;
    private Item item;
    private ItemDto itemDto;

    @BeforeEach
    void setUp() {
        requestor = new User();
        requestor.setId(1L);
        requestor.setName("Requestor");
        requestor.setEmail("requestor@email.com");

        request = new ItemRequest();
        request.setId(1L);
        request.setDescription("Need item");
        request.setRequestor(requestor);
        request.setCreated(LocalDateTime.now());

        requestDto = new ItemRequestDto();
        requestDto.setDescription("Need item");

        item = new Item();
        item.setId(1L);
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(true);
        item.setOwner(requestor);
        item.setRequest(request);

        itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setName("Item");
        itemDto.setDescription("Description");
        itemDto.setAvailable(true);
        itemDto.setRequestId(1L);

        responseDto = new ItemRequestResponseDto();
        responseDto.setId(1L);
        responseDto.setDescription("Need item");
        responseDto.setCreated(request.getCreated());
        responseDto.setItems(List.of(itemDto));
    }

    @Test
    void createRequest_ShouldSaveRequest() {
        when(userService.getUserEntityById(anyLong())).thenReturn(requestor);
        when(requestRepository.save(any(ItemRequest.class))).thenReturn(request);

        ItemRequestDto result = requestService.createRequest(requestDto, requestor.getId());

        assertNotNull(result);
        assertEquals(requestDto.getDescription(), result.getDescription());
        verify(requestRepository).save(any(ItemRequest.class));
    }

    @Test
    void createRequest_WithEmptyDescription_ShouldThrowValidationException() {
        requestDto.setDescription("");

        assertThrows(ValidationException.class,
                () -> requestService.createRequest(requestDto, requestor.getId()));
    }

    @Test
    void getAllRequestsForUser_ShouldReturnRequests() {
        when(userService.getUserEntityById(anyLong())).thenReturn(requestor);
        when(requestRepository.findByRequestorIdOrderByCreatedDesc(anyLong()))
                .thenReturn(List.of(request));

        List<ItemRequestDto> result = requestService.getAllRequestsForUser(requestor.getId());

        assertEquals(1, result.size());
        assertEquals(request.getDescription(), result.get(0).getDescription());
    }

    @Test
    void getAllRequests_ShouldReturnPaginatedRequests() {
        PageRequest page = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"));
        Page<ItemRequest> pageResult = new PageImpl<>(List.of(request));

        when(requestRepository.findByRequestorIdNot(anyLong(), any(PageRequest.class)))
                .thenReturn(pageResult);

        List<ItemRequestDto> result = requestService.getAllRequests(2L, 0, 10);

        assertEquals(1, result.size());
        assertEquals(request.getDescription(), result.get(0).getDescription());
    }

    @Test
    void getAllRequests_WithInvalidFrom_ShouldThrowValidationException() {
        assertThrows(ValidationException.class,
                () -> requestService.getAllRequests(1L, -1, 10));
    }

    @Test
    void getAllRequests_WithInvalidSize_ShouldThrowValidationException() {
        assertThrows(ValidationException.class,
                () -> requestService.getAllRequests(1L, 0, 0));
    }

    @Test
    void getRequestById_ShouldReturnRequestWithItems() {
        when(requestRepository.findById(anyLong())).thenReturn(Optional.of(request));
        when(itemRepository.findByRequestId(anyLong())).thenReturn(List.of(item));
        when(itemMapper.toSimpleItemDto(any(Item.class))).thenReturn(itemDto);

        ItemRequestResponseDto result = requestService.getRequestById(request.getId());

        assertNotNull(result);
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void getRequestById_WithInvalidId_ShouldThrowNotFoundException() {
        when(requestRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> requestService.getRequestById(999L));
    }

    @Test
    void getRequestById_WithNoItems_ShouldReturnEmptyItemsList() {
        when(requestRepository.findById(anyLong())).thenReturn(Optional.of(request));
        when(itemRepository.findByRequestId(anyLong())).thenReturn(Collections.emptyList());

        ItemRequestResponseDto result = requestService.getRequestById(request.getId());

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
    }
}

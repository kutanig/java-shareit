package ru.practicum.shareit.request;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ItemRequestServiceImpl.class, ItemRequestMapper.class, ItemMapper.class})
class ItemRequestServiceIntegrationTest {

    @Autowired
    private ItemRequestServiceImpl itemRequestService;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private User requestor;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        itemRequestRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();

        requestor = userRepository.save(User.builder()
                .name("Requestor")
                .email("requestor@email.com")
                .build());

        anotherUser = userRepository.save(User.builder()
                .name("Another User")
                .email("another@email.com")
                .build());
    }

    private ItemRequestDto createTestRequestDto() {
        return ItemRequestDto.builder()
                .description("Need a drill for home repairs")
                .build();
    }

    @Test
    void createRequest_shouldSaveRequestToDatabase() {
        ItemRequestDto requestDto = createTestRequestDto();

        ItemRequestDto savedRequest = itemRequestService.createRequest(requestDto, requestor.getId());

        assertNotNull(savedRequest.getId());
        assertEquals("Need a drill for home repairs", savedRequest.getDescription());
        assertEquals(requestor.getId(), savedRequest.getRequestorId());
        assertNotNull(savedRequest.getCreated());

        ItemRequest dbRequest = itemRequestRepository.findById(savedRequest.getId()).orElseThrow();
        assertEquals("Need a drill for home repairs", dbRequest.getDescription());
        assertEquals(requestor.getId(), dbRequest.getRequestor().getId());
    }

    @Test
    void createRequest_withEmptyDescription_shouldThrowException() {
        ItemRequestDto requestDto = ItemRequestDto.builder().description("").build();

        assertThrows(ValidationException.class, () ->
                itemRequestService.createRequest(requestDto, requestor.getId()));
    }

    @Test
    void getAllRequestsForUser_shouldReturnUserRequests() {
        ItemRequestDto request1 = itemRequestService.createRequest(createTestRequestDto(), requestor.getId());
        ItemRequestDto request2 = itemRequestService.createRequest(
                ItemRequestDto.builder().description("Need a ladder").build(),
                requestor.getId());

        itemRequestService.createRequest(createTestRequestDto(), anotherUser.getId());

        List<ItemRequestDto> requests = itemRequestService.getAllRequestsForUser(requestor.getId());

        assertEquals(2, requests.size());
        assertTrue(requests.stream().anyMatch(r -> r.getId().equals(request1.getId())));
        assertTrue(requests.stream().anyMatch(r -> r.getId().equals(request2.getId())));
        assertTrue(requests.stream().allMatch(r -> r.getRequestorId().equals(requestor.getId())));
    }

    @Test
    void getAllRequests_shouldReturnOtherUsersRequests() {
        itemRequestService.createRequest(createTestRequestDto(), requestor.getId());
        itemRequestService.createRequest(
                ItemRequestDto.builder().description("Second request").build(),
                requestor.getId());

        ItemRequestDto anotherRequest1 = itemRequestService.createRequest(
                ItemRequestDto.builder().description("Another user request 1").build(),
                anotherUser.getId());
        ItemRequestDto anotherRequest2 = itemRequestService.createRequest(
                ItemRequestDto.builder().description("Another user request 2").build(),
                anotherUser.getId());

        List<ItemRequestDto> requests = itemRequestService.getAllRequests(requestor.getId(), 0, 10);

        assertEquals(2, requests.size());
        assertTrue(requests.stream().anyMatch(r -> r.getId().equals(anotherRequest1.getId())));
        assertTrue(requests.stream().anyMatch(r -> r.getId().equals(anotherRequest2.getId())));
        assertTrue(requests.stream().noneMatch(r -> r.getRequestorId().equals(requestor.getId())));
    }

    @Test
    void getAllRequests_withPagination_shouldReturnCorrectPage() {
        for (int i = 1; i <= 5; i++) {
            itemRequestService.createRequest(
                    ItemRequestDto.builder().description("Request " + i).build(),
                    anotherUser.getId());
        }

        List<ItemRequestDto> page1 = itemRequestService.getAllRequests(requestor.getId(), 0, 2);
        assertEquals(2, page1.size());

        List<ItemRequestDto> page2 = itemRequestService.getAllRequests(requestor.getId(), 2, 2);
        assertEquals(2, page2.size());

        List<ItemRequestDto> page3 = itemRequestService.getAllRequests(requestor.getId(), 4, 2);
        assertEquals(1, page3.size());

        long totalUniqueIds = page1.stream().map(ItemRequestDto::getId).count() +
                page2.stream().map(ItemRequestDto::getId).count() +
                page3.stream().map(ItemRequestDto::getId).count();
        assertEquals(5, totalUniqueIds);
    }

    @Test
    void getAllRequests_withInvalidPagination_shouldThrowException() {
        assertThrows(ValidationException.class, () ->
                itemRequestService.getAllRequests(requestor.getId(), -1, 10));

        assertThrows(ValidationException.class, () ->
                itemRequestService.getAllRequests(requestor.getId(), 0, 0));
    }

    @Test
    void getRequestById_shouldReturnRequestWithItems() {
        ItemRequestDto requestDto = itemRequestService.createRequest(createTestRequestDto(), requestor.getId());

        ItemRequest request = itemRequestRepository.findById(requestDto.getId())
                .orElseThrow(() -> new NotFoundException("Request not found"));

        Item item1 = itemRepository.save(Item.builder()
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .owner(anotherUser)
                .request(request)
                .build());

        Item item2 = itemRepository.save(Item.builder()
                .name("Hammer Drill")
                .description("Even more powerful")
                .available(true)
                .owner(anotherUser)
                .request(request)
                .build());

        ItemRequestResponseDto response = itemRequestService.getRequestById(requestDto.getId());

        assertEquals(requestDto.getId(), response.getId());
        assertEquals("Need a drill for home repairs", response.getDescription());
        assertEquals(2, response.getItems().size());
        assertTrue(response.getItems().stream().anyMatch(i -> i.getId().equals(item1.getId())));
        assertTrue(response.getItems().stream().anyMatch(i -> i.getId().equals(item2.getId())));
    }

    @Test
    void getRequestById_withNonExistingId_shouldThrowException() {
        assertThrows(NotFoundException.class, () ->
                itemRequestService.getRequestById(9999L));
    }
}

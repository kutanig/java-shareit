package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
@ExtendWith(MockitoExtension.class)
public class ItemRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemRequestService requestService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Long userId = 1L;
    private final String userIdHeader = "X-Sharer-User-Id";
    private final LocalDateTime created = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createRequest_ShouldReturnCreatedRequest() throws Exception {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setDescription("Need a drill");

        ItemRequestDto responseDto = new ItemRequestDto();
        responseDto.setId(1L);
        responseDto.setDescription("Need a drill");
        responseDto.setRequestorId(userId);
        responseDto.setCreated(created);

        when(requestService.createRequest(any(ItemRequestDto.class), eq(userId))).thenReturn(responseDto);

        mockMvc.perform(post("/requests")
                        .header(userIdHeader, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.description").value("Need a drill"))
                .andExpect(jsonPath("$.requestorId").value(userId));

        verify(requestService).createRequest(any(ItemRequestDto.class), eq(userId));
    }

    @Test
    void getAllRequestsForUser_ShouldReturnRequestsList() throws Exception {
        ItemRequestDto request1 = new ItemRequestDto(1L, "Need a drill", userId, created);
        ItemRequestDto request2 = new ItemRequestDto(2L, "Need a hammer", userId, created.plusHours(1));
        List<ItemRequestDto> requests = List.of(request1, request2);

        when(requestService.getAllRequestsForUser(userId)).thenReturn(requests);

        mockMvc.perform(get("/requests")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(requestService).getAllRequestsForUser(userId);
    }

    @Test
    void getAllRequests_ShouldReturnRequestsList() throws Exception {
        int from = 0;
        int size = 10;
        ItemRequestDto request1 = new ItemRequestDto(1L, "Need a drill", 2L, created);
        ItemRequestDto request2 = new ItemRequestDto(2L, "Need a hammer", 3L, created.plusHours(1));
        List<ItemRequestDto> requests = List.of(request1, request2);

        when(requestService.getAllRequests(eq(userId), eq(from), eq(size))).thenReturn(requests);

        mockMvc.perform(get("/requests/all?from={from}&size={size}", from, size)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(requestService).getAllRequests(eq(userId), eq(from), eq(size));
    }

    @Test
    void getRequestById_ShouldReturnRequest() throws Exception {
        Long requestId = 1L;
        ItemRequestResponseDto responseDto = new ItemRequestResponseDto();
        responseDto.setId(requestId);
        responseDto.setDescription("Need a drill");
        responseDto.setRequestorId(userId);
        responseDto.setCreated(created);

        when(requestService.getRequestById(requestId)).thenReturn(responseDto);

        mockMvc.perform(get("/requests/{requestId}", requestId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.description").value("Need a drill"))
                .andExpect(jsonPath("$.requestorId").value(userId));

        verify(requestService).getRequestById(requestId);
    }
}

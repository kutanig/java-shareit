package ru.practicum.shareit.booking;

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
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@ExtendWith(MockitoExtension.class)
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Long userId = 1L;
    private final String userIdHeader = "X-Sharer-User-Id";
    private final LocalDateTime start = LocalDateTime.now().plusDays(1);
    private final LocalDateTime end = LocalDateTime.now().plusDays(2);

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createBooking_ShouldReturnCreatedBooking() throws Exception {
        BookingRequestDto requestDto = BookingRequestDto.builder()
                .start(start)
                .end(end)
                .itemId(1L)
                .build();

        BookingResponseDto responseDto = BookingResponseDto.builder()
                .id(1L)
                .start(start)
                .end(end)
                .status(BookingStatus.WAITING)
                .booker(new BookingResponseDto.BookerDto(userId, "Booker Name"))
                .item(new BookingResponseDto.ItemDto(1L, "Item Name"))
                .build();

        when(bookingService.createBooking(any(BookingRequestDto.class), eq(userId))).thenReturn(responseDto);

        mockMvc.perform(post("/bookings")
                        .header(userIdHeader, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.booker.id").value(userId))
                .andExpect(jsonPath("$.item.id").value(1L));

        verify(bookingService).createBooking(any(BookingRequestDto.class), eq(userId));
    }

    @Test
    void approveBooking_ShouldReturnApprovedBooking() throws Exception {
        Long bookingId = 1L;
        boolean approved = true;
        BookingResponseDto responseDto = BookingResponseDto.builder()
                .id(bookingId)
                .start(start)
                .end(end)
                .status(BookingStatus.APPROVED)
                .booker(new BookingResponseDto.BookerDto(2L, "Booker Name"))
                .item(new BookingResponseDto.ItemDto(1L, "Item Name"))
                .build();

        when(bookingService.approveBooking(eq(bookingId), eq(userId), eq(approved))).thenReturn(responseDto);

        mockMvc.perform(patch("/bookings/{bookingId}?approved={approved}", bookingId, approved)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingService).approveBooking(eq(bookingId), eq(userId), eq(approved));
    }

    @Test
    void getBookingById_ShouldReturnBooking() throws Exception {
        Long bookingId = 1L;
        BookingResponseDto responseDto = BookingResponseDto.builder()
                .id(bookingId)
                .start(start)
                .end(end)
                .status(BookingStatus.APPROVED)
                .booker(new BookingResponseDto.BookerDto(2L, "Booker Name"))
                .item(new BookingResponseDto.ItemDto(1L, "Item Name"))
                .build();

        when(bookingService.getBookingById(eq(bookingId), eq(userId))).thenReturn(responseDto);

        mockMvc.perform(get("/bookings/{bookingId}", bookingId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.item.id").value(1L))
                .andExpect(jsonPath("$.booker.id").value(2L));

        verify(bookingService).getBookingById(eq(bookingId), eq(userId));
    }

    @Test
    void getAllBookingsForUser_ShouldReturnBookingsList() throws Exception {
        String state = "ALL";
        int from = 0;
        int size = 10;
        List<BookingResponseDto> bookings = List.of(
                BookingResponseDto.builder()
                        .id(1L)
                        .start(start)
                        .end(end)
                        .status(BookingStatus.APPROVED)
                        .booker(new BookingResponseDto.BookerDto(userId, "User Name"))
                        .item(new BookingResponseDto.ItemDto(1L, "Item 1"))
                        .build(),
                BookingResponseDto.builder()
                        .id(2L)
                        .start(start.plusDays(1))
                        .end(end.plusDays(1))
                        .status(BookingStatus.WAITING)
                        .booker(new BookingResponseDto.BookerDto(userId, "User Name"))
                        .item(new BookingResponseDto.ItemDto(2L, "Item 2"))
                        .build()
        );

        when(bookingService.getAllBookingsForUser(eq(userId), eq(state), eq(from), eq(size))).thenReturn(bookings);

        mockMvc.perform(get("/bookings?state={state}&from={from}&size={size}", state, from, size)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));

        verify(bookingService).getAllBookingsForUser(eq(userId), eq(state), eq(from), eq(size));
    }

    @Test
    void getAllBookingsForOwner_ShouldReturnBookingsList() throws Exception {
        String state = "ALL";
        int from = 0;
        int size = 10;
        List<BookingResponseDto> bookings = List.of(
                BookingResponseDto.builder()
                        .id(1L)
                        .start(start)
                        .end(end)
                        .status(BookingStatus.APPROVED)
                        .booker(new BookingResponseDto.BookerDto(2L, "Booker Name"))
                        .item(new BookingResponseDto.ItemDto(1L, "Item 1"))
                        .build(),
                BookingResponseDto.builder()
                        .id(2L)
                        .start(start.plusDays(1))
                        .end(end.plusDays(1))
                        .status(BookingStatus.WAITING)
                        .booker(new BookingResponseDto.BookerDto(3L, "Another Booker"))
                        .item(new BookingResponseDto.ItemDto(1L, "Item 1"))
                        .build()
        );

        when(bookingService.getAllBookingsForOwner(eq(userId), eq(state), eq(from), eq(size))).thenReturn(bookings);

        mockMvc.perform(get("/bookings/owner?state={state}&from={from}&size={size}", state, from, size)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].item.id").value(1L))
                .andExpect(jsonPath("$[1].item.id").value(1L));

        verify(bookingService).getAllBookingsForOwner(eq(userId), eq(state), eq(from), eq(size));
    }
}

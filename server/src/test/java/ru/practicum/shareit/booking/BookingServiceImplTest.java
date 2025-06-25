package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.SelfBookingException;
import ru.practicum.shareit.exception.UnavailableItemException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;
    private Booking booking;
    private BookingRequestDto bookingRequestDto;
    private BookingResponseDto bookingResponseDto;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Owner");
        owner.setEmail("owner@email.com");

        booker = new User();
        booker.setId(2L);
        booker.setName("Booker");
        booker.setEmail("booker@email.com");

        item = new Item();
        item.setId(1L);
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(true);
        item.setOwner(owner);

        booking = new Booking();
        booking.setId(1L);
        booking.setStart(LocalDateTime.now().plusDays(1));
        booking.setEnd(LocalDateTime.now().plusDays(2));
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        bookingRequestDto = new BookingRequestDto();
        bookingRequestDto.setItemId(1L);
        bookingRequestDto.setStart(LocalDateTime.now().plusDays(1));
        bookingRequestDto.setEnd(LocalDateTime.now().plusDays(2));

        bookingResponseDto = new BookingResponseDto();
        bookingResponseDto.setId(1L);
        bookingResponseDto.setStart(booking.getStart());
        bookingResponseDto.setEnd(booking.getEnd());
        bookingResponseDto.setStatus(BookingStatus.WAITING);
    }

    @Test
    void createBooking_ShouldCreateBooking() {
        when(userService.getUserEntityById(anyLong())).thenReturn(booker);
        when(itemService.getItemEntityById(anyLong())).thenReturn(item);
        when(bookingMapper.toBooking(any(BookingRequestDto.class))).thenReturn(booking);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toBookingResponseDto(any(Booking.class))).thenReturn(bookingResponseDto);

        BookingResponseDto result = bookingService.createBooking(bookingRequestDto, booker.getId());

        assertNotNull(result);
        assertEquals(bookingResponseDto.getId(), result.getId());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_WithUnavailableItem_ShouldThrowException() {
        item.setAvailable(false);
        when(userService.getUserEntityById(anyLong())).thenReturn(booker);
        when(itemService.getItemEntityById(anyLong())).thenReturn(item);

        assertThrows(UnavailableItemException.class,
                () -> bookingService.createBooking(bookingRequestDto, booker.getId()));
    }

    @Test
    void createBooking_ByOwner_ShouldThrowException() {
        when(userService.getUserEntityById(anyLong())).thenReturn(owner);
        when(itemService.getItemEntityById(anyLong())).thenReturn(item);

        assertThrows(SelfBookingException.class,
                () -> bookingService.createBooking(bookingRequestDto, owner.getId()));
    }

    @Test
    void createBooking_WithInvalidDates_ShouldThrowException() {
        bookingRequestDto.setEnd(bookingRequestDto.getStart().minusDays(1));
        when(userService.getUserEntityById(anyLong())).thenReturn(booker);
        when(itemService.getItemEntityById(anyLong())).thenReturn(item);

        assertThrows(ValidationException.class,
                () -> bookingService.createBooking(bookingRequestDto, booker.getId()));
    }

    @Test
    void approveBooking_ShouldApproveBooking() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toBookingResponseDto(any(Booking.class))).thenReturn(bookingResponseDto);

        bookingResponseDto.setStatus(BookingStatus.APPROVED);
        BookingResponseDto result = bookingService.approveBooking(1L, owner.getId(), true);

        assertEquals(BookingStatus.APPROVED, result.getStatus());
    }

    @Test
    void approveBooking_ShouldRejectBooking() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toBookingResponseDto(any(Booking.class))).thenReturn(bookingResponseDto);

        bookingResponseDto.setStatus(BookingStatus.REJECTED);
        BookingResponseDto result = bookingService.approveBooking(1L, owner.getId(), false);

        assertEquals(BookingStatus.REJECTED, result.getStatus());
    }

    @Test
    void approveBooking_ByNotOwner_ShouldThrowException() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class,
                () -> bookingService.approveBooking(1L, 999L, true));
    }

    @Test
    void approveBooking_AlreadyApproved_ShouldThrowException() {
        booking.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(ValidationException.class,
                () -> bookingService.approveBooking(1L, owner.getId(), true));
    }

    @Test
    void getBookingById_ShouldReturnBooking() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(bookingMapper.toBookingResponseDto(any(Booking.class))).thenReturn(bookingResponseDto);

        BookingResponseDto result = bookingService.getBookingById(1L, booker.getId());

        assertNotNull(result);
        assertEquals(bookingResponseDto.getId(), result.getId());
    }

    @Test
    void getBookingById_UnauthorizedAccess_ShouldThrowException() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        assertThrows(NotFoundException.class,
                () -> bookingService.getBookingById(1L, 999L));
    }

    @Test
    void getAllBookingsForUser_ShouldReturnBookings() {
        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(userService.getUserEntityById(anyLong())).thenReturn(booker);
        when(bookingRepository.findByBookerId(anyLong(), any(PageRequest.class))).thenReturn(page);
        when(bookingMapper.toBookingResponseDto(any(Booking.class))).thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getAllBookingsForUser(booker.getId(), "ALL", 0, 10);

        assertEquals(1, result.size());
        assertEquals(bookingResponseDto.getId(), result.get(0).getId());
    }

    @Test
    void getAllBookingsForOwner_ShouldReturnBookings() {
        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(userService.getUserEntityById(anyLong())).thenReturn(owner);
        when(bookingRepository.findByItemOwnerId(anyLong(), any(PageRequest.class))).thenReturn(page);
        when(bookingMapper.toBookingResponseDto(any(Booking.class))).thenReturn(bookingResponseDto);

        List<BookingResponseDto> result = bookingService.getAllBookingsForOwner(owner.getId(), "ALL", 0, 10);

        assertEquals(1, result.size());
        assertEquals(bookingResponseDto.getId(), result.get(0).getId());
    }

    @Test
    void getAllBookingsForUser_WithInvalidState_ShouldThrowException() {
        when(userService.getUserEntityById(anyLong())).thenReturn(booker);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.getAllBookingsForUser(booker.getId(), "INVALID", 0, 10));
    }
}

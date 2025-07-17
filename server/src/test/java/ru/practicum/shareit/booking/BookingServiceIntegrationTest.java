package ru.practicum.shareit.booking;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.SelfBookingException;
import ru.practicum.shareit.exception.UnavailableItemException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BookingServiceImpl.class, BookingMapper.class})
class BookingServiceIntegrationTest {

    @Autowired
    private BookingServiceImpl bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User booker;
    private Item availableItem;
    private Item unavailableItem;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder().name("Owner").email("owner@email.com").build());
        booker = userRepository.save(User.builder().name("Booker").email("booker@email.com").build());

        availableItem = itemRepository.save(Item.builder()
                .name("Available Item")
                .description("Available Description")
                .available(true)
                .owner(owner)
                .build());

        unavailableItem = itemRepository.save(Item.builder()
                .name("Unavailable Item")
                .description("Unavailable Description")
                .available(false)
                .owner(owner)
                .build());
    }

    private BookingRequestDto createTestBookingRequest(Long itemId) {
        return BookingRequestDto.builder()
                .itemId(itemId)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .build();
    }

    @Test
    void createBooking_shouldSaveBookingToDatabase() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());

        BookingResponseDto savedBooking = bookingService.createBooking(bookingRequest, booker.getId());

        assertNotNull(savedBooking.getId());
        assertEquals(availableItem.getId(), savedBooking.getItem().getId());
        assertEquals(booker.getId(), savedBooking.getBooker().getId());
        assertEquals(BookingStatus.WAITING, savedBooking.getStatus());

        Booking dbBooking = bookingRepository.findById(savedBooking.getId()).orElseThrow();
        assertEquals(availableItem.getId(), dbBooking.getItem().getId());
        assertEquals(booker.getId(), dbBooking.getBooker().getId());
    }

    @Test
    void createBooking_withUnavailableItem_shouldThrowException() {
        BookingRequestDto bookingRequest = createTestBookingRequest(unavailableItem.getId());

        assertThrows(UnavailableItemException.class, () ->
                bookingService.createBooking(bookingRequest, booker.getId()));
    }

    @Test
    void createBooking_withSelfBooking_shouldThrowException() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());

        assertThrows(SelfBookingException.class, () ->
                bookingService.createBooking(bookingRequest, owner.getId()));
    }

    @Test
    void createBooking_withInvalidDates_shouldThrowException() {
        BookingRequestDto bookingRequest = BookingRequestDto.builder()
                .itemId(availableItem.getId())
                .start(LocalDateTime.now().plusDays(2))
                .end(LocalDateTime.now().plusDays(1))
                .build();

        assertThrows(ValidationException.class, () ->
                bookingService.createBooking(bookingRequest, booker.getId()));
    }

    @Test
    void approveBooking_shouldUpdateStatusInDatabase() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());
        BookingResponseDto savedBooking = bookingService.createBooking(bookingRequest, booker.getId());

        BookingResponseDto approvedBooking = bookingService.approveBooking(savedBooking.getId(), owner.getId(), true);

        assertEquals(BookingStatus.APPROVED, approvedBooking.getStatus());

        Booking dbBooking = bookingRepository.findById(savedBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.APPROVED, dbBooking.getStatus());
    }

    @Test
    void approveBooking_withWrongOwner_shouldThrowException() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());
        BookingResponseDto savedBooking = bookingService.createBooking(bookingRequest, booker.getId());

        assertThrows(ValidationException.class, () ->
                bookingService.approveBooking(savedBooking.getId(), booker.getId(), true));
    }

    @Test
    void approveBooking_alreadyApproved_shouldThrowException() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());
        BookingResponseDto savedBooking = bookingService.createBooking(bookingRequest, booker.getId());
        bookingService.approveBooking(savedBooking.getId(), owner.getId(), true);

        assertThrows(ValidationException.class, () ->
                bookingService.approveBooking(savedBooking.getId(), owner.getId(), false));
    }

    @Test
    void getBookingById_shouldReturnBookingFromDatabase() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());
        BookingResponseDto savedBooking = bookingService.createBooking(bookingRequest, booker.getId());

        BookingResponseDto foundBooking = bookingService.getBookingById(savedBooking.getId(), booker.getId());

        assertEquals(savedBooking.getId(), foundBooking.getId());
        assertEquals(availableItem.getId(), foundBooking.getItem().getId());
        assertEquals(booker.getId(), foundBooking.getBooker().getId());
    }

    @Test
    void getBookingById_withUnauthorizedUser_shouldThrowException() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());
        BookingResponseDto savedBooking = bookingService.createBooking(bookingRequest, booker.getId());

        User otherUser = userRepository.save(User.builder().name("Other").email("other@email.com").build());

        assertThrows(NotFoundException.class, () ->
                bookingService.getBookingById(savedBooking.getId(), otherUser.getId()));
    }

    @Test
    void getAllBookingsForUser_shouldReturnUserBookings() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());
        BookingResponseDto savedBooking = bookingService.createBooking(bookingRequest, booker.getId());

        List<BookingResponseDto> bookings = bookingService.getAllBookingsForUser(
                booker.getId(), "ALL", 0, 10);

        assertEquals(1, bookings.size());
        assertEquals(savedBooking.getId(), bookings.get(0).getId());
    }

    @Test
    void getAllBookingsForOwner_shouldReturnOwnerBookings() {
        BookingRequestDto bookingRequest = createTestBookingRequest(availableItem.getId());
        BookingResponseDto savedBooking = bookingService.createBooking(bookingRequest, booker.getId());

        List<BookingResponseDto> bookings = bookingService.getAllBookingsForOwner(
                owner.getId(), "ALL", 0, 10);

        assertEquals(1, bookings.size());
        assertEquals(savedBooking.getId(), bookings.get(0).getId());
    }

    @Test
    void getAllBookingsForUser_withDifferentStates_shouldReturnFilteredBookings() {
        Booking pastBooking = bookingRepository.save(Booking.builder()
                .start(LocalDateTime.now().minusDays(2))
                .end(LocalDateTime.now().minusDays(1))
                .item(availableItem)
                .booker(booker)
                .status(BookingStatus.APPROVED)
                .build());

        Booking currentBooking = bookingRepository.save(Booking.builder()
                .start(LocalDateTime.now().minusHours(1))
                .end(LocalDateTime.now().plusHours(1))
                .item(availableItem)
                .booker(booker)
                .status(BookingStatus.APPROVED)
                .build());

        Booking futureBooking = bookingRepository.save(Booking.builder()
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .item(availableItem)
                .booker(booker)
                .status(BookingStatus.WAITING)
                .build());

        Booking rejectedBooking = bookingRepository.save(Booking.builder()
                .start(LocalDateTime.now().plusDays(3))
                .end(LocalDateTime.now().plusDays(4))
                .item(availableItem)
                .booker(booker)
                .status(BookingStatus.REJECTED)
                .build());

        assertEquals(4, bookingService.getAllBookingsForUser(booker.getId(), "ALL", 0, 10).size());
        assertEquals(1, bookingService.getAllBookingsForUser(booker.getId(), "CURRENT", 0, 10).size());
        assertEquals(1, bookingService.getAllBookingsForUser(booker.getId(), "PAST", 0, 10).size());
        assertEquals(2, bookingService.getAllBookingsForUser(booker.getId(), "FUTURE", 0, 10).size());
        assertEquals(1, bookingService.getAllBookingsForUser(booker.getId(), "WAITING", 0, 10).size());
        assertEquals(1, bookingService.getAllBookingsForUser(booker.getId(), "REJECTED", 0, 10).size());
    }
}

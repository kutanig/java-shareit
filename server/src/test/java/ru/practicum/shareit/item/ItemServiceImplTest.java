package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.comment.Comment;
import ru.practicum.shareit.comment.CommentRepository;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;
    private User booker;
    private Item item;
    private ItemDto itemDto;
    private ItemRequest request;

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

        request = new ItemRequest();
        request.setId(1L);
        request.setDescription("Need item");
        request.setRequestor(booker);
        request.setCreated(LocalDateTime.now());

        item = new Item();
        item.setId(1L);
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setRequest(request);

        itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setName("Item");
        itemDto.setDescription("Description");
        itemDto.setAvailable(true);
        itemDto.setRequestId(1L);
    }

    @Test
    void addItem_ShouldSaveItem() {
        when(userService.getUserEntityById(anyLong())).thenReturn(owner);
        when(itemRequestRepository.findById(anyLong())).thenReturn(Optional.of(request));
        when(itemMapper.toItem(any(ItemDto.class), any(User.class), any(ItemRequest.class))).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(itemMapper.toItemDto(any(Item.class), anyLong())).thenReturn(itemDto);

        ItemDto result = itemService.addItem(itemDto, owner.getId());

        assertNotNull(result);
        assertEquals(itemDto.getName(), result.getName());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void addItem_WithOwnRequest_ShouldThrowValidationException() {
        request.setRequestor(owner);
        when(userService.getUserEntityById(anyLong())).thenReturn(owner);
        when(itemRequestRepository.findById(anyLong())).thenReturn(Optional.of(request));

        assertThrows(ValidationException.class,
                () -> itemService.addItem(itemDto, owner.getId()));
    }

    @Test
    void updateItem_ShouldUpdateFields() {
        ItemMapper realMapper = new ItemMapper(bookingRepository);

        itemService = new ItemServiceImpl(itemRepository, userService,
                itemRequestRepository, bookingRepository,
                commentRepository, realMapper);

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated");
        updateDto.setDescription("New Desc");
        updateDto.setAvailable(false);

        ItemDto result = itemService.updateItem(1L, updateDto, owner.getId());

        assertEquals("Updated", result.getName());
        assertEquals("New Desc", result.getDescription());
        assertFalse(result.getAvailable());
    }

    @Test
    void updateItem_ByNotOwner_ShouldThrowNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        assertThrows(NotFoundException.class,
                () -> itemService.updateItem(1L, itemDto, 999L));
    }

    @Test
    void getItemById_ShouldReturnItem() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(itemMapper.toItemDto(any(Item.class), anyLong())).thenReturn(itemDto);

        ItemDto result = itemService.getItemById(1L, owner.getId());

        assertEquals(itemDto.getName(), result.getName());
    }

    @Test
    void getItemById_WithInvalidId_ShouldThrowNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.getItemById(999L, owner.getId()));
    }

    @Test
    void getAllItemsByOwner_ShouldReturnItems() {
        when(itemRepository.findByOwnerIdOrderById(anyLong())).thenReturn(List.of(item));
        when(itemMapper.toItemDto(any(Item.class), anyLong())).thenReturn(itemDto);

        List<ItemDto> result = itemService.getAllItemsByOwner(owner.getId());

        assertEquals(1, result.size());
        assertEquals(itemDto.getName(), result.get(0).getName());
    }

    @Test
    void searchItems_ShouldReturnAvailableItems() {
        when(itemRepository.searchAvailableItems(anyString())).thenReturn(List.of(item));
        when(itemMapper.toItemDto(any(Item.class), any())).thenReturn(itemDto);

        List<ItemDto> result = itemService.searchItems("item");

        assertEquals(1, result.size());
        assertEquals(itemDto.getName(), result.get(0).getName());
    }

    @Test
    void searchItems_WithEmptyText_ShouldReturnEmptyList() {
        List<ItemDto> result = itemService.searchItems("");

        assertTrue(result.isEmpty());
        verify(itemRepository, never()).searchAvailableItems(anyString());
    }

    @Test
    void addComment_ShouldSaveComment() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStart(LocalDateTime.now().minusDays(2));
        booking.setEnd(LocalDateTime.now().minusDays(1));
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.APPROVED);

        Comment comment = new Comment();
        comment.setId(1L);
        comment.setText("Text");
        comment.setItem(item);
        comment.setAuthor(booker);
        comment.setCreated(LocalDateTime.now());

        CommentDto commentDto = new CommentDto();
        commentDto.setText("Text");

        when(userService.getUserEntityById(anyLong())).thenReturn(booker);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(bookingRepository.findCompletedBookingsForComment(anyLong(), anyLong()))
                .thenReturn(List.of(booking));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = itemService.addComment(1L, booker.getId(), commentDto);

        assertNotNull(result);
        assertEquals("Text", result.getText());
    }

    @Test
    void addComment_WithoutBooking_ShouldThrowValidationException() {
        when(userService.getUserEntityById(anyLong())).thenReturn(booker);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(bookingRepository.findCompletedBookingsForComment(anyLong(), anyLong()))
                .thenReturn(Collections.emptyList());

        CommentDto commentDto = new CommentDto();
        commentDto.setText("Text");

        assertThrows(ValidationException.class,
                () -> itemService.addComment(1L, booker.getId(), commentDto));
    }

    @Test
    void getItemEntityById_ShouldReturnItem() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        Item result = itemService.getItemEntityById(1L);

        assertEquals(item.getName(), result.getName());
    }

    @Test
    void getItemEntityById_WithInvalidId_ShouldThrowNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> itemService.getItemEntityById(999L));
    }
}

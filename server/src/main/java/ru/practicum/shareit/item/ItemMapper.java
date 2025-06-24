package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.comment.Comment;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemMapper {
    private final BookingRepository bookingRepository;

    public ItemDto toItemDto(Item item, Long userId) {
        if (item == null) {
            return null;
        }

        boolean isOwner = item.getOwner().getId().equals(userId);

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .ownerId(item.getOwner().getId())
                .requestId(item.getRequest() != null ? item.getRequest().getId() : null)
                .lastBooking(isOwner ? getLastBookingInfo(item) : null)
                .nextBooking(isOwner ? getNextBookingInfo(item) : null)
                .comments(mapCommentsToDto(item.getComments()))
                .build();
    }

    public Item toItem(ItemDto itemDto, User owner, ItemRequest request) {
        if (itemDto == null) {
            return null;
        }

        return Item.builder()
                .id(itemDto.getId())
                .name(itemDto.getName())
                .description(itemDto.getDescription())
                .available(itemDto.getAvailable())
                .owner(owner)
                .request(request)
                .build();
    }

    private ItemDto.BookingInfoDto getLastBookingInfo(Item item) {
        if (item.getId() == null || !item.getAvailable()) {
            return null;
        }

        Optional<Booking> lastBooking = bookingRepository.findFirstByItemIdAndStartBeforeAndStatusOrderByStartDesc(
                item.getId(), LocalDateTime.now(), BookingStatus.APPROVED);

        return lastBooking.map(booking -> ItemDto.BookingInfoDto.builder()
                        .id(booking.getId())
                        .bookerId(booking.getBooker().getId())
                        .build())
                .orElse(null);
    }

    private ItemDto.BookingInfoDto getNextBookingInfo(Item item) {
        if (item.getId() == null || !item.getAvailable()) {
            return null;
        }

        Optional<Booking> nextBooking = bookingRepository.findFirstByItemIdAndStartAfterAndStatusOrderByStartAsc(
                item.getId(), LocalDateTime.now(), BookingStatus.APPROVED);

        return nextBooking.map(booking -> ItemDto.BookingInfoDto.builder()
                        .id(booking.getId())
                        .bookerId(booking.getBooker().getId())
                        .build())
                .orElse(null);
    }

    private List<CommentDto> mapCommentsToDto(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        return comments.stream()
                .map(comment -> CommentDto.builder()
                        .id(comment.getId())
                        .text(comment.getText())
                        .authorName(comment.getAuthor().getName())
                        .created(comment.getCreated())
                        .build())
                .collect(Collectors.toList());
    }
}

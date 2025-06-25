package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.comment.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

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

@WebMvcTest(ItemController.class)
@ExtendWith(MockitoExtension.class)
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Long userId = 1L;
    private final String userIdHeader = "X-Sharer-User-Id";

    @Test
    void addItem_ShouldReturnCreatedItem() throws Exception {
        ItemDto itemDto = ItemDto.builder()
                .name("Item Name")
                .description("Item Description")
                .available(true)
                .build();
        ItemDto createdItem = ItemDto.builder()
                .id(1L)
                .name("Item Name")
                .description("Item Description")
                .available(true)
                .ownerId(userId)
                .build();

        when(itemService.addItem(any(ItemDto.class), eq(userId))).thenReturn(createdItem);

        mockMvc.perform(post("/items")
                        .header(userIdHeader, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Item Name"))
                .andExpect(jsonPath("$.ownerId").value(userId));

        verify(itemService).addItem(any(ItemDto.class), eq(userId));
    }

    @Test
    void updateItem_ShouldReturnUpdatedItem() throws Exception {
        Long itemId = 1L;
        ItemDto itemDto = ItemDto.builder()
                .name("Updated Name")
                .description("Updated Description")
                .available(false)
                .build();
        ItemDto updatedItem = ItemDto.builder()
                .id(itemId)
                .name("Updated Name")
                .description("Updated Description")
                .available(false)
                .ownerId(userId)
                .build();

        when(itemService.updateItem(eq(itemId), any(ItemDto.class), eq(userId))).thenReturn(updatedItem);

        mockMvc.perform(patch("/items/" + itemId)
                        .header(userIdHeader, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.ownerId").value(userId));

        verify(itemService).updateItem(eq(itemId), any(ItemDto.class), eq(userId));
    }

    @Test
    void getItemById_ShouldReturnItem() throws Exception {
        Long itemId = 1L;
        ItemDto itemDto = ItemDto.builder()
                .id(itemId)
                .name("Item Name")
                .description("Item Description")
                .available(true)
                .ownerId(userId)
                .build();

        when(itemService.getItemById(itemId, userId)).thenReturn(itemDto);

        mockMvc.perform(get("/items/" + itemId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value("Item Name"))
                .andExpect(jsonPath("$.ownerId").value(userId));

        verify(itemService).getItemById(itemId, userId);
    }

    @Test
    void getAllItemsByOwner_ShouldReturnItemList() throws Exception {
        List<ItemDto> items = List.of(
                ItemDto.builder().id(1L).name("Item 1").ownerId(userId).build(),
                ItemDto.builder().id(2L).name("Item 2").ownerId(userId).build()
        );

        when(itemService.getAllItemsByOwner(userId)).thenReturn(items);

        mockMvc.perform(get("/items")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$", hasSize(2)));

        verify(itemService).getAllItemsByOwner(userId);
    }

    @Test
    void searchItems_ShouldReturnMatchingItems() throws Exception {
        String searchText = "text";
        List<ItemDto> items = List.of(
                ItemDto.builder().id(1L).name("Item with text").available(true).build(),
                ItemDto.builder().id(2L).name("Another text item").available(true).build()
        );

        when(itemService.searchItems(searchText)).thenReturn(items);

        mockMvc.perform(get("/items/search")
                        .param("text", searchText)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$", hasSize(2)));

        verify(itemService).searchItems(searchText);
    }

    @Test
    void addComment_ShouldReturnCreatedComment() throws Exception {
        Long itemId = 1L;
        CommentDto commentDto = new CommentDto(null, "Comment text", null, null);
        CommentDto createdComment = new CommentDto(1L, "Comment text", "Author Name", LocalDateTime.now());

        when(itemService.addComment(eq(itemId), eq(userId), any(CommentDto.class))).thenReturn(createdComment);

        mockMvc.perform(post("/items/" + itemId + "/comment")
                        .header(userIdHeader, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.text").value("Comment text"))
                .andExpect(jsonPath("$.authorName").value("Author Name"));

        verify(itemService).addComment(eq(itemId), eq(userId), any(CommentDto.class));
    }
}

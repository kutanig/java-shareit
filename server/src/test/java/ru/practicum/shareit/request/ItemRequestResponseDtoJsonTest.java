package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestResponseDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeWithItems() throws Exception {
        ItemRequestResponseDto dto = new ItemRequestResponseDto();
        dto.setId(1L);
        dto.setDescription("Need a drill");
        dto.setRequestorId(2L);
        dto.setCreated(LocalDateTime.of(2023, 1, 1, 12, 0));

        ItemDto item1 = new ItemDto();
        item1.setId(1L);
        item1.setName("Drill");

        ItemDto item2 = new ItemDto();
        item2.setId(2L);
        item2.setName("Hammer");

        dto.setItems(List.of(item1, item2));

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"description\":\"Need a drill\"");
        assertThat(json).contains("\"requestorId\":2");
        assertThat(json).contains("\"created\":\"2023-01-01T12:00:00\"");

        assertThat(json).contains("\"items\":[");
        assertThat(json).contains("{\"id\":1,\"name\":\"Drill\"");
        assertThat(json).contains("{\"id\":2,\"name\":\"Hammer\"");
    }

    @Test
    void shouldDeserializeWithItems() throws Exception {
        String json = "{\"id\":1,\"description\":\"Need a drill\","
                + "\"requestorId\":2,\"created\":\"2023-01-01T12:00:00\","
                + "\"items\":[{\"id\":1,\"name\":\"Drill\"},{\"id\":2,\"name\":\"Hammer\"}]}";

        ItemRequestResponseDto dto = objectMapper.readValue(json, ItemRequestResponseDto.class);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getDescription()).isEqualTo("Need a drill");
        assertThat(dto.getRequestorId()).isEqualTo(2L);
        assertThat(dto.getCreated()).isEqualTo(LocalDateTime.of(2023, 1, 1, 12, 0));
        assertThat(dto.getItems()).hasSize(2);
        assertThat(dto.getItems().get(0).getId()).isEqualTo(1L);
        assertThat(dto.getItems().get(0).getName()).isEqualTo("Drill");
        assertThat(dto.getItems().get(1).getId()).isEqualTo(2L);
        assertThat(dto.getItems().get(1).getName()).isEqualTo("Hammer");
    }
}

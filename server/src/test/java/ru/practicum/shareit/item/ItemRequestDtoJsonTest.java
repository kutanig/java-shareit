package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeToJsonWithCorrectDateFormat() throws Exception {
        ItemRequestDto dto = new ItemRequestDto(
                1L,
                "Need a drill",
                2L,
                LocalDateTime.of(2023, 1, 1, 12, 0)
        );

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"description\":\"Need a drill\"");
        assertThat(json).contains("\"requestorId\":2");
        assertThat(json).contains("\"created\":\"2023-01-01T12:00:00\"");
    }

    @Test
    void shouldDeserializeFromJsonWithCorrectDateFormat() throws Exception {
        String json = "{\"id\":1,\"description\":\"Need a drill\","
                + "\"requestorId\":2,\"created\":\"2023-01-01T12:00:00\"}";

        ItemRequestDto dto = objectMapper.readValue(json, ItemRequestDto.class);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getDescription()).isEqualTo("Need a drill");
        assertThat(dto.getRequestorId()).isEqualTo(2L);
        assertThat(dto.getCreated()).isEqualTo(LocalDateTime.of(2023, 1, 1, 12, 0));
    }
}

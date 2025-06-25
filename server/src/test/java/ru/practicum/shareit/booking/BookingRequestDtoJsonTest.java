package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingRequestDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeWithCorrectDateFormats() throws Exception {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setStart(LocalDateTime.of(2023, 1, 1, 10, 0));
        dto.setEnd(LocalDateTime.of(2023, 1, 2, 10, 0));
        dto.setItemId(1L);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"start\":\"2023-01-01T10:00:00\"");
        assertThat(json).contains("\"end\":\"2023-01-02T10:00:00\"");
        assertThat(json).contains("\"itemId\":1");
    }

    @Test
    void shouldDeserializeWithCorrectDateFormats() throws Exception {
        String json = "{\"start\":\"2023-01-01T10:00:00\","
                + "\"end\":\"2023-01-02T10:00:00\","
                + "\"itemId\":1}";

        BookingRequestDto dto = objectMapper.readValue(json, BookingRequestDto.class);

        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2023, 1, 1, 10, 0));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2023, 1, 2, 10, 0));
        assertThat(dto.getItemId()).isEqualTo(1L);
    }
}

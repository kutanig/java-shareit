package ru.practicum.shareit.item;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ItemServiceImpl.class, ItemMapper.class})
class ItemServiceImplIntegrationTest {

    @Autowired
    private ItemServiceImpl itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Item createTestItem(User owner) {
        return Item.builder()
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .owner(owner)
                .build();
    }

    @Test
    void addItem_shouldSaveItemToDatabase() {
        User owner = userRepository.save(User.builder().name("Owner").email("owner@email.com").build());
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        ItemDto savedItem = itemService.addItem(itemDto, owner.getId());

        assertNotNull(savedItem.getId());
        assertEquals("Test Item", savedItem.getName());
        assertEquals(owner.getId(), savedItem.getOwnerId());

        Item dbItem = itemRepository.findById(savedItem.getId()).orElseThrow();
        assertEquals("Test Item", dbItem.getName());
        assertEquals(owner.getId(), dbItem.getOwner().getId());
    }

    @Test
    void updateItem_shouldUpdateItemInDatabase() {
        User owner = userRepository.save(User.builder().name("Owner").email("owner@email.com").build());
        Item item = itemRepository.save(createTestItem(owner));

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated");
        updateDto.setDescription("Updated Desc");
        updateDto.setAvailable(false);

        ItemDto updatedItem = itemService.updateItem(item.getId(), updateDto, owner.getId());

        assertEquals("Updated", updatedItem.getName());
        assertEquals("Updated Desc", updatedItem.getDescription());
        assertFalse(updatedItem.getAvailable());

        Item dbItem = itemRepository.findById(item.getId()).orElseThrow();
        assertEquals("Updated", dbItem.getName());
        assertEquals("Updated Desc", dbItem.getDescription());
        assertFalse(dbItem.getAvailable());
    }

    @Test
    void getItemById_shouldReturnItemFromDatabase() {
        User owner = userRepository.save(User.builder().name("Owner").email("owner@email.com").build());
        Item item = itemRepository.save(createTestItem(owner));

        ItemDto foundItem = itemService.getItemById(item.getId(), owner.getId());

        assertEquals(item.getId(), foundItem.getId());
        assertEquals("Test Item", foundItem.getName());
        assertEquals("Test Description", foundItem.getDescription());
    }

    @Test
    void getAllItemsByOwner_shouldReturnAllOwnerItemsFromDatabase() {
        User owner1 = userRepository.save(User.builder().name("Owner1").email("owner1@email.com").build());
        User owner2 = userRepository.save(User.builder().name("Owner2").email("owner2@email.com").build());

        itemRepository.save(Item.builder().name("Item1").description("Desc1").available(true).owner(owner1).build());
        itemRepository.save(Item.builder().name("Item2").description("Desc2").available(true).owner(owner1).build());
        itemRepository.save(Item.builder().name("Item3").description("Desc3").available(true).owner(owner2).build());

        List<ItemDto> owner1Items = itemService.getAllItemsByOwner(owner1.getId());

        assertEquals(2, owner1Items.size());
        assertTrue(owner1Items.stream().allMatch(item -> item.getOwnerId().equals(owner1.getId())));
    }

    @Test
    void searchItems_shouldReturnMatchingAvailableItemsFromDatabase() {
        User owner = userRepository.save(User.builder().name("Owner").email("owner@email.com").build());
        itemRepository.save(Item.builder().name("Drill").description("Powerful drill").available(true).owner(owner).build());
        itemRepository.save(Item.builder().name("Hammer").description("Heavy hammer").available(true).owner(owner).build());
        itemRepository.save(Item.builder().name("Broken Drill").description("Doesn't work").available(false).owner(owner).build());

        List<ItemDto> drillResults = itemService.searchItems("drill");
        List<ItemDto> hammerResults = itemService.searchItems("hammer");
        List<ItemDto> emptyResults = itemService.searchItems("");

        assertEquals(1, drillResults.size());
        assertEquals("Drill", drillResults.get(0).getName());

        assertEquals(1, hammerResults.size());
        assertEquals("Hammer", hammerResults.get(0).getName());

        assertTrue(emptyResults.isEmpty());
    }

    @Test
    void updateItem_withWrongOwner_shouldThrowException() {
        User owner = userRepository.save(User.builder().name("Owner").email("owner@email.com").build());
        User otherUser = userRepository.save(User.builder().name("Other").email("other@email.com").build());
        Item item = itemRepository.save(createTestItem(owner));

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated");

        assertThrows(NotFoundException.class, () ->
                itemService.updateItem(item.getId(), updateDto, otherUser.getId()));
    }

    @Test
    void getItemById_withNonExistingId_shouldThrowException() {
        assertThrows(NotFoundException.class, () ->
                itemService.getItemById(9999L, 1L));
    }
}

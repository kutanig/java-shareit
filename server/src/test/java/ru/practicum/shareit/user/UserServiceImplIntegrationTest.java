package ru.practicum.shareit.user;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.practicum.shareit.exception.DuplicateEmailException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserServiceImpl.class, UserMapper.class})
class UserServiceImplIntegrationTest {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private UserDto createTestUserDto() {
        return UserDto.builder()
                .name("Test User")
                .email("test@email.com")
                .build();
    }

    @Test
    void createUser_shouldSaveUserToDatabase() {
        UserDto userDto = createTestUserDto();

        UserDto savedUser = userService.createUser(userDto);

        assertNotNull(savedUser.getId());
        assertEquals("Test User", savedUser.getName());
        assertEquals("test@email.com", savedUser.getEmail());

        User dbUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertEquals("Test User", dbUser.getName());
        assertEquals("test@email.com", dbUser.getEmail());
    }

    @Test
    void createUser_withDuplicateEmail_shouldThrowException() {
        UserDto userDto1 = createTestUserDto();
        userService.createUser(userDto1);

        UserDto userDto2 = UserDto.builder()
                .name("Another User")
                .email("test@email.com")  // Дубликат email
                .build();

        assertThrows(DuplicateEmailException.class, () ->
                userService.createUser(userDto2));
    }

    @Test
    void updateUser_shouldUpdateUserInDatabase() {
        UserDto originalUser = userService.createUser(createTestUserDto());

        UserDto updateDto = UserDto.builder()
                .name("Updated Name")
                .email("updated@email.com")
                .build();

        UserDto updatedUser = userService.updateUser(originalUser.getId(), updateDto);

        assertEquals(originalUser.getId(), updatedUser.getId());
        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("updated@email.com", updatedUser.getEmail());

        User dbUser = userRepository.findById(originalUser.getId()).orElseThrow();
        assertEquals("Updated Name", dbUser.getName());
        assertEquals("updated@email.com", dbUser.getEmail());
    }

    @Test
    void updateUser_partialUpdate_shouldUpdateOnlySpecifiedFields() {
        UserDto originalUser = userService.createUser(createTestUserDto());

        UserDto nameUpdateDto = UserDto.builder()
                .name("New Name Only")
                .build();

        UserDto nameUpdatedUser = userService.updateUser(originalUser.getId(), nameUpdateDto);

        assertEquals(originalUser.getId(), nameUpdatedUser.getId());
        assertEquals("New Name Only", nameUpdatedUser.getName());
        assertEquals(originalUser.getEmail(), nameUpdatedUser.getEmail());

        UserDto emailUpdateDto = UserDto.builder()
                .email("newemail@test.com")
                .build();

        UserDto emailUpdatedUser = userService.updateUser(originalUser.getId(), emailUpdateDto);

        assertEquals(originalUser.getId(), emailUpdatedUser.getId());
        assertEquals("New Name Only", emailUpdatedUser.getName());  // Имя осталось прежним
        assertEquals("newemail@test.com", emailUpdatedUser.getEmail());
    }

    @Test
    void updateUser_withDuplicateEmail_shouldThrowException() {
        userService.createUser(createTestUserDto());

        UserDto secondUser = userService.createUser(UserDto.builder()
                .name("Second User")
                .email("second@email.com")
                .build());

        UserDto updateDto = UserDto.builder()
                .email("test@email.com")
                .build();

        assertThrows(DuplicateEmailException.class, () ->
                userService.updateUser(secondUser.getId(), updateDto));
    }

    @Test
    void updateUser_nonExistingUser_shouldThrowException() {
        UserDto updateDto = createTestUserDto();

        assertThrows(NotFoundException.class, () ->
                userService.updateUser(9999L, updateDto));
    }

    @Test
    void getUserById_shouldReturnUserFromDatabase() {
        UserDto createdUser = userService.createUser(createTestUserDto());

        UserDto foundUser = userService.getUserById(createdUser.getId());

        assertEquals(createdUser.getId(), foundUser.getId());
        assertEquals(createdUser.getName(), foundUser.getName());
        assertEquals(createdUser.getEmail(), foundUser.getEmail());
    }

    @Test
    void getUserById_nonExistingUser_shouldThrowException() {
        assertThrows(NotFoundException.class, () ->
                userService.getUserById(9999L));
    }

    @Test
    void getAllUsers_shouldReturnAllUsersFromDatabase() {
        UserDto user1 = userService.createUser(createTestUserDto());
        UserDto user2 = userService.createUser(UserDto.builder()
                .name("Second User")
                .email("second@email.com")
                .build());

        List<UserDto> users = userService.getAllUsers();

        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getId().equals(user1.getId())));
        assertTrue(users.stream().anyMatch(u -> u.getId().equals(user2.getId())));
    }

    @Test
    void getAllUsers_emptyDatabase_shouldReturnEmptyList() {
        List<UserDto> users = userService.getAllUsers();

        assertTrue(users.isEmpty());
    }

    @Test
    void deleteUser_shouldRemoveUserFromDatabase() {
        UserDto user = userService.createUser(createTestUserDto());

        userService.deleteUser(user.getId());

        assertFalse(userRepository.existsById(user.getId()));
    }

    @Test
    void deleteUser_nonExistingUser_shouldThrowException() {
        assertThrows(NotFoundException.class, () ->
                userService.deleteUser(9999L));
    }

    @Test
    void getUserEntityById_shouldReturnUserEntity() {
        UserDto userDto = userService.createUser(createTestUserDto());

        User userEntity = userService.getUserEntityById(userDto.getId());

        assertEquals(userDto.getId(), userEntity.getId());
        assertEquals(userDto.getName(), userEntity.getName());
        assertEquals(userDto.getEmail(), userEntity.getEmail());
    }
}

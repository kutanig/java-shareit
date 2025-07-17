package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.shareit.exception.DuplicateEmailException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@email.com");

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("Test User");
        userDto.setEmail("test@email.com");
    }

    @Test
    void createUser_ShouldSaveUser() {
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.createUser(userDto);

        assertNotNull(result);
        assertEquals(userDto.getName(), result.getName());
        assertEquals(userDto.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WithDuplicateEmail_ShouldThrowDuplicateEmailException() {
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        assertThrows(DuplicateEmailException.class,
                () -> userService.createUser(userDto));
    }

    @Test
    void updateUser_ShouldUpdateFields() {
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("updated@email.com");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        UserDto updateDto = new UserDto();
        updateDto.setName("Updated Name");
        updateDto.setEmail("updated@email.com");

        UserDto result = userService.updateUser(1L, updateDto);

        assertEquals("Updated Name", result.getName());
        assertEquals("updated@email.com", result.getEmail());
    }

    @Test
    void updateUser_WithDuplicateEmail_ShouldThrowDuplicateEmailException() {
        User existingUserWithEmail = new User();
        existingUserWithEmail.setId(2L);
        existingUserWithEmail.setEmail("duplicate@email.com");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existingUserWithEmail));

        UserDto updateDto = new UserDto();
        updateDto.setEmail("duplicate@email.com");

        assertThrows(DuplicateEmailException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    @Test
    void updateUser_WithNonExistingId_ShouldThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.updateUser(999L, userDto));
    }

    @Test
    void getUserById_ShouldReturnUser() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(userDto.getName(), result.getName());
    }

    @Test
    void getUserById_WithNonExistingId_ShouldThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.getUserById(999L));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals(userDto.getName(), result.get(0).getName());
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDto> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteUser_ShouldDeleteUser() {
        when(userRepository.existsById(anyLong())).thenReturn(true);
        doNothing().when(userRepository).deleteById(anyLong());

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_WithNonExistingId_ShouldThrowNotFoundException() {
        when(userRepository.existsById(anyLong())).thenReturn(false);

        assertThrows(NotFoundException.class,
                () -> userService.deleteUser(999L));
    }

    @Test
    void getUserEntityById_ShouldReturnUser() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        User result = userService.getUserEntityById(1L);

        assertNotNull(result);
        assertEquals(user.getName(), result.getName());
    }

    @Test
    void getUserEntityById_WithNonExistingId_ShouldThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.getUserEntityById(999L));
    }
}

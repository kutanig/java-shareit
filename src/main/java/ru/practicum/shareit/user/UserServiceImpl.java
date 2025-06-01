package ru.practicum.shareit.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.DuplicateEmailException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public UserDto createUser(UserDto userDto) {
        log.info("Creating new user with email: {}", userDto.getEmail());

        if (userDto.getName() == null || userDto.getName().isBlank()) {
            throw new ValidationException("Name cannot be blank");
        }

        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ValidationException("Email cannot be blank");
        }

        // Проверка уникальности email
        if (isEmailExists(userDto.getEmail())) {
            log.warn("Duplicate email detected: {}", userDto.getEmail());
            throw new DuplicateEmailException("Email already exists: " + userDto.getEmail());
        }

        User user = UserMapper.toUser(userDto);
        user.setId(idCounter.getAndIncrement());
        users.put(user.getId(), user);

        log.debug("Created user: ID={}, Name={}, Email={}",
                user.getId(), user.getName(), user.getEmail());
        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        log.info("Updating user ID: {}", userId);

        User existingUser = users.get(userId);
        if (existingUser == null) {
            log.warn("User not found for update: ID={}", userId);
            throw new NotFoundException("User not found with id: " + userId);
        }

        // Проверка уникальности нового email
        if (userDto.getEmail() != null &&
                !userDto.getEmail().equals(existingUser.getEmail()) &&
                isEmailExists(userDto.getEmail())) {
            log.warn("Duplicate email during update: {}", userDto.getEmail());
            throw new DuplicateEmailException("Email already exists: " + userDto.getEmail());
        }

        String originalName = existingUser.getName();
        String originalEmail = existingUser.getEmail();

        if (userDto.getName() != null) existingUser.setName(userDto.getName());
        if (userDto.getEmail() != null) existingUser.setEmail(userDto.getEmail());

        log.debug("Updated user: ID={}, Name: {} -> {}, Email: {} -> {}",
                userId, originalName, existingUser.getName(),
                originalEmail, existingUser.getEmail());

        return UserMapper.toUserDto(existingUser);
    }

    @Override
    public UserDto getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);

        User user = users.get(userId);
        if (user == null) {
            log.warn("User not found: ID={}", userId);
            throw new NotFoundException("User not found with id: " + userId);
        }

        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        log.debug("Fetching all users, count: {}", users.size());
        return users.values().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public void deleteUser(Long userId) {
        log.info("Deleting user ID: {}", userId);

        if (!users.containsKey(userId)) {
            log.warn("User not found for deletion: ID={}", userId);
            throw new NotFoundException("User not found with id: " + userId);
        }

        User removedUser = users.remove(userId);
        log.debug("Deleted user: ID={}, Email={}", userId, removedUser.getEmail());
    }

    @Override
    public User getUserEntityById(Long userId) {
        log.debug("Fetching user entity by ID: {}", userId);

        User user = users.get(userId);
        if (user == null) {
            log.warn("User entity not found: ID={}", userId);
            throw new NotFoundException("User not found with id: " + userId);
        }

        return user;
    }

    private boolean isEmailExists(String email) {
        return users.values().stream()
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .anyMatch(e -> e.equals(email));
    }
}

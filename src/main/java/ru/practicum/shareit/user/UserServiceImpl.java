package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.DuplicateEmailException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Creating new user with email: {}", userDto.getEmail());

        User user = UserMapper.toUser(userDto);

        try {
            User savedUser = userRepository.save(user);
            log.debug("Created user: ID={}, Name={}, Email={}",
                    savedUser.getId(), savedUser.getName(), savedUser.getEmail());
            return UserMapper.toUserDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate email detected: {}", userDto.getEmail());
            throw new DuplicateEmailException("Email already exists: " + userDto.getEmail());
        }
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserDto userDto) {
        log.info("Updating user ID: {}", userId);

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for update: ID={}", userId);
                    return new NotFoundException("User not found with id: " + userId);
                });

        if (userDto.getEmail() != null &&
                !userDto.getEmail().equals(existingUser.getEmail())) {
            Optional<User> userWithSameEmail = userRepository.findByEmail(userDto.getEmail());
            if (userWithSameEmail.isPresent()) {
                log.warn("Duplicate email during update: {}", userDto.getEmail());
                throw new DuplicateEmailException("Email already exists: " + userDto.getEmail());
            }
        }

        String originalName = existingUser.getName();
        String originalEmail = existingUser.getEmail();

        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }
        if (userDto.getEmail() != null) {
            existingUser.setEmail(userDto.getEmail());
        }

        User updatedUser = userRepository.save(existingUser);

        log.debug("Updated user: ID={}, Name: {} -> {}, Email: {} -> {}",
                userId, originalName, updatedUser.getName(),
                originalEmail, updatedUser.getEmail());

        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    public UserDto getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: ID={}", userId);
                    return new NotFoundException("User not found with id: " + userId);
                });

        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        log.debug("Fetching all users");
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("User not found for deletion: ID={}", userId);
            throw new NotFoundException("User not found with id: " + userId);
        }

        userRepository.deleteById(userId);
        log.debug("Deleted user: ID={}", userId);
    }

    @Override
    public User getUserEntityById(Long userId) {
        log.debug("Fetching user entity by ID: {}", userId);

        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User entity not found: ID={}", userId);
                    return new NotFoundException("User not found with id: " + userId);
                });
    }
}

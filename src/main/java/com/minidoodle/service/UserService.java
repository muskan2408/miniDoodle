package com.minidoodle.service;

import com.minidoodle.domain.Calendar;
import com.minidoodle.domain.User;
import com.minidoodle.dto.UserDTO;
import com.minidoodle.exception.BusinessException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.CalendarRepository;
import com.minidoodle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        log.info("Creating user with email: {}", userDTO.getEmail());

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new BusinessException("User with email " + userDTO.getEmail() + " already exists");
        }

        User user = User.builder()
            .name(userDTO.getName())
            .email(userDTO.getEmail())
            .build();

        User savedUser = userRepository.save(user);

        // Create calendar for user
        Calendar calendar = Calendar.builder()
            .user(savedUser)
            .timezone("UTC")
            .build();
        calendarRepository.save(calendar);

        log.info("Created user with ID: {}", savedUser.getId());
        return mapToDTO(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToDTO(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equals(userDTO.getEmail()) &&
            userRepository.existsByEmail(userDTO.getEmail())) {
            throw new BusinessException("User with email " + userDTO.getEmail() + " already exists");
        }

        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());

        User updatedUser = userRepository.save(user);
        log.info("Updated user with ID: {}", id);
        return mapToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user with ID: {}", id);
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}

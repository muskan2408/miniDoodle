package com.minidoodle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidoodle.dto.UserDTO;
import com.minidoodle.repository.CalendarRepository;
import com.minidoodle.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @BeforeEach
    void setUp() {
        calendarRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createUser_Success() throws Exception {
        UserDTO userDTO = UserDTO.builder()
            .name("John Doe")
            .email("john.doe@example.com")
            .build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("John Doe"))
            .andExpect(jsonPath("$.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createUser_DuplicateEmail_ReturnsBadRequest() throws Exception {
        UserDTO userDTO = UserDTO.builder()
            .name("John Doe")
            .email("duplicate@example.com")
            .build();

        // Create first user
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isCreated());

        // Try to create duplicate
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void createUser_InvalidEmail_ReturnsBadRequest() throws Exception {
        UserDTO userDTO = UserDTO.builder()
            .name("John Doe")
            .email("invalid-email")
            .build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void createUser_MissingName_ReturnsBadRequest() throws Exception {
        UserDTO userDTO = UserDTO.builder()
            .email("john@example.com")
            .build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_Success() throws Exception {
        // Create user first
        UserDTO userDTO = UserDTO.builder()
            .name("Jane Doe")
            .email("jane@example.com")
            .build();

        String response = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        UserDTO created = objectMapper.readValue(response, UserDTO.class);

        // Get user by ID
        mockMvc.perform(get("/api/v1/users/" + created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(created.getId()))
            .andExpect(jsonPath("$.name").value("Jane Doe"))
            .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void getUserById_NotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    void getAllUsers_Success() throws Exception {
        // Create two users
        UserDTO user1 = UserDTO.builder()
            .name("User One")
            .email("user1@example.com")
            .build();

        UserDTO user2 = UserDTO.builder()
            .name("User Two")
            .email("user2@example.com")
            .build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2)))
            .andExpect(status().isCreated());

        // Get all users
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].email", containsInAnyOrder("user1@example.com", "user2@example.com")));
    }

    @Test
    void deleteUser_Success() throws Exception {
        // Create user
        UserDTO userDTO = UserDTO.builder()
            .name("Delete Me")
            .email("delete@example.com")
            .build();

        String response = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDTO)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        UserDTO created = objectMapper.readValue(response, UserDTO.class);

        // Delete user
        mockMvc.perform(delete("/api/v1/users/" + created.getId()))
            .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/v1/users/" + created.getId()))
            .andExpect(status().isNotFound());
    }
}

package com.simdev.api_users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simdev.api_users.user.controller.UserController;
import com.simdev.api_users.user.dto.UserRequest;
import com.simdev.api_users.user.dto.UserResponse;
import com.simdev.api_users.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(controllers = UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser
    @DisplayName("Should create user successfully")
    void testCreateUser_Success() throws Exception {
        // Given
        UserRequest request = UserRequest.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("password123")
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .active(true)
                .build();

        when(userService.createUser(any(UserRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should get all users")
    void testGetAllUsers() throws Exception {
        // Given
        List<UserResponse> users = Arrays.asList(
                UserResponse.builder().id(1L).email("user1@example.com").firstName("User1").lastName("Test").active(true).build(),
                UserResponse.builder().id(2L).email("user2@example.com").firstName("User2").lastName("Test").active(true).build()
        );

        when(userService.getAllUsers()).thenReturn(users);

        // When/Then
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @WithMockUser
    @DisplayName("Should get user by ID")
    void testGetUserById() throws Exception {
        // Given
        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .active(true)
                .build();

        when(userService.getUserById(1L)).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should update user successfully")
    void testUpdateUser_Success() throws Exception {
        // Given
        UserRequest request = UserRequest.builder()
                .email("test@example.com")
                .firstName("Updated")
                .lastName("Name")
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Updated")
                .lastName("Name")
                .active(true)
                .build();

        when(userService.updateUser(eq(1L), any(UserRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(put("/users/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should delete user successfully")
    void testDeleteUser_Success() throws Exception {
        // Given
        doNothing().when(userService).deleteUser(1L);

        // When/Then
        mockMvc.perform(delete("/users/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }
}


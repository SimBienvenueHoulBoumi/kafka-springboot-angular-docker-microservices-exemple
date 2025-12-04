package com.simdev.api_users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simdev.api_users.auth.dto.LoginRequest;
import com.simdev.api_users.auth.dto.LoginResponse;
import com.simdev.api_users.auth.dto.RegisterRequest;
import com.simdev.api_users.user.dto.UserResponse;
import com.simdev.api_users.auth.controller.AuthController;
import com.simdev.api_users.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(controllers = AuthController.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("Should register user successfully")
    void testRegister_Success() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
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

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"));
    }

    @Test
    @DisplayName("Should return 400 for invalid register request")
    void testRegister_InvalidRequest() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("") // Invalid email
                .build();

        // When/Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLogin_Success() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("jwt-token-123")
                .type("Bearer")
                .userId(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Should return 400 for invalid login request")
    void testLogin_InvalidRequest() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("") // Invalid
                .build();

        // When/Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}


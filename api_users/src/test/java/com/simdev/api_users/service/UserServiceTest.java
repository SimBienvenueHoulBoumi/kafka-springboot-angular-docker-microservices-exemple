package com.simdev.api_users.service;

import com.simdev.api_users.user.domain.User;
import com.simdev.api_users.user.dto.UserRequest;
import com.simdev.api_users.user.dto.UserResponse;
import com.simdev.api_users.user.exception.ResourceNotFoundException;
import com.simdev.api_users.user.exception.UserAlreadyExistsException;
import com.simdev.api_users.user.repository.UserRepository;
import com.simdev.api_users.user.service.UserService;
import com.simdev.api_users.shared.service.OutboxService;
import com.simdev.api_users.shared.domain.OutboxEvent;
import com.simdev.api_users.shared.metrics.CustomMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomMetrics customMetrics;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        // Mock CustomMetrics methods with lenient to avoid UnnecessaryStubbingException
        lenient().doNothing().when(customMetrics).incrementUserCreated();
        lenient().doNothing().when(customMetrics).incrementUserUpdated();
        lenient().doNothing().when(customMetrics).incrementUserDeleted();
        
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("encodedPassword")
                .active(true)
                .build();

        userRequest = UserRequest.builder()
                .email("new@example.com")
                .firstName("New")
                .lastName("User")
                .password("password123")
                .build();
    }

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser_Success() throws Exception {
        // Given
        when(userRepository.existsByEmail(userRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(userRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{}");
        when(outboxService.createEvent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(OutboxEvent.class));

        // When
        UserResponse response = userService.createUser(userRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(outboxService, times(1)).createEvent(eq("user.created"), eq("user-events"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email exists")
    void testCreateUser_EmailExists() {
        // Given
        when(userRepository.existsByEmail(userRequest.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(userRequest))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(outboxService, never()).createEvent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should get all users")
    void testGetAllUsers() {
        // Given
        User user2 = User.builder().id(2L).email("user2@example.com").firstName("User2").lastName("Test").password("pass").active(true).build();
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

        // When
        List<UserResponse> users = userService.getAllUsers();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getEmail()).isEqualTo(testUser.getEmail());
        assertThat(users.get(1).getEmail()).isEqualTo(user2.getEmail());
    }

    @Test
    @DisplayName("Should get user by ID")
    void testGetUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void testGetUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilisateur");
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUser_Success() throws Exception {
        // Given
        UserRequest updateRequest = UserRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .email("test@example.com")
                .password(null)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{}");
        when(outboxService.createEvent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(OutboxEvent.class));

        // When
        UserResponse response = userService.updateUser(1L, updateRequest);

        // Then
        assertThat(response).isNotNull();
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        verify(outboxService, times(1)).createEvent(eq("user.updated"), eq("user-events"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should update password when provided")
    void testUpdateUser_WithPassword() {
        // Given
        UserRequest updateRequest = UserRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("newPassword")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUser(1L, updateRequest);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(passwordEncoder, times(1)).encode("newPassword");
    }

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUser_Success() throws Exception {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("{}");
        when(outboxService.createEvent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(OutboxEvent.class));

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository, times(1)).delete(testUser);
        verify(outboxService, times(1)).createEvent(eq("user.deleted"), eq("user-events"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent user")
    void testDeleteUser_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).delete(any(User.class));
        verify(outboxService, never()).createEvent(anyString(), anyString(), anyString(), anyString());
    }
}


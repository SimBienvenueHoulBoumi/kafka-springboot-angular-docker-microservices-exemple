package com.simdev.api_users.service;

import com.simdev.api_users.auth.service.AuthService;
import com.simdev.api_users.auth.service.JwtService;
import com.simdev.api_users.auth.service.UserDetailsServiceImpl;
import com.simdev.api_users.user.domain.User;
import com.simdev.api_users.auth.dto.LoginRequest;
import com.simdev.api_users.auth.dto.LoginResponse;
import com.simdev.api_users.auth.dto.RegisterRequest;
import com.simdev.api_users.user.dto.UserResponse;
import com.simdev.api_users.auth.exception.BadCredentialsException;
import com.simdev.api_users.user.exception.UserAlreadyExistsException;
import com.simdev.api_users.auth.exception.UserInactiveException;
import com.simdev.api_users.user.repository.UserRepository;
import com.simdev.api_users.shared.metrics.CustomMetrics;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private CustomMetrics customMetrics;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .password("encodedPassword")
                .active(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .email("newuser@example.com")
                .firstName("New")
                .lastName("User")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        // Mock CustomMetrics methods with lenient to avoid UnnecessaryStubbingException
        lenient().when(customMetrics.startRegistrationTimer()).thenReturn(mock(Timer.Sample.class));
        lenient().when(customMetrics.startLoginTimer()).thenReturn(mock(Timer.Sample.class));
        lenient().doNothing().when(customMetrics).recordRegistrationDuration(any(Timer.Sample.class));
        lenient().doNothing().when(customMetrics).recordLoginDuration(any(Timer.Sample.class));
        lenient().doNothing().when(customMetrics).incrementUserRegistration();
        lenient().doNothing().when(customMetrics).incrementUserLogin();
        lenient().doNothing().when(customMetrics).incrementAuthenticationFailure();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterUser_Success() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getFirstName()).isEqualTo(testUser.getFirstName());
        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(registerRequest.getPassword());
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email already exists")
    void testRegisterUser_EmailExists() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(registerRequest.getEmail());
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login user successfully with valid credentials")
    void testLogin_Success() {
        // Given
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(testUser.getEmail())
                .password(testUser.getPassword())
                .authorities("ROLE_USER")
                .build();

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(testUser.getEmail())).thenReturn(userDetails);
        when(jwtService.generateToken(any(UserDetails.class), eq(testUser.getId())))
                .thenReturn("jwt-token-123");

        // When
        LoginResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtService, times(1)).generateToken(any(UserDetails.class), eq(testUser.getId()));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user not found")
    void testLogin_UserNotFound() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
        
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password is incorrect")
    void testLogin_WrongPassword() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
        
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("Should throw UserInactiveException when user is inactive")
    void testLogin_UserInactive() {
        // Given
        testUser.setActive(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UserInactiveException.class);
        
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(), any());
    }
}


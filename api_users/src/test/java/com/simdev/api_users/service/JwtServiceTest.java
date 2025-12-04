package com.simdev.api_users.service;

import com.simdev.api_users.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private static final String SECRET = "testSecretKey123456789012345678901234567890"; // 32+ chars
    private static final long EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken_Success() {
        // When
        String token = jwtService.generateToken(userDetails, 1L);

        // Then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract email from token")
    void testExtractEmail_Success() {
        // Given
        String token = jwtService.generateToken(userDetails, 1L);

        // When
        String email = jwtService.extractEmail(token);

        // Then
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void testExtractUserId_Success() {
        // Given
        String token = jwtService.generateToken(userDetails, 123L);

        // When
        Long userId = jwtService.extractUserId(token);

        // Then
        assertThat(userId).isEqualTo(123L);
    }

    @Test
    @DisplayName("Should validate token successfully")
    void testValidateToken_Success() {
        // Given
        String token = jwtService.generateToken(userDetails, 1L);

        // When
        Boolean isValid = jwtService.validateToken(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should validate token without UserDetails")
    void testValidateToken_WithoutUserDetails() {
        // Given
        String token = jwtService.generateToken(userDetails, 1L);

        // When
        Boolean isValid = jwtService.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void testValidateToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        Boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void testExtractExpiration_Success() {
        // Given
        String token = jwtService.generateToken(userDetails, 1L);

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }
}


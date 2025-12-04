package com.simdev.api_users.security;

import com.simdev.api_users.shared.security.JwtAuthenticationFilter;
import com.simdev.api_users.auth.service.JwtService;
import com.simdev.api_users.auth.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserDetails userDetails;
    private String validToken;

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        validToken = "Bearer valid.jwt.token";
    }

    @Test
    @DisplayName("Should process request with valid JWT token")
    void testDoFilterInternal_ValidToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(validToken);
        when(jwtService.extractEmail("valid.jwt.token")).thenReturn("test@example.com");
        when(jwtService.validateToken("valid.jwt.token")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtService.validateToken("valid.jwt.token", userDetails)).thenReturn(true);

        // When
        // Use Spring's test utilities to test the filter
        // Since doFilterInternal is protected, we test the public doFilter method instead
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should continue filter chain when no Authorization header")
    void testDoFilterInternal_NoAuthHeader() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        // Use reflection to access the protected method
        java.lang.reflect.Method method = JwtAuthenticationFilter.class.getDeclaredMethod("doFilterInternal", HttpServletRequest.class, HttpServletResponse.class, FilterChain.class);
        method.setAccessible(true);
        method.invoke(jwtAuthenticationFilter, request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).extractEmail(anyString());
    }

    @Test
    @DisplayName("Should continue filter chain when invalid token format")
    void testDoFilterInternal_InvalidTokenFormat() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat");

        // When
        // Use reflection to access the protected method
        java.lang.reflect.Method method = JwtAuthenticationFilter.class.getDeclaredMethod("doFilterInternal", HttpServletRequest.class, HttpServletResponse.class, FilterChain.class);
        method.setAccessible(true);
        method.invoke(jwtAuthenticationFilter, request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
    }
}


package com.simdev.api_tasks.service;

import com.simdev.api_tasks.shared.exception.UserNotFoundException;
import com.simdev.api_tasks.shared.service.UserCacheService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserCacheService Tests")
class UserCacheServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserCacheService userCacheService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userCacheService, "usersServiceUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(userCacheService, "cacheTtlSeconds", 300L);
    }

    @Test
    @DisplayName("Should throw exception when user does not exist")
    void testUserExists_UserNotFound() {
        // Given
        when(restTemplate.getForEntity(anyString(), eq(Object.class)))
                .thenThrow(new RestClientException("User not found"));

        // When/Then
        assertThatThrownBy(() -> userCacheService.userExists(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should not throw exception when user exists")
    void testUserExists_UserFound() {
        // Given
        when(restTemplate.getForEntity(anyString(), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(new Object(), HttpStatus.OK));

        // When/Then - Should not throw
        userCacheService.userExists(1L);
        
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(Object.class));
    }
}


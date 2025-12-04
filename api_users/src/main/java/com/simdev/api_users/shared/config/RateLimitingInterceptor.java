package com.simdev.api_users.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    private final CacheManager cacheManager;
    private static final int MAX_REQUESTS = 10;
    private static final long TIME_WINDOW_SECONDS = 60;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIpAddress(request);
        String cacheKey = "rate_limit_" + clientIp;
        
        var cache = cacheManager.getCache("rateLimits");
        if (cache == null) {
            return true;
        }
        
        RateLimitInfo rateLimitInfo = cache.get(cacheKey, RateLimitInfo.class);
        
        if (rateLimitInfo == null) {
            rateLimitInfo = new RateLimitInfo(1, LocalDateTime.now());
            cache.put(cacheKey, rateLimitInfo);
            return true;
        }
        
        long secondsSinceFirstRequest = java.time.Duration.between(rateLimitInfo.firstRequestTime, LocalDateTime.now()).getSeconds();
        
        if (secondsSinceFirstRequest >= TIME_WINDOW_SECONDS) {
            rateLimitInfo = new RateLimitInfo(1, LocalDateTime.now());
            cache.put(cacheKey, rateLimitInfo);
            return true;
        }
        
        rateLimitInfo.requestCount++;
        
        if (rateLimitInfo.requestCount > MAX_REQUESTS) {
            log.warn("Rate limit exceeded for IP: {} - {} requests in {} seconds", 
                clientIp, rateLimitInfo.requestCount, secondsSinceFirstRequest);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", String.valueOf(TIME_WINDOW_SECONDS - secondsSinceFirstRequest));
            return false;
        }
        
        cache.put(cacheKey, rateLimitInfo);
        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(MAX_REQUESTS - rateLimitInfo.requestCount));
        
        return true;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private static class RateLimitInfo {
        int requestCount;
        LocalDateTime firstRequestTime;
        
        RateLimitInfo(int requestCount, LocalDateTime firstRequestTime) {
            this.requestCount = requestCount;
            this.firstRequestTime = firstRequestTime;
        }
    }
}


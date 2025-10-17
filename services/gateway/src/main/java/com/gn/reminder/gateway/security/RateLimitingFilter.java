package com.gn.reminder.gateway.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter using Resilience4j
 * Limits requests per IP address or User ID (if authenticated)
 * 
 * Default: 100 requests per 60 seconds per resource (IP/User)
 * AWS-Ready: Configurable via environment variables
 */
@Component
@RefreshScope
@Slf4j
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    @Value("${resilience4j.ratelimiter.instances.gatewayRateLimiter.limitForPeriod:100}")
    private int limitForPeriod;
    
    @Value("${resilience4j.ratelimiter.instances.gatewayRateLimiter.limitRefreshPeriod:60s}")
    private String limitRefreshPeriod;
    
    @Value("${resilience4j.ratelimiter.instances.gatewayRateLimiter.timeoutDuration:5s}")
    private String timeoutDuration;

    public RateLimitingFilter() {
        // Initialize rate limiter registry
        this.rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // Skip rate limiting for actuator endpoints
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        // Determine rate limit key (prefer User ID over IP)
        String rateLimitKey = getRateLimitKey(exchange);
        
        // Get or create rate limiter for this resource
        RateLimiter rateLimiter = getRateLimiter(rateLimitKey);
        
        log.debug("Rate limiting request from: {} to path: {}", rateLimitKey, path);

        // Apply rate limiting
        return Mono.just(exchange)
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .flatMap(chain::filter)
                .onErrorResume(throwable -> {
                    log.warn("Rate limit exceeded for: {} on path: {}", rateLimitKey, path);
                    return handleRateLimitExceeded(exchange);
                });
    }

    /**
     * Determine the rate limit key based on authenticated user or IP address
     */
    private String getRateLimitKey(ServerWebExchange exchange) {
        // Check if user is authenticated (header added by JwtAuthenticationFilter)
        String userId = exchange.getRequest().getHeaders().getFirst("X-Auth-User-Id");
        
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }
        
        // Fall back to IP address
        String ipAddress = getClientIp(exchange.getRequest());
        return "ip:" + ipAddress;
    }

    /**
     * Get client IP address (handles proxies and load balancers)
     */
    private String getClientIp(ServerHttpRequest request) {
        // Check X-Forwarded-For header (for AWS ALB, CloudFront)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For may contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fall back to remote address
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "unknown";
    }

    /**
     * Parse duration string to Duration object
     * Handles formats like "60s", "5m", "1h" and converts to ISO-8601 format
     */
    private Duration parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return Duration.ofSeconds(60); // Default fallback
        }

        // Handle common formats: "60s", "5m", "1h"
        String normalized = durationStr.toLowerCase().trim();

        if (normalized.endsWith("s")) {
            long seconds = Long.parseLong(normalized.substring(0, normalized.length() - 1));
            return Duration.ofSeconds(seconds);
        } else if (normalized.endsWith("m")) {
            long minutes = Long.parseLong(normalized.substring(0, normalized.length() - 1));
            return Duration.ofMinutes(minutes);
        } else if (normalized.endsWith("h")) {
            long hours = Long.parseLong(normalized.substring(0, normalized.length() - 1));
            return Duration.ofHours(hours);
        } else {
            // Try to parse as ISO-8601 format
            try {
                return Duration.parse(durationStr);
            } catch (Exception e) {
                log.warn("Failed to parse duration: {}, using default 60s", durationStr);
                return Duration.ofSeconds(60);
            }
        }
    }

    /**
     * Get or create rate limiter for a specific resource
     */
    private RateLimiter getRateLimiter(String key) {
        return rateLimiters.computeIfAbsent(key, k -> {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(limitForPeriod)
                    .limitRefreshPeriod(parseDuration(limitRefreshPeriod))
                    .timeoutDuration(parseDuration(timeoutDuration))
                    .build();
            
            RateLimiter limiter = rateLimiterRegistry.rateLimiter(k, config);
            
            log.info("Created rate limiter for: {} with limit: {}/{}", 
                    k, limitForPeriod, limitRefreshPeriod);
            
            return limiter;
        });
    }

    /**
     * Handle rate limit exceeded error
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limitForPeriod));
        response.getHeaders().add("X-RateLimit-Reset", limitRefreshPeriod);
        
        String errorBody = String.format(
                "{\"error\": \"Rate limit exceeded\", \"message\": \"Too many requests. Limit: %d requests per %s\", \"status\": 429}",
                limitForPeriod, limitRefreshPeriod
        );
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBody.getBytes())));
    }

    @Override
    public int getOrder() {
        return -50; // Execute after JWT auth filter (-100) but before other filters
    }
}


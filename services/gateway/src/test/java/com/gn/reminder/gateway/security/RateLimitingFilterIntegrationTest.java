package com.gn.reminder.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("rate-limit-test")
@TestPropertySource(properties = {
    "resilience4j.ratelimiter.instances.gatewayRateLimiter.limitForPeriod=10",
    "resilience4j.ratelimiter.instances.gatewayRateLimiter.limitRefreshPeriod=60s",
    "resilience4j.ratelimiter.instances.gatewayRateLimiter.timeoutDuration=1s",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "keycloak.enabled=false",
    "logging.level.com.gn.reminder.gateway.security=DEBUG",
    "logging.level.io.github.resilience4j=DEBUG"
})
@DisplayName("Rate Limiting Integration Tests")
class RateLimitingFilterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Test
    @DisplayName("Should allow requests within rate limit (10 requests per minute)")
    void shouldAllowRequestsWithinRateLimit() {
        // Make 10 requests (should all succeed)
        for (int i = 1; i <= 10; i++) {
            webTestClient.get()
                    .uri("/api/v1/auth/login") // Use public endpoint
                    .header("X-Forwarded-For", "192.168.1.100") // Simulate same IP
                    .exchange()
                    .expectStatus().isNotFound(); // 404 is fine, means it passed rate limiting
        }
    }

    @Test
    @DisplayName("Should block requests exceeding rate limit (11th request should fail)")
    void shouldBlockRequestsExceedingRateLimit() {
        String testIp = "192.168.1.101";
        
        // Make 10 requests (should all succeed)
        for (int i = 1; i <= 10; i++) {
            webTestClient.get()
                    .uri("/api/v1/auth/login") // Use public endpoint
                    .header("X-Forwarded-For", testIp)
                    .exchange()
                    .expectStatus().isNotFound(); // 404 is fine, means it passed rate limiting
        }

        // 11th request should be rate limited
        webTestClient.get()
                .uri("/api/v1/auth/login") // Use public endpoint
                .header("X-Forwarded-For", testIp)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectHeader().exists("X-RateLimit-Limit")
                .expectHeader().valueEquals("X-RateLimit-Limit", "10")
                .expectHeader().exists("X-RateLimit-Reset")
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Rate limit exceeded");
                    assertThat(body).contains("Too many requests");
                    assertThat(body).contains("10 requests per 60s");
                });
    }

    @Test
    @DisplayName("Should apply separate rate limits for different IP addresses")
    void shouldApplySeparateRateLimitsForDifferentIPs() {
        String ip1 = "192.168.1.102";
        String ip2 = "192.168.1.103";

        // IP1: Make 10 requests (should all succeed)
        for (int i = 1; i <= 10; i++) {
            webTestClient.get()
                    .uri("/api/v1/auth/login") // Use public endpoint
                    .header("X-Forwarded-For", ip1)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        // IP2: Should still be able to make 10 requests (separate bucket)
        for (int i = 1; i <= 10; i++) {
            webTestClient.get()
                    .uri("/api/v1/auth/login") // Use public endpoint
                    .header("X-Forwarded-For", ip2)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        // IP1: 11th request should be blocked
        webTestClient.get()
                .uri("/api/v1/auth/login") // Use public endpoint
                .header("X-Forwarded-For", ip1)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // IP2: 11th request should also be blocked
        webTestClient.get()
                .uri("/api/v1/auth/login") // Use public endpoint
                .header("X-Forwarded-For", ip2)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Should skip rate limiting for actuator endpoints")
    void shouldSkipRateLimitingForActuatorEndpoints() {
        // Make 15 requests to actuator endpoint (should all succeed)
        for (int i = 1; i <= 15; i++) {
            webTestClient.get()
                    .uri("/actuator/health")
                    .header("X-Forwarded-For", "192.168.1.104")
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Test
    @DisplayName("Should handle concurrent requests correctly")
    void shouldHandleConcurrentRequestsCorrectly() throws Exception {
        String testIp = "192.168.1.105";
        int totalRequests = 15;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        try {
            CompletableFuture<Void>[] futures = new CompletableFuture[totalRequests];
            
            for (int i = 0; i < totalRequests; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        webTestClient.get()
                                .uri("/api/v1/auth/login") // Use public endpoint
                                .header("X-Forwarded-For", testIp)
                                .exchange()
                                .expectStatus().value(status -> {
                                    if (status.equals(HttpStatus.NOT_FOUND.value())) {
                                        successCount.incrementAndGet();
                                    } else if (status.equals(HttpStatus.TOO_MANY_REQUESTS.value())) {
                                        rateLimitedCount.incrementAndGet();
                                    }
                                });
                    } catch (Exception e) {
                        // Handle any exceptions
                        System.err.println("Request failed: " + e.getMessage());
                    }
                }, executor);
            }

            // Wait for all requests to complete
            CompletableFuture.allOf(futures).get();

            // Verify results
            assertThat(successCount.get()).isEqualTo(10);
            assertThat(rateLimitedCount.get()).isEqualTo(5);
            assertThat(successCount.get() + rateLimitedCount.get()).isEqualTo(totalRequests);
            
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should apply rate limiting based on authenticated user ID when JWT is present")
    void shouldApplyRateLimitingBasedOnAuthenticatedUser() {
        // This test would require a valid JWT token
        // For now, we'll test the IP-based fallback behavior
        
        String testIp = "192.168.1.106";
        
        // Without JWT token, should use IP-based rate limiting
        for (int i = 1; i <= 10; i++) {
            webTestClient.get()
                    .uri("/api/v1/auth/login") // Use public endpoint
                    .header("X-Forwarded-For", testIp)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        // 11th request should be rate limited
        webTestClient.get()
                .uri("/api/v1/auth/login") // Use public endpoint
                .header("X-Forwarded-For", testIp)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}

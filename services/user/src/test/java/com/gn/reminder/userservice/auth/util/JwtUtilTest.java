package com.gn.reminder.userservice.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set test values using reflection
        ReflectionTestUtils.setField(jwtUtil, "secret", 
                "TestSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256AlgorithmTestOnly");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken() {
        // When
        String token = jwtUtil.generateToken("testuser", "test@example.com", "user123");

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void testExtractUsername() {
        // Given
        String token = jwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should extract user ID from valid token")
    void testExtractUserId() {
        // Given
        String token = jwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When
        String userId = jwtUtil.extractUserId(token);

        // Then
        assertThat(userId).isEqualTo("user123");
    }

    @Test
    @DisplayName("Should extract email from valid token")
    void testExtractEmail() {
        // Given
        String token = jwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When
        String email = jwtUtil.extractEmail(token);

        // Then
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should extract all claims from valid token")
    void testExtractAllClaims() {
        // Given
        String token = jwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When
        Claims claims = jwtUtil.extractAllClaims(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("userId")).hasToString("user123");
        assertThat(claims.get("email")).hasToString("test@example.com");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("Should validate a valid token")
    void testValidateToken_Valid() {
        // Given
        String token = jwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid token format")
    void testValidateToken_InvalidFormat() {
        // Given
        String invalidToken = "this.is.not.a.valid.token";

        // When
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testValidateToken_Malformed() {
        // Given
        String malformedToken = "malformed-token";

        // When
        boolean isValid = jwtUtil.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for expired token when extracting claims")
    void testExtractClaims_ExpiredToken() {
        // Given - Create a token with negative expiration
        JwtUtil shortLivedJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortLivedJwtUtil, "secret",
                "TestSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256AlgorithmTestOnly");
        ReflectionTestUtils.setField(shortLivedJwtUtil, "expiration", -1000L); // Already expired

        String expiredToken = shortLivedJwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When & Then - Should throw exception when trying to extract username
        assertThatThrownBy(() -> jwtUtil.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void testValidateToken_Null() {
        // When
        boolean isValid = jwtUtil.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should handle empty token gracefully")
    void testValidateToken_Empty() {
        // When
        boolean isValid = jwtUtil.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should check if token is expired")
    void testIsTokenExpired() {
        // Given
        String token = jwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should detect expired token")
    void testIsTokenExpired_Expired() {
        // Given - Create a token with very short expiration
        JwtUtil shortLivedJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortLivedJwtUtil, "secret",
                "TestSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256AlgorithmTestOnly");
        ReflectionTestUtils.setField(shortLivedJwtUtil, "expiration", -1000L); // Already expired

        String expiredToken = shortLivedJwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When & Then
        assertThatThrownBy(() -> jwtUtil.isTokenExpired(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testGenerateToken_DifferentUsers() {
        // When
        String token1 = jwtUtil.generateToken("user1", "user1@example.com", "id1");
        String token2 = jwtUtil.generateToken("user2", "user2@example.com", "id2");

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.extractUsername(token1)).isEqualTo("user1");
        assertThat(jwtUtil.extractUsername(token2)).isEqualTo("user2");
    }

    @Test
    @DisplayName("Should throw exception for token with invalid signature")
    void testValidateToken_InvalidSignature() {
        // Given - Generate token with different secret
        JwtUtil differentSecretJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(differentSecretJwtUtil, "secret",
                "DifferentSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256AlgorithmDifferent");
        ReflectionTestUtils.setField(differentSecretJwtUtil, "expiration", 86400000L);

        String tokenWithDifferentSignature = differentSecretJwtUtil.generateToken("testuser", "test@example.com", "user123");

        // When - Validate with original jwtUtil (different secret)
        boolean isValid = jwtUtil.validateToken(tokenWithDifferentSignature);

        // Then
        assertThat(isValid).isFalse();
    }
}


package com.gn.reminder.userservice.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.SignupRequest;
import com.gn.reminder.userservice.config.MongoTestContainerConfig;
import com.gn.reminder.userservice.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MongoTestContainerConfig.class)
@DisplayName("User Profile Integration Tests")
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    private String testUserId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up database before each test
        userRepo.deleteAll();

        // Create a test user and get the token
        SignupRequest signupRequest = SignupRequest.builder()
                .username("profileuser")
                .email("profile@example.com")
                .password("ProfilePass123")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = signupResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        testUserId = authResponse.getUserId();
    }

    @Test
    @DisplayName("Should successfully get user profile with valid user ID")
    void testGetUserProfile_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/user")
                        .header("X-Auth-User-Id", testUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId))
                .andExpect(jsonPath("$.username").value("profileuser"))
                .andExpect(jsonPath("$.email").value("profile@example.com"))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()))
                // Password should NOT be present in response
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should return 404 when user ID is invalid")
    void testGetUserProfile_InvalidUserId() throws Exception {
        // Given
        String invalidUserId = "invalid-user-id-123";

        // When & Then
        mockMvc.perform(get("/api/v1/user")
                        .header("X-Auth-User-Id", invalidUserId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when user ID header is missing")
    void testGetUserProfile_MissingUserId() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/user"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should verify password is never exposed in user profile response")
    void testGetUserProfile_PasswordNotExposed() throws Exception {
        // When
        MvcResult result = mockMvc.perform(get("/api/v1/user")
                        .header("X-Auth-User-Id", testUserId))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Then - Verify response body doesn't contain password-related fields
        org.assertj.core.api.Assertions.assertThat(responseBody)
                .doesNotContain("password")
                .doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("Should successfully get user by email")
    void testGetUserByEmail_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/user/email/profile@example.com"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("profileuser"))
                .andExpect(jsonPath("$.email").value("profile@example.com"));
    }

    @Test
    @DisplayName("Should return 404 when getting user by non-existent email")
    void testGetUserByEmail_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/user/email/nonexistent@example.com"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}


package com.gn.reminder.userservice.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.LoginRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MongoTestContainerConfig.class)
@DisplayName("Auth Integration Tests")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        userRepo.deleteAll();
    }

    @Test
    @DisplayName("Should successfully signup a new user")
    void testSignup_Success() throws Exception {
        // Given
        SignupRequest signupRequest = SignupRequest.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("SecurePassword123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400000));

        // Verify user was created in database
        assertThat(userRepo.existsByUsername("newuser")).isTrue();
        assertThat(userRepo.existsByEmail("newuser@example.com")).isTrue();
    }

    @Test
    @DisplayName("Should fail signup with duplicate username")
    void testSignup_DuplicateUsername() throws Exception {
        // Given - Create first user
        SignupRequest firstRequest = SignupRequest.builder()
                .username("duplicateuser")
                .email("first@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // When - Try to create user with same username
        SignupRequest duplicateRequest = SignupRequest.builder()
                .username("duplicateuser")
                .email("different@example.com")
                .password("Password123")
                .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Username already exists")));
    }

    @Test
    @DisplayName("Should fail signup with duplicate email")
    void testSignup_DuplicateEmail() throws Exception {
        // Given - Create first user
        SignupRequest firstRequest = SignupRequest.builder()
                .username("user1")
                .email("duplicate@example.com")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // When - Try to create user with same email
        SignupRequest duplicateRequest = SignupRequest.builder()
                .username("user2")
                .email("duplicate@example.com")
                .password("Password123")
                .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Email already exists")));
    }

    @Test
    @DisplayName("Should fail signup with invalid email format")
    void testSignup_InvalidEmail() throws Exception {
        // Given
        SignupRequest invalidRequest = SignupRequest.builder()
                .username("testuser")
                .email("invalid-email")
                .password("Password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should successfully login with username")
    void testLogin_WithUsername_Success() throws Exception {
        // Given - Create a user first
        SignupRequest signupRequest = SignupRequest.builder()
                .username("loginuser")
                .email("loginuser@example.com")
                .password("MyPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // When - Login with username
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("loginuser")
                .password("MyPassword123")
                .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.email").value("loginuser@example.com"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Should successfully login with email")
    void testLogin_WithEmail_Success() throws Exception {
        // Given - Create a user first
        SignupRequest signupRequest = SignupRequest.builder()
                .username("emaillogin")
                .email("emaillogin@example.com")
                .password("SecurePass456")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // When - Login with email
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("emaillogin@example.com")
                .password("SecurePass456")
                .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("emaillogin"))
                .andExpect(jsonPath("$.email").value("emaillogin@example.com"));
    }

    @Test
    @DisplayName("Should fail login with wrong password")
    void testLogin_WrongPassword() throws Exception {
        // Given - Create a user first
        SignupRequest signupRequest = SignupRequest.builder()
                .username("secureuser")
                .email("secure@example.com")
                .password("CorrectPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // When - Login with wrong password
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("secureuser")
                .password("WrongPassword123")
                .build();

        // Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Should fail login with non-existent user")
    void testLogin_NonExistentUser() throws Exception {
        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .usernameOrEmail("nonexistent")
                .password("SomePassword123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Should validate valid JWT token")
    void testValidateToken_Success() throws Exception {
        // Given - Create user and get token
        SignupRequest signupRequest = SignupRequest.builder()
                .username("tokenuser")
                .email("token@example.com")
                .password("TokenPass123")
                .build();

        MvcResult signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = signupResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        String token = authResponse.getToken();

        // When & Then
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value("tokenuser"));
    }

    @Test
    @DisplayName("Should reject invalid JWT token")
    void testValidateToken_Invalid() throws Exception {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer " + invalidToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid token"));
    }

    @Test
    @DisplayName("Should reject token validation without Authorization header")
    void testValidateToken_NoHeader() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/auth/validate"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing required header: Authorization"));
    }
}


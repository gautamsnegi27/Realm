package com.gn.reminder.userservice.user.controller;

import com.gn.reminder.userservice.shared.exception.UserNotFoundException;
import com.gn.reminder.userservice.user.dto.Profile;
import com.gn.reminder.userservice.user.dto.UserProfileResponse;
import com.gn.reminder.userservice.user.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UserProfileResponse mockProfileResponse;

    @BeforeEach
    void setUp() {
        Profile profile = Profile.builder()
                .firstName("John")
                .lastName("Doe")
                .bio("Test bio")
                .build();

        mockProfileResponse = UserProfileResponse.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .profile(profile)
                .isVerified(true)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("Should successfully get current user profile")
    void testGetCurrentUser_Success() throws Exception {
        // Given
        when(userService.getUserProfile("user123")).thenReturn(mockProfileResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/user")
                        .header("X-Auth-User-Id", "user123")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.profile.firstName").value("John"))
                .andExpect(jsonPath("$.profile.lastName").value("Doe"));

        verify(userService).getUserProfile("user123");
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when user not found")
    void testGetCurrentUser_NotFound() throws Exception {
        // Given
        when(userService.getUserProfile("invalid-id"))
                .thenThrow(new UserNotFoundException("no user found with provided id: invalid-id"));

        // When & Then
        mockMvc.perform(get("/api/v1/user")
                        .header("X-Auth-User-Id", "invalid-id")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(userService).getUserProfile("invalid-id");
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when X-Auth-User-Id header is missing")
    void testGetCurrentUser_MissingHeader() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/user")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUserProfile(anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should verify password is not exposed in response")
    void testGetCurrentUser_PasswordNotExposed() throws Exception {
        // Given
        when(userService.getUserProfile("user123")).thenReturn(mockProfileResponse);

        // When
        String responseBody = mockMvc.perform(get("/api/v1/user")
                        .header("X-Auth-User-Id", "user123")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Then
        org.assertj.core.api.Assertions.assertThat(responseBody)
                .doesNotContain("password")
                .doesNotContain("passwordHash");
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle user with null profile")
    void testGetCurrentUser_NullProfile() throws Exception {
        // Given
        UserProfileResponse responseWithoutProfile = UserProfileResponse.builder()
                .id("user456")
                .username("usernoprofile")
                .email("noprofile@example.com")
                .profile(null)
                .isVerified(false)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();

        when(userService.getUserProfile("user456")).thenReturn(responseWithoutProfile);

        // When & Then
        mockMvc.perform(get("/api/v1/user")
                        .header("X-Auth-User-Id", "user456")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user456"))
                .andExpect(jsonPath("$.username").value("usernoprofile"))
                .andExpect(jsonPath("$.profile").isEmpty());

        verify(userService).getUserProfile("user456");
    }
}


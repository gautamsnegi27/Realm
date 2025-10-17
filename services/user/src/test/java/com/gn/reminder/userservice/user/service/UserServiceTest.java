package com.gn.reminder.userservice.user.service;

import com.gn.reminder.userservice.shared.exception.UserNotFoundException;
import com.gn.reminder.userservice.user.domain.User;
import com.gn.reminder.userservice.user.dto.*;
import com.gn.reminder.userservice.user.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        Profile profile = Profile.builder()
                .firstName("John")
                .lastName("Doe")
                .bio("Test bio")
                .build();

        mockUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .profile(profile)
                .isVerified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastLoginAt(Instant.now())
                .build();

        userRequest = new UserRequest(
                "user123",
                "test@example.com",
                "testuser",
                "John",
                "Doe",
                null,
                "Test bio"
        );
    }

    @Test
    @DisplayName("Should successfully create a new user")
    void testCreateUser_Success() {
        // Given
        when(userRepo.save(any(User.class))).thenReturn(mockUser);

        // When
        String userId = userService.createUser(userRequest);

        // Then
        assertThat(userId).isEqualTo("user123");
        verify(userRepo).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully get user by email")
    void testGetUser_Success() {
        // Given
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        // When
      UserProfileResponse response = userService.getUser("test@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getProfile()).isNotNull();
        assertThat(response.getProfile().getFirstName()).isEqualTo("John");

        verify(userRepo).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when getting user with non-existent email")
    void testGetUser_NotFound() {
        // Given
        when(userRepo.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUser("nonexistent@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("no user found with provided email");

        verify(userRepo).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should successfully get user profile by ID")
    void testGetUserProfile_Success() {
        // Given
        when(userRepo.findById("user123")).thenReturn(Optional.of(mockUser));

        // When
        UserProfileResponse response = userService.getUserProfile("user123");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("user123");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.isVerified()).isTrue();
        assertThat(response.getProfile()).isNotNull();
        assertThat(response.getProfile().getFirstName()).isEqualTo("John");

        verify(userRepo).findById("user123");
    }

    @Test
    @DisplayName("Should throw exception when getting profile with non-existent user ID")
    void testGetUserProfile_NotFound() {
        // Given
        when(userRepo.findById("invalid-id")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserProfile("invalid-id"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("no user found with provided id");

        verify(userRepo).findById("invalid-id");
    }

    @Test
    @DisplayName("Should successfully update user")
    void testUpdateUser_Success() {
        // Given
        when(userRepo.findById("userId")).thenReturn(Optional.of(mockUser));
        when(userRepo.save(any(User.class))).thenReturn(mockUser);

        // When
        userService.updateUser("userId", userRequest);

        // Then
        verify(userRepo).findById("userId");
        verify(userRepo).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void testUpdateUser_NotFound() {
        // Given
        when(userRepo.findById("nonexistent")).thenReturn(Optional.empty());

        UserRequest nonExistentRequest = new UserRequest(
                "user999",
                "nonexistent@example.com",
                "nouser",
                "No",
                "User",
                null,
                null
        );

        // When & Then
        assertThatThrownBy(() -> userService.updateUser("nonexistent", nonExistentRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("update user:: no user found with provided id");

        verify(userRepo).findById("nonexistent");
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle user with null profile")
    void testGetUserProfile_NullProfile() {
        // Given
        User userWithoutProfile = User.builder()
                .id("user456")
                .username("usernoprofile")
                .email("noprofile@example.com")
                .passwordHash("hashedPassword")
                .profile(null)
                .isVerified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepo.findById("user456")).thenReturn(Optional.of(userWithoutProfile));

        // When
        UserProfileResponse response = userService.getUserProfile("user456");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("user456");
        assertThat(response.getUsername()).isEqualTo("usernoprofile");
        assertThat(response.getEmail()).isEqualTo("noprofile@example.com");
        assertThat(response.getProfile()).isNull();

        verify(userRepo).findById("user456");
    }
}


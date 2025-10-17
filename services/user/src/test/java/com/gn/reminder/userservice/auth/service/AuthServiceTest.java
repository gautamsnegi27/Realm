package com.gn.reminder.userservice.auth.service;

import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.LoginRequest;
import com.gn.reminder.userservice.auth.dto.SignupRequest;
import com.gn.reminder.userservice.auth.util.JwtUtil;
import com.gn.reminder.userservice.shared.exception.UserNotFoundException;
import com.gn.reminder.userservice.user.domain.User;
import com.gn.reminder.userservice.user.repository.UserRepo;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

  @Mock
  private UserRepo userRepo;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtUtil jwtUtil;

  @InjectMocks
  private AuthService authService;

  private SignupRequest signupRequest;
  private LoginRequest loginRequest;
  private User mockUser;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);

    signupRequest = SignupRequest.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();

    loginRequest = LoginRequest.builder()
            .usernameOrEmail("testuser")
            .password("password123")
            .build();

    mockUser = User.builder()
            .id("user123")
            .username("testuser")
            .email("test@example.com")
            .passwordHash("encodedPassword")
            .isVerified(false)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
  }

  @Test
  @DisplayName("Should successfully signup a new user")
  void testSignup_Success() {
    // Given
    when(userRepo.existsByUsername(signupRequest.getUsername())).thenReturn(false);
    when(userRepo.existsByEmail(signupRequest.getEmail())).thenReturn(false);
    when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn("encodedPassword");
    when(userRepo.save(any(User.class))).thenReturn(mockUser);
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");

    // When
    AuthResponse response = authService.signup(signupRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isEqualTo("jwt-token");
    assertThat(response.getUsername()).isEqualTo("testuser");
    assertThat(response.getEmail()).isEqualTo("test@example.com");
    assertThat(response.getUserId()).isEqualTo("user123");

    verify(userRepo).existsByUsername("testuser");
    verify(userRepo).existsByEmail("test@example.com");
    verify(passwordEncoder).encode("password123");
    verify(userRepo).save(any(User.class));
    verify(jwtUtil).generateToken("testuser", "test@example.com", "user123");
  }

  @Test
  @DisplayName("Should throw exception when username already exists")
  void testSignup_UsernameExists() {
    // Given
    when(userRepo.existsByUsername(signupRequest.getUsername())).thenReturn(true);

    // When & Then
    assertThatThrownBy(() -> authService.signup(signupRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Username already exists");

    verify(userRepo).existsByUsername("testuser");
    verify(userRepo, never()).existsByEmail(anyString());
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Should throw exception when email already exists")
  void testSignup_EmailExists() {
    // Given
    when(userRepo.existsByUsername(signupRequest.getUsername())).thenReturn(false);
    when(userRepo.existsByEmail(signupRequest.getEmail())).thenReturn(true);

    // When & Then
    assertThatThrownBy(() -> authService.signup(signupRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email already exists");

    verify(userRepo).existsByUsername("testuser");
    verify(userRepo).existsByEmail("test@example.com");
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Should successfully login with username")
  void testLogin_WithUsername_Success() {
    // Given
    when(userRepo.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(mockUser));
    when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
    when(userRepo.save(any(User.class))).thenReturn(mockUser);
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");

    // When
    AuthResponse response = authService.login(loginRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isEqualTo("jwt-token");
    assertThat(response.getUsername()).isEqualTo("testuser");
    assertThat(response.getEmail()).isEqualTo("test@example.com");

    verify(userRepo).findByUsernameOrEmail("testuser", "testuser");
    verify(passwordEncoder).matches("password123", "encodedPassword");
    verify(userRepo).save(any(User.class));
    verify(jwtUtil).generateToken("testuser", "test@example.com", "user123");
  }

  @Test
  @DisplayName("Should successfully login with email")
  void testLogin_WithEmail_Success() {
    // Given
    LoginRequest emailLoginRequest = LoginRequest.builder()
            .usernameOrEmail("test@example.com")
            .password("password123")
            .build();

    when(userRepo.findByUsernameOrEmail("test@example.com", "test@example.com")).thenReturn(Optional.of(mockUser));
    when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
    when(userRepo.save(any(User.class))).thenReturn(mockUser);
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");

    // When
    AuthResponse response = authService.login(emailLoginRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isEqualTo("jwt-token");

    verify(userRepo).findByUsernameOrEmail("test@example.com", "test@example.com");
  }

  @Test
  @DisplayName("Should throw exception when user not found during login")
  void testLogin_UserNotFound() {
    // Given
    when(userRepo.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("User not found");

    verify(userRepo).findByUsernameOrEmail("testuser", "testuser");
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  @DisplayName("Should throw exception when password is incorrect")
  void testLogin_InvalidPassword() {
    // Given
    when(userRepo.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(mockUser));
    when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

    // When & Then
    assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("Invalid credentials");

    verify(userRepo).findByUsernameOrEmail("testuser", "testuser");
    verify(passwordEncoder).matches("password123", "encodedPassword");
    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  @DisplayName("Should validate token successfully")
  void testValidateToken_Success() {
    // Given
    String token = "valid-jwt-token";
    when(jwtUtil.validateToken(token)).thenReturn(true);

    // When
    boolean isValid = authService.validateToken(token);

    // Then
    assertThat(isValid).isTrue();
    verify(jwtUtil).validateToken(token);
  }

  @Test
  @DisplayName("Should return false for invalid token")
  void testValidateToken_Invalid() {
    // Given
    String token = "invalid-jwt-token";
    when(jwtUtil.validateToken(token)).thenReturn(false);

    // When
    boolean isValid = authService.validateToken(token);

    // Then
    assertThat(isValid).isFalse();
    verify(jwtUtil).validateToken(token);
  }

  @Test
  @DisplayName("Should extract username from token")
  void testExtractUsername() {
    // Given
    String token = "valid-jwt-token";
    when(jwtUtil.extractUsername(token)).thenReturn("testuser");

    // When
    String username = authService.extractUsername(token);

    // Then
    assertThat(username).isEqualTo("testuser");
    verify(jwtUtil).extractUsername(token);
  }
}


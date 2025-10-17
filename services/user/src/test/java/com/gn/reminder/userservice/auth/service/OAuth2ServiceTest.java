package com.gn.reminder.userservice.auth.service;

import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.OAuth2LoginRequest;
import com.gn.reminder.userservice.auth.util.JwtUtil;
import com.gn.reminder.userservice.user.domain.User;
import com.gn.reminder.userservice.user.dto.Profile;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2Service Unit Tests")
class OAuth2ServiceTest {

  @Mock
  private UserRepo userRepository;

  @Mock
  private JwtUtil jwtUtil;

  @InjectMocks
  private OAuth2Service oAuth2Service;

  private OAuth2LoginRequest googleLoginRequest;
  private OAuth2LoginRequest githubLoginRequest;
  private User existingUser;
  private User newUser;

  @BeforeEach
  void setUp() {
    // Setup Google OAuth2 login request
    googleLoginRequest = OAuth2LoginRequest.builder()
            .provider("google")
            .providerId("google-user-123")
            .email("user@gmail.com")
            .username("googleuser")
            .firstName("John")
            .lastName("Doe")
            .accessToken("google-access-token-xyz")
            .build();

    // Setup GitHub OAuth2 login request
    githubLoginRequest = OAuth2LoginRequest.builder()
            .provider("github")
            .providerId("github-user-456")
            .email("developer@github.com")
            .username("githubdev")
            .firstName("Jane")
            .lastName("Smith")
            .accessToken("github-access-token-abc")
            .build();

    // Setup existing user
    existingUser = User.builder()
            .id("existing-user-id-123")
            .username("existinguser")
            .email("user@gmail.com")
            .profile(Profile.builder()
                    .firstName("OldFirstName")
                    .lastName("OldLastName")
                    .build())
            .isVerified(false)
            .createdAt(Instant.now().minusSeconds(86400))
            .updatedAt(Instant.now().minusSeconds(86400))
            .build();

    // Setup new user
    newUser = User.builder()
            .id("new-user-id-789")
            .username("googleuser")
            .email("user@gmail.com")
            .profile(Profile.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .build())
            .isVerified(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .lastLoginAt(Instant.now())
            .build();
  }

  @Test
  @DisplayName("Should create new user from Google OAuth2 login")
  void testHandleOAuth2Login_NewUser_Google() {
    // Arrange
    when(userRepository.findByEmail(googleLoginRequest.getEmail())).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(googleLoginRequest.getUsername())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(newUser);
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token-123");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(googleLoginRequest);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isEqualTo("jwt-token-123");
    assertThat(response.getTokenType()).isEqualTo("Bearer");
    assertThat(response.getExpiresIn()).isEqualTo(86400000L);
    assertThat(response.getUsername()).isEqualTo(newUser.getUsername());
    assertThat(response.getEmail()).isEqualTo(newUser.getEmail());
    assertThat(response.getUserId()).isEqualTo(newUser.getId());

    // Verify interactions
    verify(userRepository, times(1)).findByEmail(googleLoginRequest.getEmail());
    verify(userRepository, times(1)).save(any(User.class));
    verify(jwtUtil, times(1)).generateToken(
            newUser.getUsername(),
            newUser.getEmail(),
            newUser.getId()
    );
  }

  @Test
  @DisplayName("Should create new user from GitHub OAuth2 login")
  void testHandleOAuth2Login_NewUser_GitHub() {
    // Arrange
    User githubUser = User.builder()
            .id("github-user-id-456")
            .username("githubdev")
            .email("developer@github.com")
            .profile(Profile.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .build())
            .isVerified(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .lastLoginAt(Instant.now())
            .build();

    when(userRepository.findByEmail(githubLoginRequest.getEmail())).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(githubLoginRequest.getUsername())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(githubUser);
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token-456");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(githubLoginRequest);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isEqualTo("jwt-token-456");
    assertThat(response.getUsername()).isEqualTo("githubdev");
    assertThat(response.getEmail()).isEqualTo("developer@github.com");

    verify(userRepository, times(1)).findByEmail(githubLoginRequest.getEmail());
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  @DisplayName("Should update existing user on OAuth2 login")
  void testHandleOAuth2Login_ExistingUser() {
    // Arrange
    User updatedUser = existingUser.toBuilder()
            .isVerified(true)
            .profile(Profile.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .build())
            .updatedAt(Instant.now())
            .lastLoginAt(Instant.now())
            .build();

    when(userRepository.findByEmail(googleLoginRequest.getEmail())).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(updatedUser);
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token-existing");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(googleLoginRequest);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isEqualTo("jwt-token-existing");
    assertThat(response.getUserId()).isEqualTo(existingUser.getId());

    verify(userRepository, times(1)).findByEmail(googleLoginRequest.getEmail());
    verify(userRepository, times(1)).save(any(User.class));
    verify(jwtUtil, times(1)).generateToken(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Should mark existing user as verified after OAuth2 login")
  void testHandleOAuth2Login_ShouldVerifyExistingUser() {
    // Arrange
    User unverifiedUser = User.builder()
            .id("unverified-user-123")
            .username("unverified")
            .email("unverified@example.com")
            .isVerified(false)
            .createdAt(Instant.now().minusSeconds(3600))
            .updatedAt(Instant.now().minusSeconds(3600))
            .build();

    OAuth2LoginRequest request = OAuth2LoginRequest.builder()
            .provider("google")
            .providerId("google-123")
            .email("unverified@example.com")
            .username("unverified")
            .firstName("Test")
            .lastName("User")
            .accessToken("token")
            .build();

    User verifiedUser = unverifiedUser.toBuilder()
            .isVerified(true)
            .build();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(unverifiedUser));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User savedUser = invocation.getArgument(0);
      assertThat(savedUser.isVerified()).isTrue();
      return verifiedUser;
    });
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(request);

    // Assert
    assertThat(response).isNotNull();
    verify(userRepository, times(1)).save(argThat(User::isVerified));
  }

  @Test
  @DisplayName("Should update profile fields when they are null in existing user")
  void testHandleOAuth2Login_UpdatesNullProfileFields() {
    // Arrange
    User userWithNullProfile = User.builder()
            .id("user-123")
            .username("testuser")
            .email("test@example.com")
            .profile(Profile.builder().build()) // Empty profile
            .isVerified(false)
            .createdAt(Instant.now().minusSeconds(3600))
            .updatedAt(Instant.now().minusSeconds(3600))
            .build();

    OAuth2LoginRequest request = OAuth2LoginRequest.builder()
            .provider("google")
            .providerId("google-123")
            .email("test@example.com")
            .username("testuser")
            .firstName("NewFirst")
            .lastName("NewLast")
            .accessToken("token")
            .build();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(userWithNullProfile));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(request);

    // Assert
    assertThat(response).isNotNull();
    verify(userRepository, times(1)).save(argThat(user -> {
      Profile profile = user.getProfile();
      return profile != null &&
              "NewFirst".equals(profile.getFirstName()) &&
              "NewLast".equals(profile.getLastName());
    }));
  }

  @Test
  @DisplayName("Should NOT update profile fields when they already exist")
  void testHandleOAuth2Login_DoesNotUpdateExistingProfileFields() {
    // Arrange
    User userWithProfile = User.builder()
            .id("user-123")
            .username("testuser")
            .email("test@example.com")
            .profile(Profile.builder()
                    .firstName("ExistingFirst")
                    .lastName("ExistingLast")
                    .build())
            .isVerified(true)
            .createdAt(Instant.now().minusSeconds(3600))
            .updatedAt(Instant.now().minusSeconds(3600))
            .build();

    OAuth2LoginRequest request = OAuth2LoginRequest.builder()
            .provider("google")
            .providerId("google-123")
            .email("test@example.com")
            .username("testuser")
            .firstName("NewFirst")
            .lastName("NewLast")
            .accessToken("token")
            .build();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(userWithProfile));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(request);

    // Assert
    assertThat(response).isNotNull();
    verify(userRepository, times(1)).save(argThat(user -> {
      Profile profile = user.getProfile();
      return profile != null &&
              "ExistingFirst".equals(profile.getFirstName()) &&
              "ExistingLast".equals(profile.getLastName());
    }));
  }

  @Test
  @DisplayName("Should generate unique username when preferred username exists")
  void testHandleOAuth2Login_GeneratesUniqueUsername() {
    // Arrange
    OAuth2LoginRequest request = OAuth2LoginRequest.builder()
            .provider("google")
            .providerId("google-123")
            .email("newuser@example.com")
            .username("existinguser")
            .firstName("New")
            .lastName("User")
            .accessToken("token")
            .build();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
    when(userRepository.existsByUsername("existinguser")).thenReturn(true);
    when(userRepository.existsByUsername("existinguser1")).thenReturn(true);
    when(userRepository.existsByUsername("existinguser2")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User savedUser = invocation.getArgument(0);
      return savedUser.toBuilder().id("new-id-123").build();
    });
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(request);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getUsername()).isEqualTo("existinguser2");
    verify(userRepository, times(1)).existsByUsername("existinguser");
    verify(userRepository, times(1)).existsByUsername("existinguser1");
    verify(userRepository, times(1)).existsByUsername("existinguser2");
  }

  @Test
  @DisplayName("Should use email prefix when username is null")
  void testHandleOAuth2Login_UsesEmailPrefixWhenUsernameNull() {
    // Arrange
    OAuth2LoginRequest request = OAuth2LoginRequest.builder()
            .provider("github")
            .providerId("github-789")
            .email("developer@example.com")
            .username(null) // No username provided
            .firstName("Dev")
            .lastName("User")
            .accessToken("token")
            .build();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
    when(userRepository.existsByUsername("developer")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User savedUser = invocation.getArgument(0);
      return savedUser.toBuilder().id("new-id-456").build();
    });
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(request);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getUsername()).isEqualTo("developer");
    verify(userRepository, times(1)).existsByUsername("developer");
  }

  @Test
  @DisplayName("Should set user as verified on creation")
  void testHandleOAuth2Login_NewUserIsVerified() {
    // Arrange
    when(userRepository.findByEmail(googleLoginRequest.getEmail())).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(googleLoginRequest.getUsername())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User savedUser = invocation.getArgument(0);
      assertThat(savedUser.isVerified()).isTrue();
      return savedUser.toBuilder().id("new-id-999").build();
    });
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(googleLoginRequest);

    // Assert
    assertThat(response).isNotNull();
    verify(userRepository, times(1)).save(argThat(User::isVerified));
  }

  @Test
  @DisplayName("Should not set passwordHash for OAuth2 users")
  void testHandleOAuth2Login_NoPasswordForOAuth2Users() {
    // Arrange
    when(userRepository.findByEmail(googleLoginRequest.getEmail())).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(googleLoginRequest.getUsername())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User savedUser = invocation.getArgument(0);
      assertThat(savedUser.getPasswordHash()).isNull();
      return savedUser.toBuilder().id("new-id-888").build();
    });
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(googleLoginRequest);

    // Assert
    assertThat(response).isNotNull();
    verify(userRepository, times(1)).save(argThat(user -> user.getPasswordHash() == null));
  }

  @Test
  @DisplayName("Should update lastLoginAt timestamp on existing user login")
  void testHandleOAuth2Login_UpdatesLastLoginAt() {
    // Arrange
    Instant oldLoginTime = Instant.now().minusSeconds(86400);
    User userWithOldLogin = existingUser.toBuilder()
            .lastLoginAt(oldLoginTime)
            .build();

    when(userRepository.findByEmail(googleLoginRequest.getEmail())).thenReturn(Optional.of(userWithOldLogin));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    Instant beforeLogin = Instant.now();
    AuthResponse response = oAuth2Service.handleOAuth2Login(googleLoginRequest);
    Instant afterLogin = Instant.now();

    // Assert
    assertThat(response).isNotNull();
    verify(userRepository, times(1)).save(argThat(user -> {
      Instant lastLogin = user.getLastLoginAt();
      return lastLogin != null &&
              !lastLogin.isBefore(beforeLogin) &&
              !lastLogin.isAfter(afterLogin);
    }));
  }

  @Test
  @DisplayName("Should create profile with OAuth2 data for new user")
  void testHandleOAuth2Login_CreatesProfileWithOAuth2Data() {
    // Arrange
    when(userRepository.findByEmail(googleLoginRequest.getEmail())).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(googleLoginRequest.getUsername())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User savedUser = invocation.getArgument(0);
      return savedUser.toBuilder().id("profile-test-id").build();
    });
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(googleLoginRequest);

    // Assert
    assertThat(response).isNotNull();
    verify(userRepository, times(1)).save(argThat(user -> {
      Profile profile = user.getProfile();
      return profile != null &&
              googleLoginRequest.getFirstName().equals(profile.getFirstName()) &&
              googleLoginRequest.getLastName().equals(profile.getLastName());
    }));
  }

  @Test
  @DisplayName("Should generate JWT token with correct user details")
  void testHandleOAuth2Login_GeneratesCorrectJWT() {
    // Arrange
    when(userRepository.findByEmail(googleLoginRequest.getEmail())).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(googleLoginRequest.getUsername())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(newUser);
    when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("correct-jwt-token");
    when(jwtUtil.getExpiration()).thenReturn(86400000L);

    // Act
    AuthResponse response = oAuth2Service.handleOAuth2Login(googleLoginRequest);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isEqualTo("correct-jwt-token");
    verify(jwtUtil, times(1)).generateToken(
            eq(newUser.getUsername()),
            eq(newUser.getEmail()),
            eq(newUser.getId())
    );
  }
}




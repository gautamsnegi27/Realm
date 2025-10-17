package com.gn.reminder.userservice.auth.service;

import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.OAuth2LoginRequest;
import com.gn.reminder.userservice.auth.util.JwtUtil;
import com.gn.reminder.userservice.user.domain.User;
import com.gn.reminder.userservice.user.dto.Profile;
import com.gn.reminder.userservice.user.repository.UserRepo;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling OAuth2/Social Login integration with Keycloak
 * Manages user creation/sync between Keycloak and MongoDB
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

  private final UserRepo userRepository;
  private final JwtUtil jwtUtil;

  /**
   * Handle OAuth2 login - create or update user from social provider
   *
   * @param request OAuth2 login request with user info from Keycloak
   * @return AuthResponse with JWT token
   */
  @Transactional
  public AuthResponse handleOAuth2Login(OAuth2LoginRequest request) {
    log.info("Processing OAuth2 login for email: {} from provider: {}",
            request.getEmail(), request.getProvider());

    // Check if user exists by email
    var existingUser = userRepository.findByEmail(request.getEmail());

    User user;
    if (existingUser.isPresent()) {
      user = existingUser.get();
      log.info("Existing user found, updating social account info");
      user = updateSocialAccount(user, request);
    } else {
      log.info("New user from OAuth2, creating account");
      user = createUserFromOAuth2(request);
    }

    user = userRepository.save(user);

    // Generate custom JWT token for this user
    var token = jwtUtil.generateToken(
            user.getUsername(),
            user.getEmail(),
            user.getId()
    );

    log.info("OAuth2 login successful for user: {}", user.getUsername());

    return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .expiresIn(jwtUtil.getExpiration())
            .username(user.getUsername())
            .email(user.getEmail())
            .userId(user.getId())
            .build();
  }

  /**
   * Create a new user from OAuth2 provider information
   */
  private User createUserFromOAuth2(OAuth2LoginRequest request) {
    var now = Instant.now();

    var profile = Profile.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .build();

    return User.builder()
            .username(generateUniqueUsername(request.getUsername(), request.getEmail()))
            .email(request.getEmail())
            .profile(profile)
            .isVerified(true) // OAuth2 providers verify emails
            .createdAt(now)
            .updatedAt(now)
            .lastLoginAt(now)
            // No passwordHash for OAuth2 users
            .build();

  }

  /**
   * Update existing user with social account information
   */
  private User updateSocialAccount(User user, OAuth2LoginRequest request) {
    Instant now = Instant.now();

    // Update profile if needed
    Profile profile = user.getProfile();
    if (profile != null) {
      Profile.ProfileBuilder profileBuilder = profile.toBuilder();

      if (profile.getFirstName() == null && request.getFirstName() != null) {
        profileBuilder.firstName(request.getFirstName());
      }
      if (profile.getLastName() == null && request.getLastName() != null) {
        profileBuilder.lastName(request.getLastName());
      }

      profile = profileBuilder.build();
    }

    // Mark email as verified since it's from OAuth2 provider and update timestamps
    return user.toBuilder()
            .isVerified(true)
            .profile(profile)
            .updatedAt(now)
            .lastLoginAt(now)
            .build();

  }

  /**
   * Generate unique username from OAuth2 info
   */
  private String generateUniqueUsername(String preferredUsername, String email) {
    var baseUsername = preferredUsername != null ? preferredUsername : email.split("@")[0];
    var username = baseUsername;
    int suffix = 1;

    // Keep trying until we find a unique username
    while (userRepository.existsByUsername(username)) {
      username = baseUsername + suffix;
      suffix++;
    }

    return username;
  }
}


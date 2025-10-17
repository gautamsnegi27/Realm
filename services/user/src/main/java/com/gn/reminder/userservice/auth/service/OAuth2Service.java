package com.gn.reminder.userservice.auth.service;

import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.OAuth2LoginRequest;
import com.gn.reminder.userservice.auth.dto.OAuth2CallbackRequest;
import com.gn.reminder.userservice.auth.util.JwtUtil;
import com.gn.reminder.userservice.user.domain.User;
import com.gn.reminder.userservice.user.dto.Profile;
import com.gn.reminder.userservice.user.repository.UserRepo;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${keycloak.auth-server-url:http://localhost:9191}")
  private String keycloakUrl;

  @Value("${keycloak.realm:realm-service}")
  private String keycloakRealm;

  @Value("${keycloak.credentials.secret:changeme}")
  private String clientSecret;

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
   * Handle OAuth2 callback with authorization code
   * Securely exchanges code for tokens and processes user login
   *
   * @param request OAuth2 callback request with authorization code
   * @return AuthResponse with JWT token
   */
  @Transactional
  public AuthResponse handleOAuth2Callback(OAuth2CallbackRequest request) {
    try {
      // Exchange authorization code for tokens
      var tokenResponse = exchangeCodeForTokens(request);

      // Get user info from Keycloak UserInfo endpoint
      Map<String, Object> userInfo = getUserInfoFromKeycloak(tokenResponse.get("access_token").toString());

      // Validate required fields
      if (!userInfo.containsKey("sub")) {
        throw new RuntimeException("Invalid user info: missing subject (sub) field");
      }

      // Create OAuth2LoginRequest from token info
      String email = userInfo.containsKey("email") ?
              userInfo.get("email").toString() :
              userInfo.get("sub").toString() + "@" + request.getProvider() + ".local";

      String username = userInfo.containsKey("preferred_username") ?
              userInfo.get("preferred_username").toString() :
              (userInfo.containsKey("email") ?
                      userInfo.get("email").toString().split("@")[0] :
                      userInfo.get("sub").toString());

      var oauth2LoginRequest = OAuth2LoginRequest.builder()
              .provider(request.getProvider())
              .providerId(userInfo.get("sub").toString())
              .email(email)
              .username(username)
              .firstName(userInfo.getOrDefault("given_name", "").toString())
              .lastName(userInfo.getOrDefault("family_name", "").toString())
              .accessToken(tokenResponse.get("access_token").toString())
              .build();

      // Process the OAuth2 login
      return handleOAuth2Login(oauth2LoginRequest);

    } catch (Exception e) {
      log.error("OAuth2 callback processing failed", e);
      throw new RuntimeException("OAuth2 authentication failed: " + e.getMessage());
    }
  }

  /**
   * Exchange authorization code for tokens with Keycloak
   */
  private Map<String, Object> exchangeCodeForTokens(OAuth2CallbackRequest request) {
    var tokenEndpoint = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/token";

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setBasicAuth("user-service-client", clientSecret);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "authorization_code");
    body.add("code", request.getCode());
    body.add("redirect_uri", request.getRedirectUri());

    var requestEntity = new HttpEntity<>(body, headers);

    try {
      var response = restTemplate.exchange(
              tokenEndpoint,
              HttpMethod.POST,
              requestEntity,
              Map.class
      );

      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new RuntimeException("Failed to exchange authorization code for tokens");
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

      // Validate response contains access token
      if (responseBody == null || !responseBody.containsKey("access_token")) {
        throw new RuntimeException("Invalid token response: missing access_token");
      }

      return responseBody;

    } catch (Exception e) {
      log.error("Token exchange failed: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * Get user information from Keycloak UserInfo endpoint
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getUserInfoFromKeycloak(String accessToken) {
    var userInfoEndpoint = keycloakUrl + "/realms/" + keycloakRealm + "/protocol/openid-connect/userinfo";

    var headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    var requestEntity = new HttpEntity<>(headers);

    try {
      var response = restTemplate.exchange(
              userInfoEndpoint,
              HttpMethod.GET,
              requestEntity,
              Map.class
      );

      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new RuntimeException("Failed to fetch user info from Keycloak");
      }

      return (Map<String, Object>) response.getBody();

    } catch (Exception e) {
      log.error("Failed to fetch user info: {}", e.getMessage());
      throw new RuntimeException("Failed to fetch user info: " + e.getMessage());
    }
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


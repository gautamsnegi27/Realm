package com.gn.reminder.userservice.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OAuth2 social login
 * Contains user information received from Keycloak after successful social authentication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2LoginRequest {
  private String provider; // "google", "github", etc.

  private String providerId; // User ID from the provider

  private String email;

  private String username;

  private String firstName;

  private String lastName;

  private String accessToken; // Keycloak access token
}




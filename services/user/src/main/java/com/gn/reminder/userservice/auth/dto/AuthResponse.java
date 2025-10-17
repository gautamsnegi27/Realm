package com.gn.reminder.userservice.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
  private String token;

  @Builder.Default
  private String tokenType = "Bearer";

  private Long expiresIn;

  private String username;

  private String email;

  private String userId;

  public AuthResponse(String token, Long expiresIn, String username, String email, String userId) {
    this.token = token;
    this.tokenType = "Bearer";
    this.expiresIn = expiresIn;
    this.username = username;
    this.email = email;
    this.userId = userId;
  }
}


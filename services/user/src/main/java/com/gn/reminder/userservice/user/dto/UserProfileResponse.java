package com.gn.reminder.userservice.user.dto;

import com.gn.reminder.userservice.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
  private String id;

  private String username;

  private String email;

  private boolean isVerified;

  private Profile profile;

  private VerificationDetails verificationDetails;

  private String createdAt;

  private String updatedAt;

  private String lastLoginAt;

  public static UserProfileResponse fromUser(User user) {
    return UserProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .isVerified(user.isVerified())
            .profile(user.getProfile())
            .verificationDetails(user.getVerificationDetails())
            .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
            .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
            .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
            .build();
  }
}


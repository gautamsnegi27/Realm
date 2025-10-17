package com.gn.reminder.userservice.user.domain;

import com.gn.reminder.userservice.user.dto.Profile;
import com.gn.reminder.userservice.user.dto.VerificationDetails;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
@Document(collection = "user")
public class User {

  @Id
  private String id;

  @Indexed(name = "email", unique = true)
  private String email;

  @Indexed(name = "username", unique = true)
  private String username;

  private String passwordHash;

  private Profile profile;

  private boolean isVerified;

  private VerificationDetails verificationDetails;

  private Instant createdAt;

  private Instant updatedAt;

  private Instant lastLoginAt;


}


package com.gn.reminder.userservice.application.user;

import com.gn.reminder.userservice.application.user.dto.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
@Document(collection = "user")
class User {

    @Id
    private String id;

    @Indexed(name = "email", unique = true)
    private String email;

    @Indexed(name = "username", unique = true)
    private String username;

    private String passwordHash;

    private Profile profile;

    private Reputation reputation;

    private Stats stats;

    private boolean isVerified;

    private VerificationDetails verificationDetails;

    private SocialConnections socialConnections;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant lastLoginAt;


}

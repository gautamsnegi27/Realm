package com.gn.reminder.userservice.application.user.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "user_sessions")
public class UserSession {
    @Id
    private String id;
    private String userId;
    private String tokenHash;
    private DeviceInfo deviceInfo;
    private Instant expiresAt;
    private Instant createdAt;
}

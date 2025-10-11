package com.gn.reminder.userservice.application.user;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Builder(toBuilder = true)
@Document(collection = "user_followers")
class UserFollower {
    @Id
    private String id;
    private String followerId;
    private String followingId;
    private Instant createdAt;
}

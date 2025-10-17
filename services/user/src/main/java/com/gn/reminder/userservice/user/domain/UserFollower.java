package com.gn.reminder.userservice.user.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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


package com.gn.reminder.userservice.user.util;

import com.gn.reminder.userservice.user.domain.User;
import com.gn.reminder.userservice.user.dto.Profile;
import com.gn.reminder.userservice.user.dto.UserRequest;
import java.util.Objects;
import java.util.Optional;

public class UserUtil {

  private UserUtil() {
  }

  /**
   * to create user
   *
   * @param request
   * @return
   */
  public static User toUser(UserRequest request) {
    return Optional.ofNullable(request)
            .map(r -> User.builder().email(r.email().toLowerCase()).username(request.username().toLowerCase())
                    .profile(Profile.builder().firstName(request.firstName()).lastName(request.lastName())
                            .profileImageUrl(request.profileImageUrl()).bio(request.bio()).build())
                    .build())
            .orElseGet(() -> User.builder().build());
  }

  /**
   * to update user, should not override email and username
   *
   * @param request
   * @param user
   * @return
   */
  public static User updateUserProfile(UserRequest request, User user) {
    return user.toBuilder()
            .profile(buildProfile(user.getProfile(), request))
            .build();
  }

  private static Profile buildProfile(Profile profile, UserRequest request) {
    return profile.toBuilder().firstName(Objects.nonNull(request.firstName()) ? request.firstName() : profile.getFirstName())
            .lastName(Objects.nonNull(request.lastName()) ? request.lastName() : profile.getLastName())
            .profileImageUrl(Objects.nonNull(request.profileImageUrl()) ? request.profileImageUrl() : profile.getProfileImageUrl())
            .bio(Objects.nonNull(request.bio()) ? request.bio() : profile.getBio()).build();
  }
}


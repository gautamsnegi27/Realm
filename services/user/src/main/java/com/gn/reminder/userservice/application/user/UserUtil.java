package com.gn.reminder.userservice.application.user;

import java.util.Objects;
import java.util.Optional;

import com.gn.reminder.userservice.application.user.dto.Profile;
import com.gn.reminder.userservice.application.user.dto.UserRequest;

class UserUtil {

	/**
	 * to create user
	 * 
	 * @param request
	 * @return
	 */
	static User toUser(UserRequest request) {
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
	static User updateUserProfile(UserRequest request, User user) {
		return user.toBuilder()
				.profile((Objects.nonNull(user.getProfile())
						? buildProfile(user.getProfile().toBuilder(), request)
						: buildProfile(Profile.builder(), request)).build())
				.build();
	}

	private static Profile.ProfileBuilder buildProfile(Profile.ProfileBuilder builder, UserRequest request) {
		return builder.firstName(request.firstName()).lastName(request.lastName())
				.profileImageUrl(request.profileImageUrl()).bio(request.bio());
	}
}

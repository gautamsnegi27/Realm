package com.gn.reminder.userservice.application.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gn.reminder.userservice.application.user.dto.UserRequest;
import com.gn.reminder.userservice.application.user.dto.UserResponse;
import com.gn.reminder.userservice.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	Logger log = LoggerFactory.getLogger(this.getClass());

	private final UserRepo repository;

	/**
	 * method to create a new user
	 *
	 * @param request
	 * @return
	 */
	public String createUser(UserRequest request) {
		var user = repository.save(UserUtil.toUser(request));
		log.info("user created");
		return user.getId();
	}

	/**
	 * method to update an existing user
	 *
	 * @param request
	 */
	public void updateUser(UserRequest request) {
		var user = repository.findByEmail(request.email()).orElseThrow(() -> new UserNotFoundException(
				String.format("update user:: no user found with provided id: %s", request.id())));
		repository.save(UserUtil.updateUserProfile(request, user));
	}

	/**
	 * method to get user based on below param
	 *
	 * @param email
	 * @return
	 */
	public UserResponse getUser(String email) {
		var user = repository.findByEmail(email).orElseThrow(
				() -> new UserNotFoundException(String.format("no user found with provided email: %s", email)));
		return new UserResponse(user.getProfile(), user.getEmail(), user.getUsername());
	}
}

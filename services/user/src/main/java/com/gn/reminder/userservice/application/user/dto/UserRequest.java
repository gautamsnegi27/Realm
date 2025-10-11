package com.gn.reminder.userservice.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.mongodb.core.aggregation.StringOperators;

import java.util.Objects;

import static org.springframework.data.mongodb.core.aggregation.StringOperators.RegexMatch.valueOf;

public record UserRequest(String id,
		@NotBlank(message = "email is mandatory")
		@Email(message = "invalid email")
		@Pattern(
				regexp = "^[a-zA-Z0-9][a-zA-Z0-9._%+-]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
				message = "Email must start with a letter or digit and may contain letters, digits, ., _, %, +, and - before the '@'. Domain must be valid and end with a TLD of at least 2 letters."
		)
		String email,
		@NotBlank(message = "username is mandatory")
	  	@Pattern(regexp = , flags = Pattern.Flag.CASE_INSENSITIVE, message = "Username must start with a letter and can only contain letters, numbers, and underscores. It must be between 3 and 20 characters long")
	  	String username,
		@NotBlank(message = "firstname is mandatory") String firstName,
		@NotBlank(message = "lastname is mandatory") String lastName, String profileImageUrl, String bio,
						  SocialConnections socialConnections) {

	public static void validateCreation(UserRequest userRequest) {
		if (Objects.nonNull(userRequest.socialConnections) &&
				Objects.isNull(userRequest
						.socialConnections.getGoogle()) && Objects.isNull(userRequest.socialConnections.getFacebook())) {
			throw new RuntimeException("Needs atleast one authentication type but found none");
		} else if (valueOf("^[a-zA-Z][a-zA-Z0-9_]{2,19}$")) {

		}


	}
}

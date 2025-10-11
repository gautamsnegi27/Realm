package com.gn.reminder.userservice.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gn.reminder.userservice.application.user.UserService;
import com.gn.reminder.userservice.application.user.dto.UserRequest;
import com.gn.reminder.userservice.application.user.dto.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
class UserController {

	private final UserService service;

	@GetMapping("/email/{email}")
	ResponseEntity<UserResponse> getUser(@PathVariable("email") String email) {
		return ResponseEntity.ok(service.getUser(email));
	}

	@PostMapping
	ResponseEntity<String> createUser(@Valid @RequestBody UserRequest request) {
		return ResponseEntity.ok(service.createUser(request));
	}

	@PutMapping
	ResponseEntity<Void> updateUser(@Valid @RequestBody UserRequest request) {
		service.updateUser(request);
		return ResponseEntity.accepted().build();
	}

}

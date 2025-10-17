package com.gn.reminder.userservice.user.controller;

import com.gn.reminder.userservice.user.dto.UserProfileResponse;
import com.gn.reminder.userservice.user.dto.UserRequest;
import com.gn.reminder.userservice.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
@Slf4j
@Tag(name = "User Management", description = "User profile and account management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

  private final UserService service;

  /**
   * Get authenticated user's profile based on JWT token
   * The user ID is automatically extracted from the JWT token by the API Gateway
   * and passed via X-Auth-User-Id header
   *
   * @param userId User ID from JWT token (injected by gateway)
   * @return UserProfileResponse without password
   */
  @Operation(
          summary = "Get current user profile",
          description = "Retrieve the authenticated user's profile information. User ID is automatically extracted from JWT token by the API Gateway."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "200",
                  description = "User profile retrieved successfully",
                  content = @Content(
                          mediaType = "application/json",
                          schema = @Schema(implementation = UserProfileResponse.class),
                          examples = @ExampleObject(
                                  name = "User Profile",
                                  value = """
                                          {
                                            "id": "507f1f77bcf86cd799439011",
                                            "username": "johndoe",
                                            "email": "john@example.com",
                                            "isVerified": true,
                                            "profile": {
                                              "firstName": "John",
                                              "lastName": "Doe",
                                              "bio": "Software developer",
                                              "profileImageUrl": "https://example.com/avatar.jpg"
                                            },
                                            "reputation": {
                                              "score": 150,
                                              "level": "Bronze"
                                            },
                                            "stats": {
                                              "totalPosts": 25,
                                              "totalLikes": 100
                                            },
                                            "createdAt": "2024-01-15T10:30:00Z",
                                            "updatedAt": "2024-01-20T14:45:00Z",
                                            "lastLoginAt": "2024-01-21T09:15:00Z"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "401",
                  description = "Unauthorized - Invalid or missing JWT token",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Unauthorized",
                                  value = """
                                          {
                                            "error": "Unauthorized access"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "404",
                  description = "User not found",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "User Not Found",
                                  value = """
                                          {
                                            "error": "User not found"
                                          }
                                          """
                          )
                  )
          )
  })
  @GetMapping
  public ResponseEntity<UserProfileResponse> getCurrentUser(
          @Parameter(description = "User ID extracted from JWT token (automatically injected by API Gateway)", hidden = true)
          @RequestHeader("X-Auth-User-Id") String userId) {
    log.info("Getting current user profile for userId: {}", userId);
    return ResponseEntity.ok(service.getUserProfile(userId));
  }

  @Operation(
          summary = "Get user by email",
          description = "Retrieve user profile information by email address."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "200",
                  description = "User found and profile retrieved",
                  content = @Content(
                          mediaType = "application/json",
                          schema = @Schema(implementation = UserProfileResponse.class)
                  )
          ),
          @ApiResponse(
                  responseCode = "404",
                  description = "User not found with the provided email",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "User Not Found",
                                  value = """
                                          {
                                            "error": "User not found with email: user@example.com"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "401",
                  description = "Unauthorized - Invalid or missing JWT token"
          )
  })
  @GetMapping("/email/{email}")
  public ResponseEntity<UserProfileResponse> getUser(
          @Parameter(description = "Email address of the user to retrieve", required = true, example = "john@example.com")
          @PathVariable String email) {
    return ResponseEntity.ok(service.getUser(email));
  }

  @Operation(
          summary = "Create new user",
          description = "Create a new user account with profile information."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "200",
                  description = "User created successfully",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "User Created",
                                  value = """
                                          "507f1f77bcf86cd799439011"
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "400",
                  description = "Invalid input data or user already exists",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Validation Error",
                                  value = """
                                          {
                                            "error": "Email already exists"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "401",
                  description = "Unauthorized - Invalid or missing JWT token"
          )
  })
  @PostMapping
  public ResponseEntity<String> createUser(
          @Parameter(description = "User creation request with profile details", required = true)
          @Valid @RequestBody UserRequest request) {
    return ResponseEntity.ok(service.createUser(request));
  }

  @Operation(
          summary = "Update user profile",
          description = "Update the authenticated user's profile information. User ID is automatically extracted from JWT token."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "202",
                  description = "User profile updated successfully"
          ),
          @ApiResponse(
                  responseCode = "400",
                  description = "Invalid input data",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Validation Error",
                                  value = """
                                          {
                                            "error": "Invalid email format"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "401",
                  description = "Unauthorized - Invalid or missing JWT token"
          ),
          @ApiResponse(
                  responseCode = "404",
                  description = "User not found"
          )
  })
  @PutMapping
  public ResponseEntity<Void> updateUser(
          @Parameter(description = "User ID extracted from JWT token (automatically injected by API Gateway)", hidden = true)
          @RequestHeader("X-Auth-User-Id") String userId,
          @Parameter(description = "Updated user profile information", required = true)
          @RequestBody UserRequest request) {
    log.info("Updating user profile for userId: {} with request: {}", userId, request);
    service.updateUser(userId, request);
    return ResponseEntity.accepted().build();
  }

}


package com.gn.reminder.userservice.auth.controller;

import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.LoginRequest;
import com.gn.reminder.userservice.auth.dto.SignupRequest;
import com.gn.reminder.userservice.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

  private final AuthService authService;

  @Operation(
          summary = "Register a new user",
          description = "Create a new user account with username, email, and password. Returns JWT token upon successful registration."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "201",
                  description = "User successfully registered",
                  content = @Content(
                          mediaType = "application/json",
                          schema = @Schema(implementation = AuthResponse.class),
                          examples = @ExampleObject(
                                  name = "Successful Registration",
                                  value = """
                                          {
                                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                            "tokenType": "Bearer",
                                            "expiresIn": 86400,
                                            "username": "johndoe",
                                            "email": "john@example.com",
                                            "userId": "507f1f77bcf86cd799439011"
                                          }
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
                                            "error": "Username already exists"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "500",
                  description = "Internal server error",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Server Error",
                                  value = """
                                          {
                                            "error": "An error occurred during signup"
                                          }
                                          """
                          )
                  )
          )
  })
  @PostMapping(value = "/signup")
  public ResponseEntity<?> signup(
          @Parameter(description = "User registration details", required = true)
          @Valid @RequestBody SignupRequest request) {
    try {
      log.info("Signup request received for username: {}", request.getUsername());
      var response = authService.signup(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      log.error("Signup failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(createErrorResponse(e.getMessage()));
    } catch (Exception e) {
      log.error("Signup error", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(createErrorResponse("An error occurred during signup"));
    }
  }

  @Operation(
          summary = "Authenticate user",
          description = "Login with username/email and password. Returns JWT token upon successful authentication."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "200",
                  description = "User successfully authenticated",
                  content = @Content(
                          mediaType = "application/json",
                          schema = @Schema(implementation = AuthResponse.class),
                          examples = @ExampleObject(
                                  name = "Successful Login",
                                  value = """
                                          {
                                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                            "tokenType": "Bearer",
                                            "expiresIn": 86400,
                                            "username": "johndoe",
                                            "email": "john@example.com",
                                            "userId": "507f1f77bcf86cd799439011"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "401",
                  description = "Invalid credentials",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Authentication Failed",
                                  value = """
                                          {
                                            "error": "Invalid credentials"
                                          }
                                          """
                          )
                  )
          )
  })
  @PostMapping("/login")
  public ResponseEntity<?> login(
          @Parameter(description = "User login credentials", required = true)
          @Valid @RequestBody LoginRequest request) {
    try {
      log.info("Login request received for: {}", request.getUsernameOrEmail());
      var response = authService.login(request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Login failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(createErrorResponse("Invalid credentials"));
    }
  }

  @Operation(
          summary = "Validate JWT token",
          description = "Validate the provided JWT token and return user information if valid."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "200",
                  description = "Token is valid",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Valid Token",
                                  value = """
                                          {
                                            "valid": true,
                                            "username": "johndoe"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "401",
                  description = "Invalid or expired token",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Invalid Token",
                                  value = """
                                          {
                                            "error": "Invalid token"
                                          }
                                          """
                          )
                  )
          )
  })
  @GetMapping("/validate")
  public ResponseEntity<?> validateToken(
          @Parameter(description = "JWT token in Authorization header", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
          @RequestHeader("Authorization") String authHeader) {
    try {
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        var token = authHeader.substring(7);
        var isValid = authService.validateToken(token);

        if (isValid) {
          var username = authService.extractUsername(token);
          var response = new HashMap<String, Object>();
          response.put("valid", true);
          response.put("username", username);
          return ResponseEntity.ok(response);
        }
      }
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(createErrorResponse("Invalid token"));
    } catch (Exception e) {
      log.error("Token validation error", e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(createErrorResponse("Invalid token"));
    }
  }

  private Map<String, String> createErrorResponse(String message) {
    var error = new HashMap<String, String>();
    error.put("error", message);
    return error;
  }
}
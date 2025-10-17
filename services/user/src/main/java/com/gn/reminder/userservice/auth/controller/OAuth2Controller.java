package com.gn.reminder.userservice.auth.controller;

import com.gn.reminder.userservice.auth.dto.AuthResponse;
import com.gn.reminder.userservice.auth.dto.OAuth2CallbackRequest;
import com.gn.reminder.userservice.auth.service.OAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for OAuth2/Social Login endpoints
 * Handles callbacks from Keycloak after social authentication
 */
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2 Authentication", description = "Social login endpoints for Google, GitHub, etc.")
public class OAuth2Controller {

  private final OAuth2Service oauth2Service;



  /**
   * Get OAuth2 providers configuration
   * Returns list of available social login providers
   */
  @Operation(
          summary = "Get OAuth2 providers",
          description = "Returns list of available social login providers and Keycloak authentication URL."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "200",
                  description = "List of available OAuth2 providers",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Available Providers",
                                  value = """
                                          {
                                            "providers": ["google", "github"],
                                            "keycloakAuthUrl": "http://localhost:9191/realms/realm-service/protocol/openid-connect/auth"
                                          }
                                          """
                          )
                  )
          )
  })
  @GetMapping("/providers")
  public ResponseEntity<?> getProviders() {
    return ResponseEntity.ok(new ProvidersResponse(
            new String[]{"google", "github"},
            "http://localhost:9191/realms/realm-service/protocol/openid-connect/auth"
    ));
  }

  /**
   * Handle OAuth2 callback with authorization code
   * Securely exchanges authorization code for tokens and processes user login
   */
  @Operation(
          summary = "Handle OAuth2 callback",
          description = "Exchanges authorization code for tokens and processes OAuth2 login securely on the backend."
  )
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = "200",
                  description = "OAuth2 login successful",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Successful OAuth2 Login",
                                  value = """
                                          {
                                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                            "userId": "507f1f77bcf86cd799439011",
                                            "username": "john.doe",
                                            "email": "john.doe@example.com"
                                          }
                                          """
                          )
                  )
          ),
          @ApiResponse(
                  responseCode = "400",
                  description = "Invalid authorization code or callback parameters",
                  content = @Content(
                          mediaType = "application/json",
                          examples = @ExampleObject(
                                  name = "Invalid Callback",
                                  value = """
                                          {
                                            "error": "Invalid authorization code or state parameter"
                                          }
                                          """
                          )
                  )
          )
  })
  @PostMapping("/callback")
  public ResponseEntity<AuthResponse> handleOAuth2Callback(
          @Parameter(description = "OAuth2 callback information with authorization code", required = true)
          @Valid @RequestBody OAuth2CallbackRequest request) {
    var response = oauth2Service.handleOAuth2Callback(request);
    return ResponseEntity.ok(response);
  }

  @lombok.Data
  @lombok.AllArgsConstructor
  private static class ProvidersResponse {
    private String[] providers;
    private String keycloakAuthUrl;
  }
}




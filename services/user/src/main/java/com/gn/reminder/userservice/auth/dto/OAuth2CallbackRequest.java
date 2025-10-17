package com.gn.reminder.userservice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for OAuth2 callback handling
 * Contains authorization code and related parameters from OAuth2 provider
 */
@Data
@Schema(description = "OAuth2 callback request containing authorization code and state")
public class OAuth2CallbackRequest {

    @NotBlank(message = "Authorization code is required")
    @Schema(description = "Authorization code received from OAuth2 provider", example = "abc123def456")
    private String code;

    @NotBlank(message = "State parameter is required")
    @Schema(description = "State parameter for CSRF protection", example = "google_1234567890")
    private String state;

    @NotBlank(message = "Provider is required")
    @Schema(description = "OAuth2 provider name", example = "google", allowableValues = {"google", "github"})
    private String provider;

    @NotBlank(message = "Redirect URI is required")
    @Schema(description = "Redirect URI used in the OAuth2 flow", example = "http://localhost:3000/auth/callback")
    private String redirectUri;
}

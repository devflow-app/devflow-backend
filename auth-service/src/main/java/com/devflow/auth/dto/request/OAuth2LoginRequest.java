package com.devflow.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuth2LoginRequest {

    @NotBlank(message = "Provider is required")
    private String provider; // "google" or "github"

    @NotBlank(message = "Token is required")
    private String token; // OAuth2 access/ID token from frontend
}

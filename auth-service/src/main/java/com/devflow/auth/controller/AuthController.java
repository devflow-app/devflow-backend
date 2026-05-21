package com.devflow.auth.controller;

import com.devflow.auth.dto.request.*;
import com.devflow.auth.dto.response.*;
import com.devflow.auth.entity.User;
import com.devflow.auth.service.AuthService;
import com.devflow.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, OAuth2, token refresh and 2FA management")
public class AuthController {

    private final AuthService authService;

    // ── Public endpoints ──────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/oauth2/login")
    @Operation(summary = "Login with Google or GitHub OAuth2 token")
    public ResponseEntity<ApiResponse<AuthResponse>> oauthLogin(
            @Valid @RequestBody OAuth2LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.loginWithOAuth(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // ── Protected endpoints ───────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(authService.mapToUserResponse(user)));
    }

    // ── 2FA endpoints ─────────────────────────────────────────

    @PostMapping("/2fa/setup")
    @Operation(summary = "Generate QR code to setup Google Authenticator",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<TotpSetupResponse>> setup2fa(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(authService.setupTotp(user.getId())));
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable 2FA by verifying TOTP code",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> enable2fa(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyTotpRequest request) {
        authService.enableTotp(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("2FA enabled successfully", null));
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Disable 2FA by verifying TOTP code",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> disable2fa(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyTotpRequest request) {
        authService.disableTotp(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("2FA disabled successfully", null));
    }
}

package com.devflow.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn; // seconds

    private UserResponse user;

    /** True when user has 2FA enabled but code not yet verified in this request */
    @Builder.Default
    private boolean totpRequired = false;
}

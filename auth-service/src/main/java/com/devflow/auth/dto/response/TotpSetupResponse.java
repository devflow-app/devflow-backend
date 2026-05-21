package com.devflow.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TotpSetupResponse {

    private String secret;       // Base32 secret to store manually
    private String qrCodeUrl;   // Data URI to render QR image in frontend
    private String otpAuthUrl;  // otpauth:// URI for authenticator apps
}

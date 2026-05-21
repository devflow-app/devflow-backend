package com.devflow.auth.dto.response;

import com.devflow.common.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String avatarUrl;
    private UserRole role;
    private boolean emailVerified;
    private boolean totpEnabled;
    private Instant createdAt;
}

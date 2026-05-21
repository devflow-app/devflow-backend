package com.devflow.project.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private UUID id;
    private String name;
    private String slug;
    private Instant createdAt;
    private Instant updatedAt;
}

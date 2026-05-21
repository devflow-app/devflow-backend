package com.devflow.project.dto.response;

import com.devflow.common.enums.ProjectStatus;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String key;
    private String description;
    private ProjectStatus status;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
}

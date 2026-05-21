package com.devflow.project.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTrailResponse {
    private UUID id;
    private String entityName;
    private UUID entityId;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private UUID actorId;
    private Instant createdAt;
}

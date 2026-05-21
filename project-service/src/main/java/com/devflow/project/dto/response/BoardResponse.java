package com.devflow.project.dto.response;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
}

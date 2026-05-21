package com.devflow.project.dto.response;

import com.devflow.project.entity.SprintStatus;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String goal;
    private Instant startDate;
    private Instant endDate;
    private SprintStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}

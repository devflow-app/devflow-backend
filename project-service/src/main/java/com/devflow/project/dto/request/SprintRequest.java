package com.devflow.project.dto.request;

import com.devflow.project.entity.SprintStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Sprint name is required")
    private String name;

    private String goal;

    private Instant startDate;

    private Instant endDate;

    private SprintStatus status;
}

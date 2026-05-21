package com.devflow.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintTaskDto {

    private UUID id;
    private String title;
    private String description;
    private Integer storyPoints;
    private Integer estimatedHours;
    private String status; // TODO, IN_PROGRESS, DONE, etc.
    private String priority; // LOW, MEDIUM, HIGH
    private String assigneeName; // Optional, to check balance
}

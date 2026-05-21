package com.devflow.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintEstimationRequestDto {

    @NotEmpty(message = "La lista de tareas no puede estar vacía")
    private List<SprintTaskDto> tasks;

    private Integer sprintDurationWeeks; // Default will be set if null (e.g. 2)
    private Integer teamCapacityHours; // Total capacity of the team in hours for this sprint
    private Integer teamVelocityStoryPoints; // Team velocity in story points (if using SPs)
}

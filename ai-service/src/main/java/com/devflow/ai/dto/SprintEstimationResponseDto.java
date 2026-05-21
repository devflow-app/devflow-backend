package com.devflow.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintEstimationResponseDto {

    private Integer totalTasks;
    private Integer totalEstimatedHours;
    private Integer totalStoryPoints;
    private String feasibility; // e.g. "FEASIBLE", "RISKY", "UNFEASIBLE", "OVERLOADED"
    private String summary; // Short description of feasibility
    private List<String> bottlenecks; // Potential bottlenecks (e.g. "User X has too many high-priority tasks")
    private List<String> recommendations; // Allocation or scoping recommendations
    private List<String> priorityOrder; // Suggested list of task titles ordered by priority/execution order
}

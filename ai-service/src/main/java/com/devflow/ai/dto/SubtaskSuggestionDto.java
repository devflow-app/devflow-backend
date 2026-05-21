package com.devflow.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskSuggestionDto {

    private String title;
    private String description;
    private Integer estimatedHours;
    private String priority; // LOW, MEDIUM, HIGH
}

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
public class SubtaskSuggestionResponseDto {

    private String originalTaskTitle;
    private List<SubtaskSuggestionDto> subtasks;
    private String rationale; // AI justification or tips for implementing the subtasks
}

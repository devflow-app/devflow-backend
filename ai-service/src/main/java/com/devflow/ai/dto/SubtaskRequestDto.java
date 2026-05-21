package com.devflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskRequestDto {

    @NotBlank(message = "El título de la tarea es requerido")
    private String title;

    private String description;
}

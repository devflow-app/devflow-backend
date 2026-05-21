package com.devflow.ai.controller;

import com.devflow.ai.dto.*;
import com.devflow.ai.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Controller", description = "Endpoints para la asistencia inteligente con IA de DevFlow (Sugerencia de subtareas y estimación de sprints)")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiService aiService;

    @PostMapping("/tasks/subtasks")
    @Operation(
            summary = "Sugerir subtareas lógicas",
            description = "Analiza el título y descripción de una tarea principal y genera una lista secuencial de subtareas estimadas y priorizadas.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Sugerencias generadas exitosamente",
                            content = @Content(schema = @Schema(implementation = SubtaskSuggestionResponseDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
                    @ApiResponse(responseCode = "401", description = "No autorizado"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
            }
    )
    public ResponseEntity<SubtaskSuggestionResponseDto> suggestSubtasks(
            @Valid @RequestBody SubtaskRequestDto request) {
        log.info("REST request to suggest subtasks for: {}", request.getTitle());
        SubtaskSuggestionResponseDto response = aiService.suggestSubtasks(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tasks/sprint-estimation")
    @Operation(
            summary = "Estimar viabilidad del sprint",
            description = "Analiza un conjunto de tareas y evalúa la viabilidad del sprint basado en la capacidad y velocidad del equipo.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Estimación completada exitosamente",
                            content = @Content(schema = @Schema(implementation = SprintEstimationResponseDto.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
                    @ApiResponse(responseCode = "401", description = "No autorizado"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
            }
    )
    public ResponseEntity<SprintEstimationResponseDto> estimateSprint(
            @Valid @RequestBody SprintEstimationRequestDto request) {
        log.info("REST request to estimate sprint feasibility with {} tasks", request.getTasks().size());
        SprintEstimationResponseDto response = aiService.estimateSprint(request);
        return ResponseEntity.ok(response);
    }
}

package com.devflow.ai.service;

import com.devflow.ai.client.OpenAiClient;
import com.devflow.ai.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final OpenAiClient openAiClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${devflow.ai.cache-ttl-minutes:60}")
    private Long cacheTtlMinutes;

    @Value("${devflow.ai.max-suggestions:5}")
    private Integer maxSuggestions;

    /**
     * Recommends subtasks for a given parent task using OpenAI and caching results in Redis.
     */
    public SubtaskSuggestionResponseDto suggestSubtasks(SubtaskRequestDto request) {
        String cacheKey = "ai:subtasks:" + generateSha256(request.getTitle() + "|" + Objects.toString(request.getDescription(), ""));
        
        try {
            // Check cache
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("Returning cached subtask suggestions for task: {}", request.getTitle());
                return objectMapper.convertValue(cachedData, SubtaskSuggestionResponseDto.class);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve from Redis cache: {}", e.getMessage());
        }

        log.info("No cache hit. Querying OpenAI for subtasks suggestion of: {}", request.getTitle());
        String systemPrompt = String.format("""
                Eres un asistente experto en ingeniería de software y metodologías ágiles (Scrum).
                Tu tarea es descomponer una tarea principal en una lista de hasta %d subtareas lógicas, secuenciales y bien definidas.
                Debes responder ÚNICAMENTE con un objeto JSON válido con la siguiente estructura exacta:
                {
                  "originalTaskTitle": "título de la tarea principal original",
                  "subtasks": [
                    {
                      "title": "título conciso de la subtarea",
                      "description": "descripción de lo que se debe hacer en esta subtarea",
                      "estimatedHours": 4,
                      "priority": "HIGH"
                    }
                  ],
                  "rationale": "justificación o notas breves sobre por qué se sugieren estas subtareas y cómo acometerlas"
                }
                No agregues explicaciones fuera del objeto JSON. No utilices bloques de código de markdown de triple comilla (como ```json) en tu respuesta. El JSON debe ser parseable directamente de forma programática.
                """, maxSuggestions);

        String userPrompt = String.format("Tarea Principal:\nTítulo: %s\nDescripción: %s", 
                request.getTitle(), Objects.toString(request.getDescription(), "Sin descripción"));

        String rawJson = openAiClient.generateStructuredResponse(systemPrompt, userPrompt);
        
        try {
            // Clean up backticks in case OpenAI didn't follow the instructions
            rawJson = cleanJsonString(rawJson);
            
            SubtaskSuggestionResponseDto response = objectMapper.readValue(rawJson, SubtaskSuggestionResponseDto.class);
            response.setOriginalTaskTitle(request.getTitle()); // Ensure correct mapping
            
            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, response, cacheTtlMinutes, TimeUnit.MINUTES);
                log.info("Cached subtask suggestions in Redis with key: {}", cacheKey);
            } catch (Exception cacheEx) {
                log.warn("Failed to write to Redis cache: {}", cacheEx.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response to DTO: {}. Raw response: {}", e.getMessage(), rawJson);
            throw new RuntimeException("Error al procesar la sugerencia de la IA. Por favor, intente de nuevo.", e);
        }
    }

    /**
     * Estimates sprint feasibility and resource allocation.
     */
    public SprintEstimationResponseDto estimateSprint(SprintEstimationRequestDto request) {
        // Build payload key based on tasks info to cache
        StringBuilder keyPayload = new StringBuilder();
        keyPayload.append("duration:").append(Objects.toString(request.getSprintDurationWeeks(), "2")).append("|");
        keyPayload.append("capacity:").append(Objects.toString(request.getTeamCapacityHours(), "0")).append("|");
        keyPayload.append("velocity:").append(Objects.toString(request.getTeamVelocityStoryPoints(), "0")).append("|");
        for (SprintTaskDto task : request.getTasks()) {
            keyPayload.append(task.getId()).append("-").append(task.getStatus()).append("-").append(task.getAssigneeName()).append(";");
        }
        
        String cacheKey = "ai:sprint-est:" + generateSha256(keyPayload.toString());

        try {
            // Check cache
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("Returning cached sprint estimation.");
                return objectMapper.convertValue(cachedData, SprintEstimationResponseDto.class);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve from Redis cache: {}", e.getMessage());
        }

        log.info("No cache hit. Querying OpenAI for sprint estimation.");
        String systemPrompt = """
                Eres un asistente experto en ingeniería de software y Scrum Master.
                Tu tarea es analizar un conjunto de tareas propuestas para un sprint y evaluar su viabilidad basándote en la duración del sprint, la capacidad total del equipo en horas, y la velocidad estimada en puntos de historia.
                Debes responder ÚNICAMENTE con un objeto JSON válido con la siguiente estructura exacta:
                {
                  "totalTasks": 10,
                  "totalEstimatedHours": 80,
                  "totalStoryPoints": 20,
                  "feasibility": "FEASIBLE",
                  "summary": "resumen breve de la viabilidad del sprint",
                  "bottlenecks": [
                    "lista de posibles cuellos de botella detectados (ej: sobreasignación a un desarrollador, dependencias complejas, falta de estimación en tareas críticas)"
                  ],
                  "recommendations": [
                    "lista de recomendaciones para equilibrar la carga, priorizar o cambiar el alcance"
                  ],
                  "priorityOrder": [
                    "lista de títulos de las tareas ordenadas lógicamente según su prioridad y dependencias recomendadas para ejecución"
                  ]
                }
                No agregues explicaciones fuera del objeto JSON. No utilices bloques de código de markdown de triple comilla (como ```json) en tu respuesta. El JSON debe ser parseable directamente de forma programática.
                """;

        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append(String.format("Detalles del Sprint:\nDuración: %d semanas\nCapacidad del equipo en horas: %d\nVelocidad en puntos de historia: %d\n\nTareas:\n",
                request.getSprintDurationWeeks() != null ? request.getSprintDurationWeeks() : 2,
                request.getTeamCapacityHours() != null ? request.getTeamCapacityHours() : 0,
                request.getTeamVelocityStoryPoints() != null ? request.getTeamVelocityStoryPoints() : 0));

        for (int i = 0; i < request.getTasks().size(); i++) {
            SprintTaskDto task = request.getTasks().get(i);
            userPromptBuilder.append(String.format("- Tarea %d:\n  Título: %s\n  Descripción: %s\n  Story Points: %d\n  Horas estimadas: %d\n  Estado: %s\n  Prioridad: %s\n  Asignado a: %s\n",
                    i + 1,
                    task.getTitle(),
                    Objects.toString(task.getDescription(), "Sin descripción"),
                    task.getStoryPoints() != null ? task.getStoryPoints() : 0,
                    task.getEstimatedHours() != null ? task.getEstimatedHours() : 0,
                    Objects.toString(task.getStatus(), "TODO"),
                    Objects.toString(task.getPriority(), "MEDIUM"),
                    Objects.toString(task.getAssigneeName(), "Sin asignar")));
        }

        String rawJson = openAiClient.generateStructuredResponse(systemPrompt, userPromptBuilder.toString());

        try {
            // Clean up backticks in case OpenAI didn't follow the instructions
            rawJson = cleanJsonString(rawJson);

            SprintEstimationResponseDto response = objectMapper.readValue(rawJson, SprintEstimationResponseDto.class);
            response.setTotalTasks(request.getTasks().size()); // Ensure correct count

            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, response, cacheTtlMinutes, TimeUnit.MINUTES);
                log.info("Cached sprint estimation in Redis with key: {}", cacheKey);
            } catch (Exception cacheEx) {
                log.warn("Failed to write to Redis cache: {}", cacheEx.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to parse OpenAI sprint response to DTO: {}. Raw response: {}", e.getMessage(), rawJson);
            throw new RuntimeException("Error al procesar la estimación del sprint con la IA. Por favor, intente de nuevo.", e);
        }
    }

    private String cleanJsonString(String rawJson) {
        if (rawJson == null) return "{}";
        rawJson = rawJson.trim();
        if (rawJson.startsWith("```json")) {
            rawJson = rawJson.substring(7);
        } else if (rawJson.startsWith("```")) {
            rawJson = rawJson.substring(3);
        }
        if (rawJson.endsWith("```")) {
            rawJson = rawJson.substring(0, rawJson.length() - 3);
        }
        return rawJson.trim();
    }

    private String generateSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            log.warn("SHA-256 hash generation failed, falling back to hashCode string: {}", ex.getMessage());
            return String.valueOf(data.hashCode());
        }
    }
}

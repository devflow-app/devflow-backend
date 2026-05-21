package com.devflow.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${devflow.openai.api-key}")
    private String apiKey;

    @Value("${devflow.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${devflow.openai.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${devflow.openai.temperature:0.7}")
    private Double temperature;

    @Value("${devflow.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    public String generateStructuredResponse(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("dummy-key")) {
            log.warn("OpenAI API Key is empty or dummy. Falling back to mock generator.");
            return generateMockResponse(systemPrompt, userPrompt);
        }

        try {
            String url = baseUrl + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Construct payload
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("response_format", Map.of("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            log.debug("Sending request to OpenAI API with model {}", model);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String content = choices.get(0).path("message").path("content").asText();
                    log.debug("OpenAI API call succeeded");
                    return content;
                }
            }
            throw new RuntimeException("Empty response or bad format from OpenAI. Status: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to generate structured response from OpenAI: {}. Falling back to mock response.", e.getMessage());
            return generateMockResponse(systemPrompt, userPrompt);
        }
    }

    private String generateMockResponse(String systemPrompt, String userPrompt) {
        log.info("Generating mock response because OpenAI client failed or was unconfigured.");
        // Determine whether it's subtask generation or sprint estimation based on system prompt
        if (systemPrompt.toLowerCase().contains("subtask") || userPrompt.toLowerCase().contains("subtask")) {
            return """
            {
              "originalTaskTitle": "Mock Task Title",
              "subtasks": [
                {
                  "title": "Configuración del entorno y dependencias",
                  "description": "Configurar las librerías necesarias, variables de entorno y base de datos local para el módulo.",
                  "estimatedHours": 4,
                  "priority": "HIGH"
                },
                {
                  "title": "Implementación del modelo de datos",
                  "description": "Crear entidades JPA, repositorios y scripts de migración correspondientes.",
                  "estimatedHours": 6,
                  "priority": "HIGH"
                },
                {
                  "title": "Creación del servicio de negocio",
                  "description": "Escribir la lógica de negocio principal y pruebas unitarias iniciales.",
                  "estimatedHours": 8,
                  "priority": "MEDIUM"
                },
                {
                  "title": "Exposición de endpoints REST",
                  "description": "Configurar el controlador REST, DTOs de entrada/salida y manejo de excepciones.",
                  "estimatedHours": 4,
                  "priority": "LOW"
                }
              ],
              "rationale": "Sugerencias de subtareas simuladas de forma estructurada para agilizar el desarrollo del módulo. Se asume un flujo estándar de desarrollo de Spring Boot."
            }
            """;
        } else {
            return """
            {
              "totalTasks": 3,
              "totalEstimatedHours": 22,
              "totalStoryPoints": 8,
              "feasibility": "FEASIBLE",
              "summary": "El sprint es altamente viable en base a la capacidad provista.",
              "bottlenecks": [
                "No se detectan cuellos de botella críticos con la asignación actual."
              ],
              "recommendations": [
                "Monitorear las tareas de mayor estimación durante la primera semana.",
                "Mantener reuniones diarias para detectar bloqueos tempranos."
              ],
              "priorityOrder": [
                "Configuración del entorno y dependencias",
                "Implementación del modelo de datos",
                "Creación del servicio de negocio"
              ]
            }
            """;
        }
    }
}

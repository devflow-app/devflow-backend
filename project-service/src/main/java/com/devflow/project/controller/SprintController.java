package com.devflow.project.controller;

import com.devflow.common.dto.ApiResponse;
import com.devflow.project.dto.request.SprintRequest;
import com.devflow.project.dto.response.SprintResponse;
import com.devflow.project.security.UserPrincipal;
import com.devflow.project.service.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sprints")
@RequiredArgsConstructor
@Tag(name = "Sprints", description = "Sprint management and lifecycle")
@SecurityRequirement(name = "Bearer Authentication")
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    @Operation(summary = "Create a new sprint")
    public ResponseEntity<ApiResponse<SprintResponse>> create(
            @Valid @RequestBody SprintRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(sprintService.createSprint(request, principal.getId())));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all sprints in a project")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> getByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.getSprintsByProject(projectId)));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a sprint (transition status to ACTIVE)")
    public ResponseEntity<ApiResponse<SprintResponse>> start(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.startSprint(id, principal.getId())));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a active sprint, moving unresolved tasks to a target sprint or backlog")
    public ResponseEntity<ApiResponse<SprintResponse>> complete(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID targetSprintId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(sprintService.completeSprint(id, targetSprintId, principal.getId())));
    }
}

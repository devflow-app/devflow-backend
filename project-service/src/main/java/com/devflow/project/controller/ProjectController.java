package com.devflow.project.controller;

import com.devflow.common.dto.ApiResponse;
import com.devflow.project.dto.request.ProjectRequest;
import com.devflow.project.dto.response.ProjectResponse;
import com.devflow.project.security.UserPrincipal;
import com.devflow.project.service.ProjectService;
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
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Project management")
@SecurityRequirement(name = "Bearer Authentication")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(projectService.createProject(request, principal.getId())));
    }

    @GetMapping("/org/{organizationId}")
    @Operation(summary = "Get all projects in an organization")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getByOrg(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.getProjectsByOrganization(organizationId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.getProjectById(id)));
    }

    @GetMapping("/key/{key}")
    @Operation(summary = "Get project by key")
    public ResponseEntity<ApiResponse<ProjectResponse>> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.getProjectByKey(key)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project details")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.updateProject(id, request, principal.getId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a project")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        projectService.deleteProject(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft deleted project")
    public ResponseEntity<ApiResponse<ProjectResponse>> restore(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(projectService.restoreProject(id, principal.getId())));
    }
}

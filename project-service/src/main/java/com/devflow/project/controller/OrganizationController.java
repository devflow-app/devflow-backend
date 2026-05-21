package com.devflow.project.controller;

import com.devflow.common.dto.ApiResponse;
import com.devflow.project.dto.request.OrganizationRequest;
import com.devflow.project.dto.response.OrganizationResponse;
import com.devflow.project.security.UserPrincipal;
import com.devflow.project.service.OrganizationService;
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
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Multi-tenant Organization management")
@SecurityRequirement(name = "Bearer Authentication")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @Operation(summary = "Create a new organization")
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(organizationService.createOrganization(request, principal.getId())));
    }

    @GetMapping
    @Operation(summary = "Get all active organizations")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getAllOrganizations()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getOrganizationById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get organization by slug")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getOrganizationBySlug(slug)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an organization")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.updateOrganization(id, request, principal.getId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete an organization")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        organizationService.deleteOrganization(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft deleted organization")
    public ResponseEntity<ApiResponse<OrganizationResponse>> restore(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.restoreOrganization(id, principal.getId())));
    }
}

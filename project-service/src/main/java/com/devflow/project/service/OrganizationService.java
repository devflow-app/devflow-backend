package com.devflow.project.service;

import com.devflow.common.exception.DevFlowException;
import com.devflow.project.dto.request.OrganizationRequest;
import com.devflow.project.dto.response.OrganizationResponse;
import com.devflow.project.entity.Organization;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final ProjectMapper projectMapper;
    private final AuditTrailService auditTrailService;

    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request, UUID actorId) {
        if (organizationRepository.existsBySlugAndDeletedAtIsNull(request.getSlug())) {
            throw new DevFlowException("Organization slug already exists", HttpStatus.CONFLICT, "SLUG_ALREADY_EXISTS");
        }

        Organization org = Organization.builder()
                .name(request.getName().trim())
                .slug(request.getSlug().toLowerCase().trim())
                .build();

        org = organizationRepository.save(org);
        auditTrailService.logChange("Organization", org.getId(), "CREATE", null, null, org.getName(), actorId);

        log.info("Organization created: {} ({})", org.getName(), org.getSlug());
        return projectMapper.toResponse(org);
    }

    public List<OrganizationResponse> getAllOrganizations() {
        return organizationRepository.findAllByDeletedAtIsNull().stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    public OrganizationResponse getOrganizationById(UUID id) {
        Organization org = organizationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Organization not found", HttpStatus.NOT_FOUND));
        return projectMapper.toResponse(org);
    }

    public OrganizationResponse getOrganizationBySlug(String slug) {
        Organization org = organizationRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new DevFlowException("Organization not found", HttpStatus.NOT_FOUND));
        return projectMapper.toResponse(org);
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID id, OrganizationRequest request, UUID actorId) {
        Organization org = organizationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Organization not found", HttpStatus.NOT_FOUND));

        if (!org.getSlug().equals(request.getSlug()) && organizationRepository.existsBySlugAndDeletedAtIsNull(request.getSlug())) {
            throw new DevFlowException("Organization slug already exists", HttpStatus.CONFLICT, "SLUG_ALREADY_EXISTS");
        }

        String oldName = org.getName();
        String oldSlug = org.getSlug();

        org.setName(request.getName().trim());
        org.setSlug(request.getSlug().toLowerCase().trim());
        org = organizationRepository.save(org);

        if (!oldName.equals(org.getName())) {
            auditTrailService.logChange("Organization", org.getId(), "UPDATE", "name", oldName, org.getName(), actorId);
        }
        if (!oldSlug.equals(org.getSlug())) {
            auditTrailService.logChange("Organization", org.getId(), "UPDATE", "slug", oldSlug, org.getSlug(), actorId);
        }

        return projectMapper.toResponse(org);
    }

    @Transactional
    public void deleteOrganization(UUID id, UUID actorId) {
        Organization org = organizationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Organization not found", HttpStatus.NOT_FOUND));

        org.setDeletedAt(Instant.now());
        organizationRepository.save(org);
        auditTrailService.logChange("Organization", org.getId(), "DELETE", "deletedAt", null, org.getDeletedAt().toString(), actorId);
        log.info("Organization soft deleted: {}", org.getName());
    }

    @Transactional
    public OrganizationResponse restoreOrganization(UUID id, UUID actorId) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new DevFlowException("Organization not found", HttpStatus.NOT_FOUND));

        if (org.getDeletedAt() == null) {
            throw new DevFlowException("Organization is not deleted", HttpStatus.BAD_REQUEST);
        }

        org.setDeletedAt(null);
        org = organizationRepository.save(org);
        auditTrailService.logChange("Organization", org.getId(), "RESTORE", "deletedAt", null, null, actorId);
        log.info("Organization restored: {}", org.getName());
        return projectMapper.toResponse(org);
    }
}

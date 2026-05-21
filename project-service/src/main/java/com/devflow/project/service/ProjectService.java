package com.devflow.project.service;

import com.devflow.common.enums.ProjectStatus;
import com.devflow.common.exception.DevFlowException;
import com.devflow.project.dto.request.ProjectRequest;
import com.devflow.project.dto.response.ProjectResponse;
import com.devflow.project.entity.Board;
import com.devflow.project.entity.Organization;
import com.devflow.project.entity.Project;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.BoardRepository;
import com.devflow.project.repository.OrganizationRepository;
import com.devflow.project.repository.ProjectRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final BoardRepository boardRepository;
    private final ProjectMapper projectMapper;
    private final AuditTrailService auditTrailService;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, UUID ownerId) {
        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(request.getOrganizationId())
                .orElseThrow(() -> new DevFlowException("Organization not found", HttpStatus.NOT_FOUND));

        String key = request.getKey().toUpperCase().trim();
        if (projectRepository.existsByKeyAndDeletedAtIsNull(key)) {
            throw new DevFlowException("Project key already exists", HttpStatus.CONFLICT, "KEY_ALREADY_EXISTS");
        }

        Project project = Project.builder()
                .organization(organization)
                .name(request.getName().trim())
                .key(key)
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : ProjectStatus.ACTIVE)
                .ownerId(ownerId)
                .build();

        project = projectRepository.save(project);
        auditTrailService.logChange("Project", project.getId(), "CREATE", null, null, project.getName(), ownerId);

        // Auto-create a default board for the project
        Board defaultBoard = Board.builder()
                .project(project)
                .name("Kanban Board")
                .build();
        boardRepository.save(defaultBoard);

        log.info("Project created: {} ({}) with default Kanban board", project.getName(), project.getKey());
        return projectMapper.toResponse(project);
    }

    public List<ProjectResponse> getProjectsByOrganization(UUID organizationId) {
        return projectRepository.findAllByOrganizationIdAndDeletedAtIsNull(organizationId).stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(UUID id) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Project not found", HttpStatus.NOT_FOUND));
        return projectMapper.toResponse(project);
    }

    public ProjectResponse getProjectByKey(String key) {
        Project project = projectRepository.findByKeyAndDeletedAtIsNull(key.toUpperCase())
                .orElseThrow(() -> new DevFlowException("Project not found", HttpStatus.NOT_FOUND));
        return projectMapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID id, ProjectRequest request, UUID actorId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Project not found", HttpStatus.NOT_FOUND));

        String oldName = project.getName();
        ProjectStatus oldStatus = project.getStatus();

        project.setName(request.getName().trim());
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        project.setDescription(request.getDescription());
        project = projectRepository.save(project);

        if (!oldName.equals(project.getName())) {
            auditTrailService.logChange("Project", project.getId(), "UPDATE", "name", oldName, project.getName(), actorId);
        }
        if (oldStatus != project.getStatus()) {
            auditTrailService.logChange("Project", project.getId(), "UPDATE", "status", oldStatus.name(), project.getStatus().name(), actorId);
        }

        return projectMapper.toResponse(project);
    }

    @Transactional
    public void deleteProject(UUID id, UUID actorId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Project not found", HttpStatus.NOT_FOUND));

        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
        auditTrailService.logChange("Project", project.getId(), "DELETE", "deletedAt", null, project.getDeletedAt().toString(), actorId);
        log.info("Project soft deleted: {}", project.getName());
    }

    @Transactional
    public ProjectResponse restoreProject(UUID id, UUID actorId) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new DevFlowException("Project not found", HttpStatus.NOT_FOUND));

        if (project.getDeletedAt() == null) {
            throw new DevFlowException("Project is not deleted", HttpStatus.BAD_REQUEST);
        }

        project.setDeletedAt(null);
        project = projectRepository.save(project);
        auditTrailService.logChange("Project", project.getId(), "RESTORE", "deletedAt", null, null, actorId);
        log.info("Project restored: {}", project.getName());
        return projectMapper.toResponse(project);
    }
}

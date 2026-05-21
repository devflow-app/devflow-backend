package com.devflow.project.service;

import com.devflow.common.enums.TaskStatus;
import com.devflow.common.exception.DevFlowException;
import com.devflow.project.dto.request.SprintRequest;
import com.devflow.project.dto.response.SprintResponse;
import com.devflow.project.entity.Project;
import com.devflow.project.entity.Sprint;
import com.devflow.project.entity.SprintStatus;
import com.devflow.project.entity.Task;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.project.repository.SprintRepository;
import com.devflow.project.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;
    private final AuditTrailService auditTrailService;

    @Transactional
    public SprintResponse createSprint(SprintRequest request, UUID actorId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.getProjectId())
                .orElseThrow(() -> new DevFlowException("Project not found", HttpStatus.NOT_FOUND));

        Sprint sprint = Sprint.builder()
                .project(project)
                .name(request.getName().trim())
                .goal(request.getGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus() != null ? request.getStatus() : SprintStatus.PLANNED)
                .build();

        sprint = sprintRepository.save(sprint);
        auditTrailService.logChange("Sprint", sprint.getId(), "CREATE", null, null, sprint.getName(), actorId);

        log.info("Sprint created: {} for project {}", sprint.getName(), project.getName());
        return projectMapper.toResponse(sprint);
    }

    public List<SprintResponse> getSprintsByProject(UUID projectId) {
        return sprintRepository.findAllByProjectId(projectId).stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SprintResponse startSprint(UUID sprintId, UUID actorId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new DevFlowException("Sprint not found", HttpStatus.NOT_FOUND));

        if (sprint.getStatus() != SprintStatus.PLANNED) {
            throw new DevFlowException("Sprint is already started or completed", HttpStatus.BAD_REQUEST);
        }

        // Check if there's already an active sprint for this project
        List<Sprint> activeSprints = sprintRepository.findAllByProjectIdAndStatus(sprint.getProject().getId(), SprintStatus.ACTIVE);
        if (!activeSprints.isEmpty()) {
            throw new DevFlowException("There is already an active sprint for this project. Complete it first.", HttpStatus.CONFLICT);
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        sprint = sprintRepository.save(sprint);
        auditTrailService.logChange("Sprint", sprint.getId(), "UPDATE", "status", SprintStatus.PLANNED.name(), SprintStatus.ACTIVE.name(), actorId);

        log.info("Sprint started: {}", sprint.getName());
        return projectMapper.toResponse(sprint);
    }

    @Transactional
    public SprintResponse completeSprint(UUID sprintId, UUID targetSprintId, UUID actorId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new DevFlowException("Sprint not found", HttpStatus.NOT_FOUND));

        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new DevFlowException("Sprint is not active", HttpStatus.BAD_REQUEST);
        }

        sprint.setStatus(SprintStatus.COMPLETED);
        sprint = sprintRepository.save(sprint);
        auditTrailService.logChange("Sprint", sprint.getId(), "UPDATE", "status", SprintStatus.ACTIVE.name(), SprintStatus.COMPLETED.name(), actorId);

        // Move unresolved tasks (not in DONE status)
        List<Task> tasks = taskRepository.findAllBySprintIdAndDeletedAtIsNull(sprintId);
        Sprint targetSprint = null;
        if (targetSprintId != null) {
            targetSprint = sprintRepository.findById(targetSprintId)
                    .orElseThrow(() -> new DevFlowException("Target sprint not found", HttpStatus.NOT_FOUND));
        }

        for (Task task : tasks) {
            if (task.getStatus() != TaskStatus.DONE) {
                Sprint oldSprint = task.getSprint();
                task.setSprint(targetSprint); // If targetSprint is null, it goes to Backlog
                taskRepository.save(task);

                String oldSprintName = oldSprint != null ? oldSprint.getName() : "Backlog";
                String newSprintName = targetSprint != null ? targetSprint.getName() : "Backlog";
                auditTrailService.logChange("Task", task.getId(), "MOVE_SPRINT", "sprintId", oldSprintName, newSprintName, actorId);
            }
        }

        log.info("Sprint completed: {}", sprint.getName());
        return projectMapper.toResponse(sprint);
    }
}

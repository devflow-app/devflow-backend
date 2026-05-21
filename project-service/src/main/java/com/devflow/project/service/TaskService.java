package com.devflow.project.service;

import com.devflow.common.enums.TaskPriority;
import com.devflow.common.enums.TaskStatus;
import com.devflow.common.event.TaskAssignedEvent;
import com.devflow.common.event.TaskCreatedEvent;
import com.devflow.common.exception.DevFlowException;
import com.devflow.project.document.TaskDocument;
import com.devflow.project.dto.request.TaskRequest;
import com.devflow.project.dto.response.TaskResponse;
import com.devflow.project.entity.Board;
import com.devflow.project.entity.Project;
import com.devflow.project.entity.Sprint;
import com.devflow.project.entity.Task;
import com.devflow.project.kafka.ProjectEventProducer;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.BoardRepository;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.project.repository.SprintRepository;
import com.devflow.project.repository.TaskRepository;
import com.devflow.project.repository.search.TaskSearchRepository;
import com.devflow.project.repository.spec.TaskSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final BoardRepository boardRepository;
    private final SprintRepository sprintRepository;
    private final TaskSearchRepository taskSearchRepository;
    private final ProjectEventProducer eventProducer;
    private final ProjectMapper projectMapper;
    private final AuditTrailService auditTrailService;

    @Transactional
    public TaskResponse createTask(TaskRequest request, UUID reporterId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.getProjectId())
                .orElseThrow(() -> new DevFlowException("Project not found", HttpStatus.NOT_FOUND));

        Board board = null;
        if (request.getBoardId() != null) {
            board = boardRepository.findById(request.getBoardId())
                    .orElseThrow(() -> new DevFlowException("Board not found", HttpStatus.NOT_FOUND));
        }

        Sprint sprint = null;
        if (request.getSprintId() != null) {
            sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new DevFlowException("Sprint not found", HttpStatus.NOT_FOUND));
        }

        Task parent = null;
        if (request.getParentId() != null) {
            parent = taskRepository.findByIdAndDeletedAtIsNull(request.getParentId())
                    .orElseThrow(() -> new DevFlowException("Parent task not found", HttpStatus.NOT_FOUND));
        }

        // Generate task key (e.g. PRJ-101)
        long taskCount = taskRepository.countByProjectId(project.getId());
        String taskKey = project.getKey().toUpperCase() + "-" + (taskCount + 1);

        Task task = Task.builder()
                .project(project)
                .board(board)
                .sprint(sprint)
                .parent(parent)
                .key(taskKey)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .storyPoints(request.getStoryPoints())
                .assigneeId(request.getAssigneeId())
                .reporterId(reporterId)
                .dueDate(request.getDueDate())
                .build();

        task = taskRepository.save(task);

        // Audit Trail
        auditTrailService.logChange("Task", task.getId(), "CREATE", null, null, task.getTitle(), reporterId);

        // Elasticsearch indexing (graceful fail)
        indexTaskInElasticsearch(task);

        // Kafka event
        eventProducer.publishTaskCreated(TaskCreatedEvent.builder()
                .taskId(task.getId().toString())
                .taskTitle(task.getTitle())
                .taskDescription(task.getDescription())
                .projectId(project.getId().toString())
                .projectName(project.getName())
                .createdByUserId(reporterId.toString())
                .assigneeUserId(task.getAssigneeId() != null ? task.getAssigneeId().toString() : null)
                .status(task.getStatus())
                .priority(task.getPriority())
                .build());

        log.info("Task created: {} - {}", task.getKey(), task.getTitle());
        return projectMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(UUID id, TaskRequest request, UUID actorId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Task not found", HttpStatus.NOT_FOUND));

        // Tracks fields that change to log changes
        String oldTitle = task.getTitle();
        String oldDesc = task.getDescription();
        TaskStatus oldStatus = task.getStatus();
        TaskPriority oldPriority = task.getPriority();
        Integer oldStoryPoints = task.getStoryPoints();
        UUID oldAssignee = task.getAssigneeId();
        Instant oldDueDate = task.getDueDate();
        Sprint oldSprint = task.getSprint();
        Board oldBoard = task.getBoard();

        // Update fields if changed
        if (request.getTitle() != null) task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        task.setStoryPoints(request.getStoryPoints());
        task.setDueDate(request.getDueDate());

        // Update Board & Sprint reference
        if (request.getBoardId() != null) {
            Board board = boardRepository.findById(request.getBoardId())
                    .orElseThrow(() -> new DevFlowException("Board not found", HttpStatus.NOT_FOUND));
            task.setBoard(board);
        } else if (request.getBoardId() == null) {
            task.setBoard(null);
        }

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new DevFlowException("Sprint not found", HttpStatus.NOT_FOUND));
            task.setSprint(sprint);
        } else if (request.getSprintId() == null) {
            task.setSprint(null);
        }

        // Set Assignee
        task.setAssigneeId(request.getAssigneeId());

        task = taskRepository.save(task);

        // Audit Trail comparison and logging
        logAuditTrailChanges(task, oldTitle, oldDesc, oldStatus, oldPriority, oldStoryPoints, oldAssignee, oldDueDate, oldSprint, oldBoard, actorId);

        // Publish assignment change to Kafka
        if (!Objects.equals(oldAssignee, task.getAssigneeId()) && task.getAssigneeId() != null) {
            eventProducer.publishTaskAssigned(TaskAssignedEvent.builder()
                    .taskId(task.getId().toString())
                    .taskTitle(task.getTitle())
                    .projectId(task.getProject().getId().toString())
                    .projectName(task.getProject().getName())
                    .assignedByUserId(actorId.toString())
                    .assignedByUserName("DevFlow User") // Mock or retrieve from context if possible
                    .assigneeUserId(task.getAssigneeId().toString())
                    .assigneeEmail("member@devflow.com") // Dummy email, notification-service can resolve if needed
                    .build());
        }

        // Elasticsearch sync
        indexTaskInElasticsearch(task);

        return projectMapper.toResponse(task);
    }

    public List<TaskResponse> getTasks(UUID projectId, UUID boardId, UUID sprintId, TaskStatus status, TaskPriority priority, UUID assigneeId) {
        Specification<Task> spec = Specification.where(TaskSpecifications.isNotDeleted());

        if (projectId != null) spec = spec.and(TaskSpecifications.byProjectId(projectId));
        if (boardId != null) spec = spec.and(TaskSpecifications.byBoardId(boardId));
        if (sprintId != null) spec = spec.and(TaskSpecifications.bySprintId(sprintId));
        if (status != null) spec = spec.and(TaskSpecifications.byStatus(status));
        if (priority != null) spec = spec.and(TaskSpecifications.byPriority(priority));
        if (assigneeId != null) spec = spec.and(TaskSpecifications.byAssigneeId(assigneeId));

        return taskRepository.findAll(spec).stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> searchTasks(String query, UUID projectId) {
        List<TaskDocument> docs = taskSearchRepository.findByTitleContainingOrDescriptionContainingAndProjectId(query, query, projectId.toString());
        return docs.stream()
                .map(doc -> {
                    // Try to resolve full entity from JPA, fall back to mock TaskResponse
                    return taskRepository.findByIdAndDeletedAtIsNull(UUID.fromString(doc.getId()))
                            .map(projectMapper::toResponse)
                            .orElse(TaskResponse.builder()
                                    .id(UUID.fromString(doc.getId()))
                                    .projectId(UUID.fromString(doc.getProjectId()))
                                    .key(doc.getKey())
                                    .title(doc.getTitle())
                                    .description(doc.getDescription())
                                    .status(TaskStatus.valueOf(doc.getStatus()))
                                    .priority(TaskPriority.valueOf(doc.getPriority()))
                                    .assigneeId(doc.getAssigneeId() != null ? UUID.fromString(doc.getAssigneeId()) : null)
                                    .build());
                })
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(UUID id) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Task not found", HttpStatus.NOT_FOUND));
        return projectMapper.toResponse(task);
    }

    public TaskResponse getTaskByKey(String key) {
        Task task = taskRepository.findByKeyAndDeletedAtIsNull(key.toUpperCase())
                .orElseThrow(() -> new DevFlowException("Task not found", HttpStatus.NOT_FOUND));
        return projectMapper.toResponse(task);
    }

    @Transactional
    public void deleteTask(UUID id, UUID actorId) {
        Task task = taskRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new DevFlowException("Task not found", HttpStatus.NOT_FOUND));

        task.setDeletedAt(Instant.now());
        taskRepository.save(task);

        auditTrailService.logChange("Task", task.getId(), "DELETE", "deletedAt", null, task.getDeletedAt().toString(), actorId);

        // De-index from Elasticsearch
        deindexTaskInElasticsearch(id);
    }

    @Transactional
    public TaskResponse restoreTask(UUID id, UUID actorId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new DevFlowException("Task not found", HttpStatus.NOT_FOUND));

        if (task.getDeletedAt() == null) {
            throw new DevFlowException("Task is not deleted", HttpStatus.BAD_REQUEST);
        }

        task.setDeletedAt(null);
        task = taskRepository.save(task);

        auditTrailService.logChange("Task", task.getId(), "RESTORE", "deletedAt", null, null, actorId);

        // Re-index to Elasticsearch
        indexTaskInElasticsearch(task);

        return projectMapper.toResponse(task);
    }

    // ── Helper Methods ────────────────────────────────────────

    private void indexTaskInElasticsearch(Task task) {
        try {
            TaskDocument doc = TaskDocument.builder()
                    .id(task.getId().toString())
                    .projectId(task.getProject().getId().toString())
                    .projectKey(task.getProject().getKey())
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .status(task.getStatus().name())
                    .priority(task.getPriority().name())
                    .assigneeId(task.getAssigneeId() != null ? task.getAssigneeId().toString() : null)
                    .key(task.getKey())
                    .build();
            taskSearchRepository.save(doc);
            log.info("Indexed task {} in Elasticsearch", task.getKey());
        } catch (Exception e) {
            log.error("Failed to index task {} in Elasticsearch: {}", task.getKey(), e.getMessage());
        }
    }

    private void deindexTaskInElasticsearch(UUID taskId) {
        try {
            taskSearchRepository.deleteById(taskId.toString());
            log.info("Deleted task {} from Elasticsearch index", taskId);
        } catch (Exception e) {
            log.error("Failed to delete task {} from Elasticsearch index: {}", taskId, e.getMessage());
        }
    }

    private void logAuditTrailChanges(Task task, String oldTitle, String oldDesc, TaskStatus oldStatus, TaskPriority oldPriority,
                                      Integer oldStoryPoints, UUID oldAssignee, Instant oldDueDate, Sprint oldSprint, Board oldBoard, UUID actorId) {
        UUID taskId = task.getId();
        if (!Objects.equals(oldTitle, task.getTitle())) {
            auditTrailService.logChange("Task", taskId, "UPDATE", "title", oldTitle, task.getTitle(), actorId);
        }
        if (!Objects.equals(oldDesc, task.getDescription())) {
            auditTrailService.logChange("Task", taskId, "UPDATE", "description", oldDesc, task.getDescription(), actorId);
        }
        if (oldStatus != task.getStatus()) {
            auditTrailService.logChange("Task", taskId, "UPDATE", "status", oldStatus.name(), task.getStatus().name(), actorId);
        }
        if (oldPriority != task.getPriority()) {
            auditTrailService.logChange("Task", taskId, "UPDATE", "priority", oldPriority.name(), task.getPriority().name(), actorId);
        }
        if (!Objects.equals(oldStoryPoints, task.getStoryPoints())) {
            auditTrailService.logChange("Task", taskId, "UPDATE", "storyPoints", String.valueOf(oldStoryPoints), String.valueOf(task.getStoryPoints()), actorId);
        }
        if (!Objects.equals(oldAssignee, task.getAssigneeId())) {
            auditTrailService.logChange("Task", taskId, "UPDATE", "assigneeId", String.valueOf(oldAssignee), String.valueOf(task.getAssigneeId()), actorId);
        }
        if (!Objects.equals(oldDueDate, task.getDueDate())) {
            auditTrailService.logChange("Task", taskId, "UPDATE", "dueDate", String.valueOf(oldDueDate), String.valueOf(task.getDueDate()), actorId);
        }
        if (!Objects.equals(oldSprint, task.getSprint())) {
            String osName = oldSprint != null ? oldSprint.getName() : "None";
            String nsName = task.getSprint() != null ? task.getSprint().getName() : "None";
            auditTrailService.logChange("Task", taskId, "UPDATE", "sprintId", osName, nsName, actorId);
        }
        if (!Objects.equals(oldBoard, task.getBoard())) {
            String obName = oldBoard != null ? oldBoard.getName() : "None";
            String nbName = task.getBoard() != null ? task.getBoard().getName() : "None";
            auditTrailService.logChange("Task", taskId, "UPDATE", "boardId", obName, nbName, actorId);
        }
    }
}

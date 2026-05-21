package com.devflow.project.controller;

import com.devflow.common.dto.ApiResponse;
import com.devflow.common.enums.TaskPriority;
import com.devflow.common.enums.TaskStatus;
import com.devflow.project.dto.request.TaskRequest;
import com.devflow.project.dto.response.TaskResponse;
import com.devflow.project.entity.Task;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.TaskRepository;
import com.devflow.project.security.UserPrincipal;
import com.devflow.project.service.ExportService;
import com.devflow.project.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task tracking, full-text search, exports and QR code management")
@SecurityRequirement(name = "Bearer Authentication")
public class TaskController {

    private final TaskService taskService;
    private final ExportService exportService;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;

    @Value("${devflow.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @PostMapping
    @Operation(summary = "Create a new task or subtask")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(taskService.createTask(request, principal.getId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task details")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.updateTask(id, request, principal.getId())));
    }

    @GetMapping
    @Operation(summary = "Get tasks with filters")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID boardId,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getTasks(projectId, boardId, sprintId, status, priority, assigneeId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getTaskById(id)));
    }

    @GetMapping("/key/{key}")
    @Operation(summary = "Get task by key (e.g. DEV-1)")
    public ResponseEntity<ApiResponse<TaskResponse>> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getTaskByKey(key)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a task")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        taskService.deleteTask(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft deleted task")
    public ResponseEntity<ApiResponse<TaskResponse>> restore(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.restoreTask(id, principal.getId())));
    }

    // ── Elasticsearch Search ──────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Full-text search tasks using Elasticsearch")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> search(
            @RequestParam String q,
            @RequestParam UUID projectId) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.searchTasks(q, projectId)));
    }

    // ── QR Code ───────────────────────────────────────────────

    @GetMapping("/{id}/qrcode")
    @Operation(summary = "Get shareable QR code for a task as Data URI")
    public ResponseEntity<ApiResponse<String>> getQrCode(@PathVariable UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        String qrCodeDataUri = exportService.generateTaskQrCodeDataUri(task, appBaseUrl);
        return ResponseEntity.ok(ApiResponse.ok("QR code generated", qrCodeDataUri));
    }

    // ── Document Export ───────────────────────────────────────

    @GetMapping("/export/pdf")
    @Operation(summary = "Export project tasks as PDF download")
    public ResponseEntity<byte[]> exportPdf(@RequestParam UUID projectId) {
        List<Task> tasks = taskRepository.findAllByProjectIdAndDeletedAtIsNull(projectId);
        String projectName = tasks.isEmpty() ? "Project" : tasks.get(0).getProject().getName();
        byte[] pdfBytes = exportService.exportTasksToPdf(projectName, tasks);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tasks-" + projectId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export project tasks as Excel spreadsheet download")
    public ResponseEntity<byte[]> exportExcel(@RequestParam UUID projectId) {
        List<Task> tasks = taskRepository.findAllByProjectIdAndDeletedAtIsNull(projectId);
        byte[] excelBytes = exportService.exportTasksToExcel(tasks);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tasks-" + projectId + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}

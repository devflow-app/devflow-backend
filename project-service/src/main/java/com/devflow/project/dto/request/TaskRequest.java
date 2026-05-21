package com.devflow.project.dto.request;

import com.devflow.common.enums.TaskPriority;
import com.devflow.common.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID boardId;

    private UUID sprintId;

    private UUID parentId;

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private Integer storyPoints;

    private UUID assigneeId;

    private Instant dueDate;
}

package com.devflow.project.dto.response;

import com.devflow.common.enums.TaskPriority;
import com.devflow.common.enums.TaskStatus;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private UUID id;
    private UUID projectId;
    private UUID boardId;
    private UUID sprintId;
    private UUID parentId;
    private String key;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Integer storyPoints;
    private UUID assigneeId;
    private UUID reporterId;
    private Instant dueDate;
    private Instant createdAt;
    private Instant updatedAt;
}

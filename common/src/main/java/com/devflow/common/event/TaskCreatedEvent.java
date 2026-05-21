package com.devflow.common.event;

import com.devflow.common.enums.TaskPriority;
import com.devflow.common.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to Kafka topic: devflow.task.created
 * Consumed by: notification-service, ai-service (to generate subtask suggestions)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreatedEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private String taskId;
    private String taskTitle;
    private String taskDescription;
    private String projectId;
    private String projectName;
    private String createdByUserId;
    private String assigneeUserId;
    private TaskStatus status;
    private TaskPriority priority;
}

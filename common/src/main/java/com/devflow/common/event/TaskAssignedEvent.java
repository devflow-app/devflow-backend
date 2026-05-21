package com.devflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to Kafka topic: devflow.task.assigned
 * Consumed by: notification-service (notify assignee)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignedEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private Instant occurredAt = Instant.now();

    private String taskId;
    private String taskTitle;
    private String projectId;
    private String projectName;
    private String assignedByUserId;
    private String assignedByUserName;
    private String assigneeUserId;
    private String assigneeEmail;
}

package com.devflow.project.repository.spec;

import com.devflow.common.enums.TaskPriority;
import com.devflow.common.enums.TaskStatus;
import com.devflow.project.entity.Task;
import org.springframework.data.jpa.domain.Specification;
import java.util.UUID;

public class TaskSpecifications {

    public static Specification<Task> byProjectId(UUID projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> byBoardId(UUID boardId) {
        return (root, query, cb) -> cb.equal(root.get("board").get("id"), boardId);
    }

    public static Specification<Task> bySprintId(UUID sprintId) {
        return (root, query, cb) -> cb.equal(root.get("sprint").get("id"), sprintId);
    }

    public static Specification<Task> byStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> byPriority(TaskPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> byAssigneeId(UUID assigneeId) {
        return (root, query, cb) -> cb.equal(root.get("assigneeId"), assigneeId);
    }

    public static Specification<Task> byReporterId(UUID reporterId) {
        return (root, query, cb) -> cb.equal(root.get("reporterId"), reporterId);
    }

    public static Specification<Task> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}

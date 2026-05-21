package com.devflow.project.mapper;

import com.devflow.project.dto.response.*;
import com.devflow.project.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    OrganizationResponse toResponse(Organization organization);

    @Mapping(source = "organization.id", target = "organizationId")
    ProjectResponse toResponse(Project project);

    @Mapping(source = "project.id", target = "projectId")
    BoardResponse toResponse(Board board);

    @Mapping(source = "project.id", target = "projectId")
    SprintResponse toResponse(Sprint sprint);

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "board.id", target = "boardId")
    @Mapping(source = "sprint.id", target = "sprintId")
    @Mapping(source = "parent.id", target = "parentId")
    TaskResponse toResponse(Task task);

    @Mapping(source = "task.id", target = "taskId")
    CommentResponse toResponse(Comment comment);

    AuditTrailResponse toResponse(AuditTrail auditTrail);
}

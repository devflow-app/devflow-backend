package com.devflow.project.repository;

import com.devflow.project.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    List<Task> findAllByProjectIdAndDeletedAtIsNull(UUID projectId);
    List<Task> findAllBySprintIdAndDeletedAtIsNull(UUID sprintId);
    List<Task> findAllByBoardIdAndDeletedAtIsNull(UUID boardId);
    Optional<Task> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Task> findByKeyAndDeletedAtIsNull(String key);
    long countByProjectId(UUID projectId);
    
    // For subtasks
    List<Task> findAllByParentIdAndDeletedAtIsNull(UUID parentId);
}

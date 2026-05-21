package com.devflow.project.repository;

import com.devflow.project.entity.Sprint;
import com.devflow.project.entity.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {
    List<Sprint> findAllByProjectId(UUID projectId);
    List<Sprint> findAllByProjectIdAndStatus(UUID projectId, SprintStatus status);
}

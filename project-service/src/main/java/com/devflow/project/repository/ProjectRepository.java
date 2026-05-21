package com.devflow.project.repository;

import com.devflow.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId);
    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Project> findByKeyAndDeletedAtIsNull(String key);
    boolean existsByKeyAndDeletedAtIsNull(String key);
}

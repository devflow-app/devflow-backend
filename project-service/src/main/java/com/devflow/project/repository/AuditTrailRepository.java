package com.devflow.project.repository;

import com.devflow.project.entity.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, UUID> {
    List<AuditTrail> findAllByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, UUID entityId);
}

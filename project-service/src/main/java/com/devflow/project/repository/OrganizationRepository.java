package com.devflow.project.repository;

import com.devflow.project.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findAllByDeletedAtIsNull();
    Optional<Organization> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Organization> findBySlugAndDeletedAtIsNull(String slug);
    boolean existsBySlugAndDeletedAtIsNull(String slug);
}

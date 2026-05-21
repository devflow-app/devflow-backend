package com.devflow.project.service;

import com.devflow.project.entity.AuditTrail;
import com.devflow.project.repository.AuditTrailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditTrailService {

    private final AuditTrailRepository auditTrailRepository;

    @Transactional
    public void logChange(String entityName, UUID entityId, String action, String fieldName, String oldValue, String newValue, UUID actorId) {
        AuditTrail log = AuditTrail.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .actorId(actorId)
                .build();
        auditTrailRepository.save(log);
    }

    public List<AuditTrail> getAuditHistory(String entityName, UUID entityId) {
        return auditTrailRepository.findAllByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId);
    }
}

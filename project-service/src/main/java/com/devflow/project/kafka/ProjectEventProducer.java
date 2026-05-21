package com.devflow.project.kafka;

import com.devflow.common.event.TaskAssignedEvent;
import com.devflow.common.event.TaskCreatedEvent;
import com.devflow.project.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTaskCreated(TaskCreatedEvent event) {
        log.info("Publishing TaskCreatedEvent for taskId: {}", event.getTaskId());
        kafkaTemplate.send(KafkaConfig.TOPIC_TASK_CREATED, event.getTaskId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TaskCreatedEvent: {}", ex.getMessage());
                    } else {
                        log.debug("TaskCreatedEvent published — partition: {}, offset: {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishTaskAssigned(TaskAssignedEvent event) {
        log.info("Publishing TaskAssignedEvent for taskId: {}", event.getTaskId());
        kafkaTemplate.send(KafkaConfig.TOPIC_TASK_ASSIGNED, event.getTaskId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TaskAssignedEvent: {}", ex.getMessage());
                    } else {
                        log.debug("TaskAssignedEvent published — partition: {}, offset: {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

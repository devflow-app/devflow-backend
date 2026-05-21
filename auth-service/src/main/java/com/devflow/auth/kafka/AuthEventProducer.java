package com.devflow.auth.kafka;

import com.devflow.auth.config.KafkaConfig;
import com.devflow.common.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCreated(UserCreatedEvent event) {
        log.info("Publishing UserCreatedEvent for userId: {}", event.getUserId());
        kafkaTemplate.send(KafkaConfig.TOPIC_USER_CREATED, event.getUserId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserCreatedEvent: {}", ex.getMessage());
                    } else {
                        log.debug("UserCreatedEvent published — partition: {}, offset: {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

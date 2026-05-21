package com.devflow.project.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_TASK_CREATED  = "devflow.task.created";
    public static final String TOPIC_TASK_ASSIGNED = "devflow.task.assigned";

    @Bean
    public NewTopic taskCreatedTopic() {
        return TopicBuilder.name(TOPIC_TASK_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic taskAssignedTopic() {
        return TopicBuilder.name(TOPIC_TASK_ASSIGNED).partitions(3).replicas(1).build();
    }
}

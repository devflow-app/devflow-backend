package com.devflow.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_USER_CREATED          = "devflow.user.created";
    public static final String TOPIC_USER_LOGGED_IN        = "devflow.user.logged-in";
    public static final String TOPIC_USER_PASSWORD_CHANGED = "devflow.user.password-changed";

    @Bean
    public NewTopic userCreatedTopic() {
        return TopicBuilder.name(TOPIC_USER_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userLoggedInTopic() {
        return TopicBuilder.name(TOPIC_USER_LOGGED_IN).partitions(3).replicas(1).build();
    }
}

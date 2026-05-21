package com.devflow.notification.consumer;

import com.devflow.common.event.TaskAssignedEvent;
import com.devflow.common.event.UserCreatedEvent;
import com.devflow.notification.model.Notification;
import com.devflow.notification.repository.NotificationRepository;
import com.devflow.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "devflow.user.created", groupId = "notification-service")
    public void consumeUserCreated(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent: {}", event);
        try {
            // 1. Guardar la notificación en BD
            Notification notification = Notification.builder()
                    .userId(UUID.fromString(event.getUserId()))
                    .title("¡Te damos la bienvenida a DevFlow!")
                    .content("Tu cuenta ha sido creada exitosamente. Explora tus proyectos y colabora con tu equipo.")
                    .type("USER_CREATED")
                    .read(false)
                    .build();
            
            notification = notificationRepository.save(notification);
            log.debug("Saved welcome notification with ID: {}", notification.getId());
            
            // 2. Enviar correo de bienvenida
            String fullName = event.getFirstName() + " " + event.getLastName();
            if (event.getEmail() != null && !event.getEmail().trim().isEmpty()) {
                emailService.sendWelcomeEmail(event.getEmail(), fullName);
            } else {
                log.warn("Skipping welcome email: Email is empty for user {}", event.getUserId());
            }
            
            // 3. Transmitir por WebSocket
            messagingTemplate.convertAndSend("/topic/notifications/" + event.getUserId(), notification);
            
        } catch (Exception e) {
            log.error("Failed to process UserCreatedEvent", e);
        }
    }

    @KafkaListener(topics = "devflow.task.assigned", groupId = "notification-service")
    public void consumeTaskAssigned(TaskAssignedEvent event) {
        log.info("Received TaskAssignedEvent: {}", event);
        try {
            // 1. Guardar la notificación en BD
            Notification notification = Notification.builder()
                    .userId(UUID.fromString(event.getAssigneeUserId()))
                    .title("Nueva tarea asignada")
                    .content(String.format("Se te ha asignado la tarea '%s' en el proyecto '%s' por %s.",
                            event.getTaskTitle(), event.getProjectName(), event.getAssignedByUserName()))
                    .type("TASK_ASSIGNED")
                    .read(false)
                    .relatedEntityId(UUID.fromString(event.getTaskId()))
                    .build();
            
            notification = notificationRepository.save(notification);
            log.debug("Saved task assignment notification with ID: {}", notification.getId());
            
            // 2. Enviar correo de asignación
            if (event.getAssigneeEmail() != null && !event.getAssigneeEmail().trim().isEmpty()) {
                emailService.sendTaskAssignedEmail(
                        event.getAssigneeEmail(),
                        event.getTaskTitle(),
                        event.getProjectName(),
                        event.getAssignedByUserName(),
                        event.getProjectId(),
                        event.getTaskId()
                );
            } else {
                log.warn("Skipping task assignment email: Assignee email is empty for user {}", event.getAssigneeUserId());
            }
            
            // 3. Transmitir por WebSocket
            messagingTemplate.convertAndSend("/topic/notifications/" + event.getAssigneeUserId(), notification);
            log.info("Broadcasted notification to WS subscription /topic/notifications/{}", event.getAssigneeUserId());
            
        } catch (Exception e) {
            log.error("Failed to process TaskAssignedEvent", e);
        }
    }
}

package com.devflow.notification.controller;

import com.devflow.common.dto.ApiResponse;
import com.devflow.notification.model.Notification;
import com.devflow.notification.repository.NotificationRepository;
import com.devflow.notification.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Endpoints para la gestión e historial de notificaciones")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Operation(summary = "Obtener notificaciones del usuario", description = "Retorna una página de notificaciones ordenada por fecha de creación descendente.")
    public ResponseEntity<ApiResponse<Page<Notification>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Fetching notifications for user: {}", principal.getId());
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Obtener cantidad de notificaciones no leídas", description = "Retorna el contador de notificaciones no leídas para el usuario autenticado.")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        long count = notificationRepository.countByUserIdAndReadFalse(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marcar notificación como leída", description = "Actualiza el estado 'read' de una notificación específica a true.")
    public ResponseEntity<ApiResponse<Notification>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.debug("Marking notification {} as read for user {}", id, principal.getId());
        return notificationRepository.findById(id)
                .map(notification -> {
                    if (!notification.getUserId().equals(principal.getId())) {
                        return ResponseEntity.status(403).body(ApiResponse.<Notification>error("No tienes permisos para modificar esta notificación"));
                    }
                    notification.setRead(true);
                    Notification updated = notificationRepository.save(notification);
                    return ResponseEntity.ok(ApiResponse.ok("Notificación marcada como leída", updated));
                })
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Notificación no encontrada")));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Marcar todas las notificaciones como leídas", description = "Actualiza el estado 'read' de todas las notificaciones del usuario a true.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Marking all notifications as read for user {}", principal.getId());
        notificationRepository.markAllAsReadByUserId(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Todas las notificaciones marcadas como leídas", null));
    }
}

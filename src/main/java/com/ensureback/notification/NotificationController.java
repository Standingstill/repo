package com.ensureback.notification;

import com.ensureback.notification.dto.NotificationDto;
import com.ensureback.notification.dto.UpdateNotificationDeliveryRequest;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> list(@RequestParam(value = "userId", required = false) UUID userId,
                                                      @RequestParam(value = "recipientEmail", required = false) String recipientEmail) {
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            return ResponseEntity.ok(notificationService.listByRecipient(recipientEmail));
        }
        if (userId != null) {
            return ResponseEntity.ok(notificationService.listByUser(userId.toString()));
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationDto> findById(@PathVariable UUID notificationId) {
        return ResponseEntity.of(notificationService.findById(notificationId));
    }

    @PostMapping("/{notificationId}/delivery")
    public ResponseEntity<NotificationDto> updateDelivery(@PathVariable UUID notificationId,
                                                          @Valid @RequestBody UpdateNotificationDeliveryRequest request) {
        Optional<NotificationDto> updated = notificationService.updateDeliveryStatus(notificationId, request.delivered());
        return updated
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/count-unread")
    public ResponseEntity<Long> countUnread(@RequestParam("userId") UUID userId) {
        long unread = notificationService.countUnread(userId.toString());
        return ResponseEntity.ok(unread);
    }
}
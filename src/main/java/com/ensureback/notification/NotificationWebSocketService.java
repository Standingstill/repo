package com.ensureback.notification;

import com.ensureback.notification.dto.NotificationDto;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(String principal, NotificationDto notification) {
        if (StringUtils.hasText(principal)) {
            messagingTemplate.convertAndSendToUser(principal, "/topic/notifications", notification);
        }
    }

    public void sendToMerchant(UUID merchantId, NotificationDto notification) {
        if (merchantId != null) {
            messagingTemplate.convertAndSend("/topic/merchant." + merchantId, notification);
        }
    }

    public void sendToAdmins(NotificationDto notification) {
        messagingTemplate.convertAndSend("/topic/admin", notification);
    }
}
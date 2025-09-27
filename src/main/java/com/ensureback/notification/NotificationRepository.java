package com.ensureback.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Notification> findByUserIdAndDeliveredFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail, Pageable pageable);

    long countByUserIdAndDeliveredFalse(String userId);
}
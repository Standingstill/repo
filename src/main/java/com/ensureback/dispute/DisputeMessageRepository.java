package com.ensureback.dispute;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeMessageRepository extends JpaRepository<DisputeMessage, UUID> {

    List<DisputeMessage> findByDispute_IdOrderByCreatedAtAsc(UUID disputeId);
}

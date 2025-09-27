package com.ensureback.timer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimerRepository extends JpaRepository<Timer, UUID> {

    List<Timer> findByStateAndExpiresAtBefore(Timer.State state, OffsetDateTime expiresAt);

    Optional<Timer> findByOrder_IdAndType(UUID orderId, Timer.Type type);
}

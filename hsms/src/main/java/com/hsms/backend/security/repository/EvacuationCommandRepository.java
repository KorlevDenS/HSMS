package com.hsms.backend.security.repository;

import com.hsms.backend.common.HsmsDomain.EvacuationStatus;
import com.hsms.backend.security.model.EvacuationCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface EvacuationCommandRepository extends JpaRepository<EvacuationCommand, Long> {
    List<EvacuationCommand> findByStatusInAndExpiresAtBefore(Collection<EvacuationStatus> statuses, Instant expiresAt);
}

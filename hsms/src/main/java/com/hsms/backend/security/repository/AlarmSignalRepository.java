package com.hsms.backend.security.repository;

import com.hsms.backend.security.model.AlarmSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlarmSignalRepository extends JpaRepository<AlarmSignal, Long> {
    Optional<AlarmSignal> findByMissionIdAndExternalEventId(Long missionId, String externalEventId);
}

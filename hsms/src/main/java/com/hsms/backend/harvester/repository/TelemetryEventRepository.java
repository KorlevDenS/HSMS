package com.hsms.backend.harvester.repository;

import com.hsms.backend.common.FreshnessStatus;
import com.hsms.backend.harvester.model.TelemetryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, Long> {
    List<TelemetryEvent> findByMissionIdOrderByEventTimeDescIdDesc(Long missionId);

    Optional<TelemetryEvent> findFirstByMissionIdAndFreshnessStatusOrderByEventTimeDescIdDesc(Long missionId, FreshnessStatus freshnessStatus);

    Optional<TelemetryEvent> findByMissionIdAndExternalEventId(Long missionId, String externalEventId);
}

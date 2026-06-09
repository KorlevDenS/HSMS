package com.hsms.backend.harvester.repository;

import com.hsms.backend.harvester.model.MissionReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionReportRepository extends JpaRepository<MissionReport, Long> {
    Optional<MissionReport> findByMissionId(Long missionId);
}

package com.hsms.backend.mission.repository;

import com.hsms.backend.mission.model.MissionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MissionPlanRepository extends JpaRepository<MissionPlan, Long> {
    Optional<MissionPlan> findByMissionId(Long missionId);
}

package com.hsms.backend.risk.repository;

import com.hsms.backend.risk.model.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {
    Optional<RiskScore> findFirstByMissionIdOrderByCalculatedAtDescIdDesc(Long missionId);

    List<RiskScore> findByMissionIdOrderByCalculatedAtDescIdDesc(Long missionId);

    List<RiskScore> findByMissionIdAndStaleFalse(Long missionId);
}

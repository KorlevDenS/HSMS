package com.hsms.backend.insurance.repository;

import com.hsms.backend.common.InsuranceStatus;
import com.hsms.backend.insurance.model.InsuranceCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceCaseRepository extends JpaRepository<InsuranceCase, Long> {
    List<InsuranceCase> findAllByOrderByIdAsc();

    long countByStatusNot(InsuranceStatus status);

    Optional<InsuranceCase> findFirstByMissionIdOrderByOpenedAtDescIdDesc(Long missionId);

    Optional<InsuranceCase> findFirstByMissionIdAndIncidentIdIsNullAndStatusNotOrderByOpenedAtDescIdDesc(Long missionId, InsuranceStatus status);

    Optional<InsuranceCase> findFirstByMissionIdAndIncidentIdAndStatusNotOrderByOpenedAtDescIdDesc(Long missionId, Long incidentId, InsuranceStatus status);
}

package com.hsms.backend.insurance.repository;

import com.hsms.backend.insurance.model.InsuranceRecalculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceRecalculationRepository extends JpaRepository<InsuranceRecalculation, Long> {
    List<InsuranceRecalculation> findByInsuranceCaseIdOrderByCalculatedAtDescIdDesc(Long caseId);

    Optional<InsuranceRecalculation> findFirstByInsuranceCaseIdOrderByCalculatedAtDescIdDesc(Long caseId);
}

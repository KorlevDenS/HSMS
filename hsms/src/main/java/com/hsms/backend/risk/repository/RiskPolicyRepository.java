package com.hsms.backend.risk.repository;

import com.hsms.backend.risk.model.RiskPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskPolicyRepository extends JpaRepository<RiskPolicy, Long> {
    Optional<RiskPolicy> findFirstByActiveTrueOrderByActiveFromDescIdDesc();

    List<RiskPolicy> findByActiveTrue();
}

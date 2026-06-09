package com.hsms.backend.readmodel;

import com.hsms.backend.common.repository.AuditEventRepository;
import com.hsms.backend.insurance.repository.InsuranceCaseRepository;
import com.hsms.backend.mission.repository.MissionRepository;
import com.hsms.backend.risk.repository.RiskPolicyRepository;
import com.hsms.backend.security.repository.IncidentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HsmsModuleHealthService {

    private final AuditEventRepository auditEventRepository;
    private final MissionRepository missionRepository;
    private final RiskPolicyRepository riskPolicyRepository;
    private final IncidentRepository incidentRepository;
    private final InsuranceCaseRepository insuranceCaseRepository;

    public HsmsModuleHealthService(
            AuditEventRepository auditEventRepository,
            MissionRepository missionRepository,
            RiskPolicyRepository riskPolicyRepository,
            IncidentRepository incidentRepository,
            InsuranceCaseRepository insuranceCaseRepository
    ) {
        this.auditEventRepository = auditEventRepository;
        this.missionRepository = missionRepository;
        this.riskPolicyRepository = riskPolicyRepository;
        this.incidentRepository = incidentRepository;
        this.insuranceCaseRepository = insuranceCaseRepository;
    }

    public Map<String, Boolean> snapshot() {
        Map<String, Boolean> status = new LinkedHashMap<>();
        status.put("common", repositoryAvailable(auditEventRepository));
        status.put("mission", repositoryAvailable(missionRepository));
        status.put("risk", activeRiskPolicyAvailable());
        status.put("security", repositoryAvailable(incidentRepository));
        status.put("insurance", repositoryAvailable(insuranceCaseRepository));
        return status;
    }

    public double gauge(String module) {
        return switch (module) {
            case "common" -> repositoryAvailable(auditEventRepository) ? 1.0 : 0.0;
            case "mission" -> repositoryAvailable(missionRepository) ? 1.0 : 0.0;
            case "risk" -> activeRiskPolicyAvailable() ? 1.0 : 0.0;
            case "security" -> repositoryAvailable(incidentRepository) ? 1.0 : 0.0;
            case "insurance" -> repositoryAvailable(insuranceCaseRepository) ? 1.0 : 0.0;
            default -> 0.0;
        };
    }

    private boolean activeRiskPolicyAvailable() {
        try {
            return riskPolicyRepository.findFirstByActiveTrueOrderByActiveFromDescIdDesc().isPresent();
        } catch (RuntimeException error) {
            return false;
        }
    }

    private boolean repositoryAvailable(JpaRepository<?, ?> repository) {
        try {
            repository.count();
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }
}

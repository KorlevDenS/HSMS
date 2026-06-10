package com.hsms.backend.readmodel;

import com.hsms.backend.common.IncidentStatus;
import com.hsms.backend.common.InsuranceStatus;
import com.hsms.backend.common.MissionStatus;
import com.hsms.backend.insurance.repository.InsuranceCaseRepository;
import com.hsms.backend.mission.repository.MissionRepository;
import com.hsms.backend.security.repository.IncidentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HsmsDomainMetrics {

    private final HsmsModuleHealthService moduleHealthService;

    public HsmsDomainMetrics(
            MeterRegistry registry,
            MissionRepository missionRepository,
            IncidentRepository incidentRepository,
            InsuranceCaseRepository insuranceCaseRepository,
            HsmsModuleHealthService moduleHealthService
    ) {
        this.moduleHealthService = moduleHealthService;
        registry.gauge("hsms_missions_active", missionRepository,
                repository -> repository.countByStatus(MissionStatus.ACTIVE));
        registry.gauge("hsms_missions_closed_total", missionRepository,
                repository -> repository.countByStatusIn(List.of(MissionStatus.CLOSED, MissionStatus.LOST)));
        registry.gauge("hsms_incidents_open", incidentRepository,
                repository -> repository.countByStatusNot(IncidentStatus.CLOSED));
        registry.gauge("hsms_insurance_cases_open", insuranceCaseRepository,
                repository -> repository.countByStatusNot(InsuranceStatus.CLOSED));
        registerModuleHealth(registry, "common");
        registerModuleHealth(registry, "mission");
        registerModuleHealth(registry, "risk");
        registerModuleHealth(registry, "security");
        registerModuleHealth(registry, "insurance");
    }

    private void registerModuleHealth(MeterRegistry registry, String module) {
        Gauge.builder("hsms_module_health", moduleHealthService, service -> service.gauge(module))
                .description("HSMS module health-check status: 1 means available, 0 means failed probe")
                .tag("module", module)
                .register(registry);
    }
}

package com.hsms.backend.simulation.service;

import com.hsms.backend.common.HsmsAccessService;
import com.hsms.backend.common.HsmsDomain.*;
import com.hsms.backend.harvester.api.HarvesterApi;
import com.hsms.backend.mission.api.MissionApi;
import com.hsms.backend.risk.api.RiskApi;
import com.hsms.backend.security.api.SecurityApi;
import com.hsms.backend.simulation.api.SimulationApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.hsms.backend.common.HsmsOps.blankToDefault;

@Service
@Transactional
public class DesertSimulationService implements SimulationApi {

    private final HsmsAccessService access;
    private final MissionApi missionApi;
    private final RiskApi riskApi;
    private final HarvesterApi harvesterApi;
    private final SecurityApi securityApi;

    public DesertSimulationService(
            HsmsAccessService access,
            MissionApi missionApi,
            RiskApi riskApi,
            HarvesterApi harvesterApi,
            SecurityApi securityApi
    ) {
        this.access = access;
        this.missionApi = missionApi;
        this.riskApi = riskApi;
        this.harvesterApi = harvesterApi;
        this.securityApi = securityApi;
    }

    @Override
    public DesertSimulationResult runDesertScenario(String actorLogin, DesertSimulationRequest request) {
        access.requireAny(actorLogin, RoleCode.ROLE_ADMINISTRATOR);
        DesertSimulationRequest scenario = normalize(request);
        Instant now = Instant.now();
        List<RoutePointDto> route = routeForZone(scenario.zoneId());
        MissionCreateRequest missionRequest = new MissionCreateRequest();
        missionRequest.title = "Симуляция пустыни " + now;
        missionRequest.zoneId = scenario.zoneId();
        missionRequest.harvesterId = scenario.harvesterId();
        missionRequest.crewId = scenario.crewId();
        missionRequest.plannedStart = now.plusSeconds(600);
        missionRequest.plannedEnd = now.plusSeconds(4 * 3600L);
        missionRequest.route = route;
        MissionDto mission = missionApi.createMission(actorLogin, missionRequest);
        RiskSnapshotDto risk = riskApi.assessRisk(actorLogin, mission.id());
        mission = missionApi.launchMission(actorLogin, mission.id(), new LaunchRequest(true, "Старт тестового сценария среды"));

        List<TelemetryEventDto> telemetry = new ArrayList<>();
        for (int index = 0; index < scenario.telemetryPoints(); index++) {
            RoutePointDto base = route.get(Math.min(index, route.size() - 1));
            String status = equipmentStatus(index, scenario);
            TelemetryResponse response = harvesterApi.submitTelemetry(actorLogin, mission.id(), new TelemetryRequest(
                    "sim-" + mission.id() + "-tel-" + index,
                    now.plusSeconds(700 + index * 60L),
                    base.lat() + index * 0.012,
                    base.lon() + index * 0.015,
                    status
            ));
            telemetry.add(response.event());
        }

        AlarmResponse alarm = null;
        if (scenario.includeAlarm()) {
            alarm = securityApi.submitAlarm(actorLogin, mission.id(), new AlarmRequest(
                    "sim-" + mission.id() + "-alarm-1",
                    now.plusSeconds(700 + scenario.telemetryPoints() * 60L),
                    "Симулятор: акустическая и сейсмическая сигнатура угрозы"
            ));
            securityApi.classifyIncident(actorLogin, alarm.incidentId(), new ClassificationRequest(
                    scenario.threatSeverity(),
                    "Классификация симулятора пустынной среды"
            ));
        }

        risk = riskApi.latestRisk(mission.id());
        return new DesertSimulationResult(missionApi.mission(mission.id()), telemetry, alarm, risk);
    }

    private DesertSimulationRequest normalize(DesertSimulationRequest request) {
        if (request == null) {
            return new DesertSimulationRequest(1, 1, 1, 8, Severity.HIGH, true);
        }
        int points = request.telemetryPoints() <= 0 ? 8 : Math.min(request.telemetryPoints(), 50);
        return new DesertSimulationRequest(
                request.zoneId() <= 0 ? 1 : request.zoneId(),
                request.harvesterId() <= 0 ? 1 : request.harvesterId(),
                request.crewId() <= 0 ? 1 : request.crewId(),
                points,
                request.threatSeverity() == null ? Severity.HIGH : request.threatSeverity(),
                request.includeAlarm()
        );
    }

    private List<RoutePointDto> routeForZone(long zoneId) {
        double lat = switch ((int) zoneId) {
            case 2 -> 22.11;
            case 3 -> 20.01;
            default -> 24.42;
        };
        double lon = switch ((int) zoneId) {
            case 2 -> 53.91;
            case 3 -> 51.74;
            default -> 54.20;
        };
        return List.of(
                new RoutePointDto(1, lat, lon),
                new RoutePointDto(2, lat + 0.18, lon + 0.14),
                new RoutePointDto(3, lat + 0.36, lon + 0.31),
                new RoutePointDto(4, lat + 0.52, lon + 0.45)
        );
    }

    private String equipmentStatus(int index, DesertSimulationRequest request) {
        if (!request.includeAlarm()) {
            return "NORMAL";
        }
        if (index >= Math.max(1, request.telemetryPoints() - 2)) {
            return switch (request.threatSeverity()) {
                case CRITICAL -> "CRITICAL_DRIVE_STRESS";
                case HIGH -> "DEGRADED_SEISMIC_NOISE";
                case MEDIUM -> "WARN_SENSOR_DRIFT";
                case LOW -> "NORMAL";
            };
        }
        return blankToDefault(null, "NORMAL");
    }
}

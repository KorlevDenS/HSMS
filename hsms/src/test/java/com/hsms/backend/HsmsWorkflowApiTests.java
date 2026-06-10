package com.hsms.backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HsmsWorkflowApiTests extends H2IntegrationTestBase {

    @LocalServerPort
    int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    @Test
    void allFourUseCasesWorkThroughHttpApi() {
        Map<String, Object> created = post("dispatcher", "/missions", Map.of(
                "title", "Интеграционный рейс UC-01",
                "zoneId", 2,
                "harvesterId", 2,
                "crewId", 2,
                "plannedStart", Instant.now().plusSeconds(3600).toString(),
                "plannedEnd", Instant.now().plusSeconds(14400).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 22.11, "lon", 53.91),
                        Map.of("seqNo", 2, "lat", 22.35, "lon", 54.02),
                        Map.of("seqNo", 3, "lat", 22.60, "lon", 54.25)
                )
        ));
        long missionId = id(created);

        Map<String, Object> risk = post("dispatcher", "/missions/" + missionId + "/risk-assessments", Map.of());
        assertThat((Integer) risk.get("riskScore")).isBetween(0, 100);

        Map<String, Object> launched = post("dispatcher", "/missions/" + missionId + "/launch", Map.of(
                "confirmWarning", true,
                "reason", "Подтверждено интеграционным тестом"
        ));
        assertThat(launched.get("status")).isEqualTo("ACTIVE");

        Map<String, Object> plan = getJson("crew", "/missions/" + missionId + "/plan");
        assertThat(plan.get("missionId")).isEqualTo((int) missionId);
        Map<String, Object> acknowledgedPlan = post("crew", "/missions/" + missionId + "/plan/ack", Map.of());
        assertThat(acknowledgedPlan.get("acknowledgedBy")).isEqualTo("crew");

        Map<String, Object> telemetry = post("crew", "/missions/" + missionId + "/telemetry", Map.of(
                "externalEventId", "it-telemetry-1",
                "eventTime", Instant.now().toString(),
                "lat", 22.42,
                "lon", 54.14,
                "equipmentStatus", "NORMAL"
        ));
        assertThat(telemetry.get("status")).isIn("ACCEPTED", "DUPLICATE");
        assertThat(telemetry.get("riskMarkedStale")).isEqualTo(true);

        Map<String, Object> degradedTelemetry = post("crew", "/missions/" + missionId + "/telemetry", Map.of(
                "externalEventId", "it-telemetry-degraded",
                "eventTime", Instant.now().plusSeconds(1).toString(),
                "lat", 22.43,
                "lon", 54.15,
                "equipmentStatus", "DEGRADED"
        ));
        assertThat(degradedTelemetry.get("riskMarkedStale")).isEqualTo(true);
        Map<String, Object> monitoredMission = getJson("dispatcher", "/missions/" + missionId);
        assertThat(((Number) monitoredMission.get("monitoringPriority")).intValue()).isGreaterThanOrEqualTo(60);
        assertThat((String) monitoredMission.get("monitoringContext")).contains("DEGRADED");

        Map<String, Object> alarm = post("crew", "/missions/" + missionId + "/alarms", Map.of(
                "externalEventId", "it-alarm-1",
                "eventTime", Instant.now().toString(),
                "reason", "Сигнатура песчаного червя"
        ));
        long incidentId = ((Number) alarm.get("incidentId")).longValue();
        Map<String, Object> missionAfterAlarm = getJson("dispatcher", "/missions/" + missionId);
        assertThat(missionAfterAlarm.get("riskReviewRequiredAt")).isNull();
        assertThat(missionAfterAlarm.get("riskReviewReason")).isNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> riskAfterAlarm = (Map<String, Object>) missionAfterAlarm.get("risk");
        assertThat(riskAfterAlarm.get("stale")).isEqualTo(false);
        @SuppressWarnings("unchecked")
        Map<String, Object> riskFactorsAfterAlarm = (Map<String, Object>) riskAfterAlarm.get("factors");
        assertThat(((Number) riskFactorsAfterAlarm.get("incidentPenalty")).doubleValue()).isGreaterThan(0.0);

        Map<String, Object> classified = patch("security", "/incidents/" + incidentId + "/classification", Map.of(
                "severity", "CRITICAL",
                "reason", "Угроза подтверждена"
        ));
        assertThat(classified.get("status")).isEqualTo("CLASSIFIED");

        Map<String, Object> evacuation = post("security", "/incidents/" + incidentId + "/evacuation", Map.of(
                "reason", "Запуск эвакуации по НВР"
        ));
        assertThat(evacuation.get("status")).isEqualTo("SENT");

        Map<String, Object> delivered = post("crew", "/incidents/" + incidentId + "/evacuation/delivered", Map.of());
        assertThat(delivered.get("status")).isEqualTo("DELIVERED");
        assertThat(delivered.get("deliveredAt")).isNotNull();

        Map<String, Object> acknowledged = post("crew", "/incidents/" + incidentId + "/evacuation/ack", Map.of());
        assertThat(acknowledged.get("status")).isEqualTo("ACKNOWLEDGED");

        post("security", "/incidents/" + incidentId + "/close", Map.of());

        Map<String, Object> incompleteReport = new java.util.HashMap<>();
        incompleteReport.put("actualStart", Instant.now().minusSeconds(7200).toString());
        incompleteReport.put("actualEnd", null);
        incompleteReport.put("spiceAmount", BigDecimal.valueOf(1204));
        incompleteReport.put("harvesterFinalStatus", "READY");
        incompleteReport.put("abnormalSituations", "");
        assertThat(status("crew", "POST", "/missions/" + missionId + "/report", incompleteReport)).isEqualTo(400);

        Instant reportActualEnd = Instant.now();
        post("crew", "/missions/" + missionId + "/report", Map.of(
                "actualStart", Instant.now().minusSeconds(7200).toString(),
                "actualEnd", reportActualEnd.toString(),
                "spiceAmount", BigDecimal.valueOf(1204),
                "harvesterFinalStatus", "READY",
                "abnormalSituations", "Эвакуация выполнена"
        ));

        Map<String, Object> closedMission = post("dispatcher", "/missions/" + missionId + "/close", Map.of(
                "finalStatus", "CLOSED",
                "reason", "Рейс закрыт после отчета экипажа"
        ));
        assertThat(closedMission.get("status")).isEqualTo("CLOSED");
        assertThat(closedMission.get("closedBy")).isEqualTo("dispatcher");
        assertThat(closedMission.get("riskReviewRequiredAt")).isNull();
        assertThat(closedMission.get("riskReviewReason")).isNull();
        long insuranceCaseId = ((Number) closedMission.get("insuranceCaseId")).longValue();

        Map<String, Object> waitingCase = post("insurance", "/insurance-cases/" + insuranceCaseId + "/close", Map.of(
                "reason", "Попытка закрытия без перерасчета"
        ));
        assertThat(waitingCase.get("status")).isEqualTo("WAITING_FOR_DATA");
        assertThat((String) waitingCase.get("missingData")).contains("finalRiskScore").contains("finalPremium");

        Map<String, Object> recalculated = post("insurance", "/insurance-cases/" + insuranceCaseId + "/recalculate", Map.of(
                "reason", "Перерасчет по закрытому рейсу"
        ));
        assertThat(recalculated.get("status")).isEqualTo("RECALCULATED");
        assertThat(recalculated.get("finalPremium")).isNotNull();
        assertThat(recalculated.get("incidentSeverity")).isEqualTo("CRITICAL");
        assertThat(recalculated.get("incidentClosedAt")).isNotNull();
        assertThat(recalculated.get("incidentOperator")).isEqualTo("security");

        Map<String, Object> termsUpdated = patch("insurance", "/insurance-cases/" + insuranceCaseId + "/terms", Map.of(
                "premium", BigDecimal.valueOf(2200),
                "reason", "Ручное обновление условий после проверки истории"
        ));
        assertThat(termsUpdated.get("status")).isEqualTo("TERMS_UPDATED");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> termsHistory = (List<Map<String, Object>>) termsUpdated.get("history");
        assertThat(termsHistory).anySatisfy(item -> assertThat(item.get("eventType")).isEqualTo("TERMS_UPDATED"));

        Map<String, Object> closedCase = post("insurance", "/insurance-cases/" + insuranceCaseId + "/close", Map.of(
                "reason", "Финальное страховое решение"
        ));
        assertThat(closedCase.get("status")).isEqualTo("CLOSED");

        String missionsCsv = get("dispatcher", "/reports/missions.csv");
        assertThat(missionsCsv)
                .contains("createdAt,updatedAt,plannedStart,plannedEnd,actualStart,reportActualStart,reportActualEnd,reportSubmittedAt,closedAt")
                .contains("relatedIncidentIds")
                .contains("Интеграционный рейс UC-01")
                .contains(reportActualEnd.toString());
    }

    @Test
    void draftAndRiskCancellationPayloadsMatchRequirements() {
        Map<String, Object> incompleteDraft = post("dispatcher", "/missions", Map.of(
                "title", "Черновик без ресурсов"
        ));
        assertThat(incompleteDraft.get("status")).isEqualTo("DRAFT");
        assertThat((String) incompleteDraft.get("draftMissingFields"))
                .contains("zoneId")
                .contains("harvesterId")
                .contains("crewId")
                .contains("plannedStart")
                .contains("plannedEnd")
                .contains("route");
        assertThat(status("dispatcher", "POST", "/missions/" + id(incompleteDraft) + "/risk-assessments", Map.of())).isEqualTo(400);

        Map<String, Object> harvester = post("dispatcher", "/harvesters", Map.of(
                "name", "HV-IT-RISK-CANCEL",
                "type", "STANDARD",
                "status", "READY",
                "noiseLevel", 0.35,
                "capacity", 100
        ));
        Map<String, Object> crew = post("dispatcher", "/crews", Map.of(
                "name", "Экипаж IT риск-отмена",
                "status", "READY",
                "contactChannel", "it-risk-cancel",
                "memberCount", 4,
                "assignedLogin", "crew"
        ));
        Map<String, Object> mission = post("dispatcher", "/missions", Map.of(
                "title", "Рейс риск-отмены",
                "zoneId", 3,
                "harvesterId", id(harvester),
                "crewId", id(crew),
                "plannedStart", Instant.now().plusSeconds(3600).toString(),
                "plannedEnd", Instant.now().plusSeconds(7200).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 20.01, "lon", 51.74),
                        Map.of("seqNo", 2, "lat", 20.22, "lon", 51.95)
                )
        ));
        long missionId = id(mission);
        post("dispatcher", "/missions/" + missionId + "/risk-assessments", Map.of());
        Map<String, Object> cancelled = post("dispatcher", "/missions/" + missionId + "/risk-cancel", Map.of(
                "reason", "Risk-score выше допустимого порога"
        ));
        assertThat(cancelled.get("status")).isEqualTo("RISK_CANCELLED");
        assertThat(cancelled.get("closedBy")).isEqualTo("dispatcher");
        assertThat(cancelled.get("riskReviewRequiredAt")).isNull();
        assertThat(cancelled.get("riskReviewReason")).isNull();

        List<Map<String, Object>> cases = list("insurance", "/insurance-cases");
        Map<String, Object> riskCase = cases.stream()
                .filter(item -> ((Number) item.get("missionId")).longValue() == missionId && item.get("incidentId") == null)
                .findFirst()
                .orElseThrow();
        assertThat(riskCase.get("status")).isEqualTo("READY_FOR_RECALCULATION");
        assertThat(riskCase.get("triggerType")).isEqualTo("RISK_CANCELLATION");
        assertThat(riskCase.get("triggerReason")).isEqualTo("Risk-score выше допустимого порога");
        assertThat(riskCase.get("triggerPAttack")).isNotNull();
        assertThat(riskCase.get("triggerRiskScore")).isNotNull();
        assertThat(riskCase.get("triggerDecisionAt")).isNotNull();
        assertThat(riskCase.get("triggerDecisionBy")).isEqualTo("dispatcher");
        assertThat(riskCase.get("missingData")).isNull();
    }

    @Test
    void missionRouteCanBeReplacedWithReusedSequenceNumbers() {
        Map<String, Object> harvester = post("dispatcher", "/harvesters", Map.of(
                "name", "HV-IT-ROUTE-REPLACE",
                "type", "STANDARD",
                "status", "READY",
                "noiseLevel", 0.31,
                "capacity", 115
        ));
        Map<String, Object> crew = post("dispatcher", "/crews", Map.of(
                "name", "Экипаж IT замены маршрута",
                "status", "READY",
                "contactChannel", "it-route-replace",
                "memberCount", 4,
                "assignedLogin", "crew"
        ));
        Map<String, Object> mission = post("dispatcher", "/missions", Map.of(
                "title", "Рейс проверки замены маршрута",
                "zoneId", 2,
                "harvesterId", id(harvester),
                "crewId", id(crew),
                "plannedStart", Instant.now().plusSeconds(5400).toString(),
                "plannedEnd", Instant.now().plusSeconds(10800).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 22.10, "lon", 53.90),
                        Map.of("seqNo", 2, "lat", 22.20, "lon", 54.00),
                        Map.of("seqNo", 3, "lat", 22.30, "lon", 54.10)
                )
        ));
        long missionId = id(mission);
        assertThat(((Number) mission.get("routeVersion")).intValue()).isEqualTo(1);

        Map<String, Object> firstReplacement = patch("dispatcher", "/missions/" + missionId, Map.of(
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 23.10, "lon", 55.10),
                        Map.of("seqNo", 2, "lat", 23.20, "lon", 55.20),
                        Map.of("seqNo", 3, "lat", 23.30, "lon", 55.30)
                )
        ));
        assertThat(firstReplacement.get("status")).isEqualTo("RISK_ASSESSED");
        assertThat(((Number) firstReplacement.get("routeVersion")).intValue()).isEqualTo(2);
        assertRoutePoint(firstReplacement, 0, 1, 23.10, 55.10);
        assertRoutePoint(firstReplacement, 1, 2, 23.20, 55.20);
        assertRoutePoint(firstReplacement, 2, 3, 23.30, 55.30);

        Map<String, Object> secondReplacement = patch("dispatcher", "/missions/" + missionId, Map.of(
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 24.10, "lon", 56.10),
                        Map.of("seqNo", 2, "lat", 24.20, "lon", 56.20)
                )
        ));
        assertThat(secondReplacement.get("status")).isEqualTo("RISK_ASSESSED");
        assertThat(((Number) secondReplacement.get("routeVersion")).intValue()).isEqualTo(3);
        assertRoutePoint(secondReplacement, 0, 1, 24.10, 56.10);
        assertRoutePoint(secondReplacement, 1, 2, 24.20, 56.20);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> route = (List<Map<String, Object>>) secondReplacement.get("route");
        assertThat(route).hasSize(2);
    }

    @Test
    void alternativeSecurityAndPolicyFlowsWorkThroughHttpApi() {
        Map<String, Object> policy = patch("admin", "/risk-policy", Map.of(
                "version", "it-policy-2",
                "warningThreshold", 45,
                "blockThreshold", 90,
                "formulaDescription", "Интеграционный тест изменения порогов риска",
                "changeReason", "Проверка управляемого изменения политики риска",
                "validatedScenarios", "normal launch, warning launch, blocking risk, degraded telemetry, incident, CHOAM insurance",
                "choamImpact", "Policy version is preserved in risk snapshots and insurance decisions"
        ));
        assertThat(policy.get("version")).isEqualTo("it-policy-2");
        assertThat(policy.get("warningThreshold")).isEqualTo(45);
        assertThat(policy.get("blockThreshold")).isEqualTo(90);
        assertThat(policy.get("changedBy")).isEqualTo("admin");
        assertThat(policy.get("changeReason")).isEqualTo("Проверка управляемого изменения политики риска");

        long harvesterId = firstId("dispatcher", "/harvesters/free");
        long crewId = firstId("dispatcher", "/crews/free");

        Map<String, Object> created = post("dispatcher", "/missions", Map.of(
                "title", "Интеграционный рейс UC-03 LOW",
                "zoneId", 3,
                "harvesterId", harvesterId,
                "crewId", crewId,
                "plannedStart", Instant.now().plusSeconds(5400).toString(),
                "plannedEnd", Instant.now().plusSeconds(10800).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 18.10, "lon", 55.30),
                        Map.of("seqNo", 2, "lat", 18.25, "lon", 55.45)
                )
        ));
        long missionId = id(created);
        post("dispatcher", "/missions/" + missionId + "/risk-assessments", Map.of());
        post("dispatcher", "/missions/" + missionId + "/launch", Map.of(
                "confirmWarning", true,
                "reason", "Подтверждено альтернативным интеграционным тестом"
        ));

        Map<String, Object> alarm = post("crew", "/missions/" + missionId + "/alarms", Map.of(
                "externalEventId", "it-low-alarm-1",
                "eventTime", Instant.now().toString(),
                "reason", "Сомнительная телеметрия без подтверждения угрозы"
        ));
        long incidentId = ((Number) alarm.get("incidentId")).longValue();

        Map<String, Object> classified = patch("security", "/incidents/" + incidentId + "/classification", Map.of(
                "severity", "LOW",
                "reason", "Низкая критичность, требуется наблюдение"
        ));
        assertThat(classified.get("status")).isEqualTo("MONITORING");
        assertThat(classified.get("severity")).isEqualTo("LOW");

        Map<String, Object> dashboard = getJson("management", "/dashboard/operations");
        @SuppressWarnings("unchecked")
        Map<String, Object> incidentsBySeverity = (Map<String, Object>) dashboard.get("incidentsBySeverity");
        assertThat(((Number) incidentsBySeverity.get("LOW")).longValue()).isGreaterThanOrEqualTo(1);

        String incidentsCsv = get("security", "/reports/incidents.csv");
        assertThat(incidentsCsv).contains("LOW").contains("MONITORING");
    }

    @Test
    void rbacResourceAndEvacuationFailureControlsAreEnforced() {
        List<Map<String, Object>> roles = list("admin", "/roles");
        assertThat(roles).noneSatisfy(role -> assertThat(role.get("name")).isEqualTo("ROLE_SUPERVISOR"));
        assertThat(status("management", "PATCH", "/risk-policy", Map.of(
                "version", "it-policy-forbidden",
                "warningThreshold", 40,
                "blockThreshold", 80,
                "formulaDescription", "Недопустимое изменение надзором",
                "changeReason", "Надзор не должен менять risk policy напрямую",
                "validatedScenarios", "normal launch, warning launch, blocking risk, degraded telemetry, incident, CHOAM insurance",
                "choamImpact", "CHOAM impact is not approved by administrator"
        ))).isEqualTo(403);

        Map<String, Object> crewBootstrap = getJson("crew", "/bootstrap");
        assertThat((List<?>) crewBootstrap.get("users")).isEmpty();
        assertThat((List<?>) crewBootstrap.get("audit")).isEmpty();
        assertThat(crewBootstrap.get("dashboard")).isNull();

        Map<String, Object> harvester = post("dispatcher", "/harvesters", Map.of(
                "name", "HV-IT-RBAC",
                "type", "STANDARD",
                "status", "READY",
                "noiseLevel", 0.25,
                "capacity", 100
        ));
        Map<String, Object> crew = post("dispatcher", "/crews", Map.of(
                "name", "Экипаж IT RBAC",
                "status", "READY",
                "contactChannel", "it-rbac",
                "memberCount", 4,
                "assignedLogin", "crew-other"
        ));

        Map<String, Object> mission = post("dispatcher", "/missions", Map.of(
                "title", "Рейс проверки RBAC экипажа",
                "zoneId", 1,
                "harvesterId", id(harvester),
                "crewId", id(crew),
                "plannedStart", Instant.now().plusSeconds(7200).toString(),
                "plannedEnd", Instant.now().plusSeconds(14400).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 24.42, "lon", 54.20),
                        Map.of("seqNo", 2, "lat", 24.52, "lon", 54.34)
                )
        ));
        long missionId = id(mission);
        assertThat(mission.get("status")).isEqualTo("DRAFT");
        post("dispatcher", "/missions/" + missionId + "/risk-assessments", Map.of());
        post("dispatcher", "/missions/" + missionId + "/launch", Map.of(
                "confirmWarning", true,
                "reason", "Запуск для проверки RBAC экипажа"
        ));
        assertThat(statusGet("crew", "/missions/" + missionId + "/plan")).isEqualTo(403);
        assertThat(status("crew", "POST", "/missions/" + missionId + "/plan/ack", Map.of())).isEqualTo(403);
        assertThat(status("crew", "POST", "/missions/" + missionId + "/telemetry", Map.of(
                "externalEventId", "it-rbac-forbidden",
                "eventTime", Instant.now().toString(),
                "lat", 24.45,
                "lon", 54.21,
                "equipmentStatus", "NORMAL"
        ))).isEqualTo(403);
        assertThat(status("crew", "POST", "/missions/" + missionId + "/report", Map.of(
                "actualStart", Instant.now().minusSeconds(1800).toString(),
                "actualEnd", Instant.now().toString(),
                "spiceAmount", BigDecimal.TEN,
                "harvesterFinalStatus", "READY",
                "abnormalSituations", "Нет"
        ))).isEqualTo(403);

        Map<String, Object> alarm = post("admin", "/missions/" + missionId + "/alarms", Map.of(
                "externalEventId", "it-rbac-admin-alarm",
                "eventTime", Instant.now().toString(),
                "reason", "Проверка жизненного цикла эвакуации"
        ));
        @SuppressWarnings("unchecked")
        Map<String, Object> incident = (Map<String, Object>) alarm.get("incident");
        Instant startedAt = Instant.parse((String) incident.get("slaStartedAt"));
        Instant deadlineAt = Instant.parse((String) incident.get("slaDeadlineAt"));
        assertThat(java.time.Duration.between(startedAt, deadlineAt)).isEqualTo(java.time.Duration.ofMinutes(5));

        long incidentId = ((Number) alarm.get("incidentId")).longValue();
        Map<String, Object> evacuation = post("security", "/incidents/" + incidentId + "/evacuation", Map.of(
                "reason", "Проверка отправки команды"
        ));
        assertThat(evacuation.get("status")).isEqualTo("SENT");
        Map<String, Object> failed = post("security", "/incidents/" + incidentId + "/evacuation/delivery-failed", Map.of(
                "reason", "Канал связи не подтвердил доставку"
        ));
        assertThat(failed.get("status")).isEqualTo("DELIVERY_FAILED");
    }

    @Test
    void riskListGovernanceTelemetryAndEvacuationControlsAreEnforced() {
        assertThat(status("admin", "PATCH", "/risk-policy", Map.of(
                "version", "it-policy-without-context",
                "warningThreshold", 40,
                "blockThreshold", 80,
                "formulaDescription", "Политика без обязательного основания"
        ))).isEqualTo(400);
        assertThat(status("admin", "PATCH", "/risk-policy", Map.of(
                "version", "it-policy-too-soft",
                "warningThreshold", 60,
                "blockThreshold", 95,
                "formulaDescription", "Недопустимо мягкий блокирующий порог",
                "changeReason", "Попытка смягчить политику под добычные квоты",
                "validatedScenarios", "normal launch, warning launch, blocking risk, degraded telemetry, incident, CHOAM insurance",
                "choamImpact", "CHOAM must still receive traceable policy versions"
        ))).isEqualTo(400);

        Map<String, Object> harvester = post("dispatcher", "/harvesters", Map.of(
                "name", "HV-IT-RISKLIST",
                "type", "HEAVY",
                "status", "READY",
                "noiseLevel", 0.55,
                "capacity", 140
        ));
        Map<String, Object> crew = post("dispatcher", "/crews", Map.of(
                "name", "Экипаж IT RiskList",
                "status", "READY",
                "contactChannel", "it-risklist",
                "memberCount", 5,
                "assignedLogin", "crew"
        ));
        Map<String, Object> mission = post("dispatcher", "/missions", Map.of(
                "title", "Рейс проверки RiskList",
                "zoneId", 2,
                "harvesterId", id(harvester),
                "crewId", id(crew),
                "plannedStart", Instant.now().plusSeconds(3600).toString(),
                "plannedEnd", Instant.now().plusSeconds(10800).toString(),
                "route", List.of(
                        Map.of("seqNo", 1, "lat", 22.11, "lon", 53.91),
                        Map.of("seqNo", 2, "lat", 22.30, "lon", 54.10)
                )
        ));
        long missionId = id(mission);
        post("dispatcher", "/missions/" + missionId + "/risk-assessments", Map.of());
        post("dispatcher", "/missions/" + missionId + "/launch", Map.of(
                "confirmWarning", true,
                "reason", "Запуск для проверки деградации телеметрии"
        ));

        Map<String, Object> staleTelemetry = post("crew", "/missions/" + missionId + "/telemetry", Map.of(
                "externalEventId", "it-risklist-stale-telemetry",
                "eventTime", Instant.now().minusSeconds(900).toString(),
                "lat", 22.21,
                "lon", 54.01,
                "equipmentStatus", "NORMAL"
        ));
        assertThat(staleTelemetry.get("status")).isEqualTo("STALE");
        assertThat(staleTelemetry.get("riskMarkedStale")).isEqualTo(true);
        Map<String, Object> missionAfterStaleTelemetry = getJson("dispatcher", "/missions/" + missionId);
        assertThat(((Number) missionAfterStaleTelemetry.get("monitoringPriority")).intValue()).isGreaterThanOrEqualTo(70);
        assertThat((String) missionAfterStaleTelemetry.get("monitoringContext")).contains("устарела");

        Map<String, Object> staleRisk = post("dispatcher", "/missions/" + missionId + "/risk-assessments", Map.of());
        assertThat(staleRisk.get("dataQuality")).isEqualTo("STALE");
        @SuppressWarnings("unchecked")
        Map<String, Object> staleFactors = (Map<String, Object>) staleRisk.get("factors");
        assertThat(((Number) staleFactors.get("telemetryQualityPenalty")).doubleValue()).isGreaterThanOrEqualTo(0.45);

        Map<String, Object> alarm = post("admin", "/missions/" + missionId + "/alarms", Map.of(
                "externalEventId", "it-risklist-critical-alarm",
                "eventTime", Instant.now().toString(),
                "reason", "Проверка обязательного подтверждения эвакуации"
        ));
        long incidentId = ((Number) alarm.get("incidentId")).longValue();
        patch("security", "/incidents/" + incidentId + "/classification", Map.of(
                "severity", "CRITICAL",
                "reason", "Критичный сценарий RiskList"
        ));
        assertThat(status("security", "POST", "/incidents/" + incidentId + "/close", Map.of())).isEqualTo(400);

        Map<String, Object> evacuation = post("security", "/incidents/" + incidentId + "/evacuation", Map.of(
                "reason", "Проверка отмены команды эвакуации"
        ));
        assertThat(evacuation.get("status")).isEqualTo("SENT");
        Map<String, Object> cancelled = post("security", "/incidents/" + incidentId + "/evacuation/cancel", Map.of(
                "reason", "Учебная отмена команды до подтверждения экипажа"
        ));
        assertThat(cancelled.get("status")).isEqualTo("CANCELLED");
        assertThat(status("security", "POST", "/incidents/" + incidentId + "/close", Map.of())).isEqualTo(400);

        post("security", "/incidents/" + incidentId + "/evacuation", Map.of(
                "reason", "Повторная команда после отмены"
        ));
        post("admin", "/incidents/" + incidentId + "/evacuation/delivered", Map.of());
        post("admin", "/incidents/" + incidentId + "/evacuation/ack", Map.of());
        Map<String, Object> closedIncident = post("security", "/incidents/" + incidentId + "/close", Map.of());
        assertThat(closedIncident.get("status")).isEqualTo("CLOSED");
    }

    private Map<String, Object> post(String actor, String path, Object body) {
        return jsonRequest(actor, "POST", path, body);
    }

    private Map<String, Object> patch(String actor, String path, Object body) {
        return jsonRequest(actor, "PATCH", path, body);
    }

    private Map<String, Object> jsonRequest(String actor, String method, String path, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path)))
                    .header("Content-Type", "application/json");
            if (!path.equals("/auth/login")) {
                builder.header("Authorization", "Bearer " + token(actor));
            }
            HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isBetween(200, 299);
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP request failed: " + method + " " + path, error);
        }
    }

    private int status(String actor, String method, String path, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token(actor));
            HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP request failed: " + method + " " + path, error);
        }
    }

    private int statusGet(String actor, String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                    .header("Authorization", "Bearer " + token(actor))
                    .GET()
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP request failed: GET " + path, error);
        }
    }

    private String get(String actor, String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                    .header("Authorization", "Bearer " + token(actor))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isBetween(200, 299);
            return response.body();
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("HTTP request failed: GET " + path, error);
        }
    }

    private Map<String, Object> getJson(String actor, String path) {
        try {
            return objectMapper.readValue(get(actor, path), new TypeReference<>() {
            });
        } catch (IOException error) {
            throw new AssertionError("HTTP response parsing failed: GET " + path, error);
        }
    }

    private long firstId(String actor, String path) {
        try {
            List<Map<String, Object>> rows = list(actor, path);
            assertThat(rows).as(path).isNotEmpty();
            return ((Number) rows.get(0).get("id")).longValue();
        } catch (RuntimeException error) {
            throw error;
        }
    }

    private List<Map<String, Object>> list(String actor, String path) {
        try {
            return objectMapper.readValue(get(actor, path), new TypeReference<>() {
            });
        } catch (IOException error) {
            throw new AssertionError("HTTP response parsing failed: GET " + path, error);
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }

    private String token(String actor) {
        return tokens.computeIfAbsent(actor, login -> {
            Map<String, Object> response = jsonRequest(login, "POST", "/auth/login", Map.of(
                    "login", login,
                    "password", "hsms123"
            ));
            return (String) response.get("token");
        });
    }

    private long id(Map<String, Object> map) {
        return ((Number) map.get("id")).longValue();
    }

    private void assertRoutePoint(Map<String, Object> mission, int index, int seqNo, double lat, double lon) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> route = (List<Map<String, Object>>) mission.get("route");
        assertThat(route).hasSizeGreaterThan(index);
        Map<String, Object> point = route.get(index);
        assertThat(((Number) point.get("seqNo")).intValue()).isEqualTo(seqNo);
        assertThat(((Number) point.get("lat")).doubleValue()).isEqualTo(lat);
        assertThat(((Number) point.get("lon")).doubleValue()).isEqualTo(lon);
    }
}

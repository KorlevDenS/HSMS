package com.hsms.backend.api_gateway;

import com.hsms.backend.auth.api.AuthApi;
import com.hsms.backend.auth.api.RoleResponse;
import com.hsms.backend.common.HsmsDomain.*;
import com.hsms.backend.common.RealtimeEventService;
import com.hsms.backend.mission.api.MissionApi;
import com.hsms.backend.harvester.api.HarvesterApi;
import com.hsms.backend.insurance.api.InsuranceApi;
import com.hsms.backend.reporting.api.ReportingApi;
import com.hsms.backend.risk.api.RiskApi;
import com.hsms.backend.security.api.SecurityApi;
import com.hsms.backend.simulation.api.SimulationApi;
import com.hsms.backend.security.auth.HsmsPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class HsmsApiController {

    private final AuthApi authApi;
    private final MissionApi missionApi;
    private final HarvesterApi harvesterApi;
    private final RiskApi riskApi;
    private final SecurityApi securityApi;
    private final InsuranceApi insuranceApi;
    private final ReportingApi reportingApi;
    private final SimulationApi simulationApi;
    private final RealtimeEventService realtimeEventService;
    private final HsmsMissionWorkflowService missionWorkflowService;

    public HsmsApiController(
            AuthApi authApi,
            MissionApi missionApi,
            HarvesterApi harvesterApi,
            RiskApi riskApi,
            SecurityApi securityApi,
            InsuranceApi insuranceApi,
            ReportingApi reportingApi,
            SimulationApi simulationApi,
            RealtimeEventService realtimeEventService,
            HsmsMissionWorkflowService missionWorkflowService
    ) {
        this.authApi = authApi;
        this.missionApi = missionApi;
        this.harvesterApi = harvesterApi;
        this.riskApi = riskApi;
        this.securityApi = securityApi;
        this.insuranceApi = insuranceApi;
        this.reportingApi = reportingApi;
        this.simulationApi = simulationApi;
        this.realtimeEventService = realtimeEventService;
        this.missionWorkflowService = missionWorkflowService;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody(required = false) LoginRequest request) {
        return authApi.login(request);
    }

    @GetMapping("/auth/me")
    public HsmsUserDto me(Authentication authentication) {
        return authApi.currentUser(actor(authentication));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public List<RoleResponse> roles() {
        return authApi.getAllRoles();
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public HsmsUserDto createUser(Authentication authentication, @RequestBody UserCreateRequest request) {
        return authApi.createUser(actor(authentication), request);
    }

    @PatchMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public HsmsUserDto updateUserRoles(Authentication authentication, @PathVariable long userId, @RequestBody UserRoleUpdateRequest request) {
        return authApi.updateUserRoles(actor(authentication), userId, request);
    }

    @GetMapping("/bootstrap")
    public BootstrapDto bootstrap(Authentication authentication) {
        return authApi.bootstrap(actor(authentication));
    }

    @GetMapping("/dashboard/operations")
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public DashboardDto dashboard(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return authApi.dashboard(from, to);
    }

    @GetMapping("/harvesters")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<HarvesterDto> harvesters() {
        return harvesterApi.harvesters();
    }

    @PostMapping("/harvesters")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public HarvesterDto createHarvester(Authentication authentication, @RequestBody HarvesterCreateRequest request) {
        return harvesterApi.createHarvester(actor(authentication), request);
    }

    @GetMapping("/harvesters/free")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<HarvesterDto> freeHarvesters() {
        return harvesterApi.freeHarvesters();
    }

    @GetMapping("/harvesters/{harvesterId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public HarvesterDto harvester(@PathVariable long harvesterId) {
        return harvesterApi.harvester(harvesterId);
    }

    @GetMapping("/crews")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<CrewDto> crews() {
        return harvesterApi.crews();
    }

    @PostMapping("/crews")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public CrewDto createCrew(Authentication authentication, @RequestBody CrewCreateRequest request) {
        return harvesterApi.createCrew(actor(authentication), request);
    }

    @GetMapping("/crews/free")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<CrewDto> freeCrews() {
        return harvesterApi.freeCrews();
    }

    @GetMapping("/crews/{crewId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public CrewDto crew(@PathVariable long crewId) {
        return harvesterApi.crew(crewId);
    }

    @GetMapping("/missions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public List<MissionDto> missions() {
        return missionApi.missions();
    }

    @PostMapping("/missions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public MissionDto createMission(
            Authentication authentication,
            @RequestBody MissionCreateRequest request
    ) {
        return missionApi.createMission(actor(authentication), request);
    }

    @GetMapping("/missions/{missionId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_HARVESTER_CREW','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public MissionDto mission(@PathVariable long missionId) {
        return missionApi.mission(missionId);
    }

    @PatchMapping("/missions/{missionId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public MissionDto updateMission(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody MissionPatchRequest request
    ) {
        String actor = actor(authentication);
        return missionWorkflowService.updateMission(actor, missionId, request);
    }

    @GetMapping("/missions/{missionId}/timeline")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public MissionTimelineDto missionTimeline(@PathVariable long missionId) {
        return missionApi.missionTimeline(missionId);
    }

    @PostMapping("/missions/{missionId}/risk-assessments")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public RiskSnapshotDto assessRisk(
            Authentication authentication,
            @PathVariable long missionId
    ) {
        String actor = actor(authentication);
        return missionWorkflowService.assessRisk(actor, missionId);
    }

    @PatchMapping("/risk-policy")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public RiskPolicyDto updateRiskPolicy(
            Authentication authentication,
            @RequestBody RiskPolicyUpdateRequest request
    ) {
        return riskApi.updateRiskPolicy(actor(authentication), request);
    }

    @PostMapping("/missions/{missionId}/launch")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public MissionDto launchMission(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody(required = false) LaunchRequest request
    ) {
        return missionApi.launchMission(actor(authentication), missionId, request);
    }

    @PostMapping("/missions/{missionId}/risk-cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public MissionDto riskCancelMission(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody(required = false) RiskCancelRequest request
    ) {
        String actor = actor(authentication);
        return missionWorkflowService.riskCancelMission(actor, missionId, request);
    }

    @GetMapping("/missions/{missionId}/plan")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public MissionPlanDto missionPlan(Authentication authentication, @PathVariable long missionId) {
        return missionApi.missionPlan(actor(authentication), missionId);
    }

    @PostMapping("/missions/{missionId}/plan/ack")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public MissionPlanDto acknowledgePlan(
            Authentication authentication,
            @PathVariable long missionId
    ) {
        return missionApi.acknowledgePlan(actor(authentication), missionId);
    }

    @PostMapping("/missions/{missionId}/telemetry")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public TelemetryResponse submitTelemetry(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody TelemetryRequest request
    ) {
        return harvesterApi.submitTelemetry(actor(authentication), missionId, request);
    }

    @GetMapping("/missions/{missionId}/telemetry")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public List<TelemetryEventDto> telemetry(@PathVariable long missionId) {
        return harvesterApi.telemetry(missionId);
    }

    @GetMapping("/missions/{missionId}/telemetry/latest")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public TelemetryEventDto latestTelemetry(@PathVariable long missionId) {
        return harvesterApi.latestTelemetry(missionId);
    }

    @PostMapping("/missions/{missionId}/alarms")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public AlarmResponse submitAlarm(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody AlarmRequest request
    ) {
        return securityApi.submitAlarm(actor(authentication), missionId, request);
    }

    @PostMapping("/missions/{missionId}/report")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public MissionDto submitReport(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody MissionReportRequest request
    ) {
        return missionApi.submitMissionReport(actor(authentication), missionId, request);
    }

    @PostMapping("/missions/{missionId}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public MissionDto closeMission(
            Authentication authentication,
            @PathVariable long missionId,
            @RequestBody(required = false) MissionCloseRequest request
    ) {
        String actor = actor(authentication);
        return missionWorkflowService.closeMission(actor, missionId, request);
    }

    @GetMapping("/incidents/queue")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public List<IncidentDto> incidentQueue() {
        return securityApi.incidentQueue();
    }

    @GetMapping("/incidents/{incidentId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_SUPPLY_MANAGER','ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public IncidentDto incident(@PathVariable long incidentId) {
        return securityApi.incident(incidentId);
    }

    @PatchMapping("/incidents/{incidentId}/classification")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public IncidentDto classifyIncident(
            Authentication authentication,
            @PathVariable long incidentId,
            @RequestBody ClassificationRequest request
    ) {
        return securityApi.classifyIncident(actor(authentication), incidentId, request);
    }

    @PostMapping("/incidents/{incidentId}/evacuation")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public EvacuationCommandDto issueEvacuation(
            Authentication authentication,
            @PathVariable long incidentId,
            @RequestBody(required = false) EvacuationRequest request
    ) {
        return securityApi.issueEvacuation(actor(authentication), incidentId, request);
    }

    @PostMapping("/incidents/{incidentId}/evacuation/delivered")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public EvacuationCommandDto markEvacuationDelivered(
            Authentication authentication,
            @PathVariable long incidentId
    ) {
        return securityApi.markEvacuationDelivered(actor(authentication), incidentId);
    }

    @PostMapping("/incidents/{incidentId}/evacuation/ack")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public EvacuationCommandDto acknowledgeEvacuation(
            Authentication authentication,
            @PathVariable long incidentId
    ) {
        return securityApi.acknowledgeEvacuation(actor(authentication), incidentId);
    }

    @PostMapping("/incidents/{incidentId}/evacuation/delivery-failed")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public EvacuationCommandDto markEvacuationDeliveryFailed(
            Authentication authentication,
            @PathVariable long incidentId,
            @RequestBody(required = false) EvacuationRequest request
    ) {
        return securityApi.markEvacuationDeliveryFailed(actor(authentication), incidentId, request);
    }

    @PostMapping("/incidents/{incidentId}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public IncidentDto closeIncident(
            Authentication authentication,
            @PathVariable long incidentId
    ) {
        return securityApi.closeIncident(actor(authentication), incidentId);
    }

    @GetMapping("/insurance-cases")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public List<InsuranceCaseDto> insuranceCases() {
        return insuranceApi.insuranceCases();
    }

    @GetMapping("/insurance-cases/{caseId}")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto insuranceCase(@PathVariable long caseId) {
        return insuranceApi.insuranceCase(caseId);
    }

    @GetMapping("/insurance-cases/{caseId}/history")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public List<InsuranceRecalculationDto> insuranceHistory(@PathVariable long caseId) {
        return insuranceApi.insuranceHistory(caseId);
    }

    @PostMapping("/insurance-cases/{caseId}/recalculate")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto recalculateInsurance(
            Authentication authentication,
            @PathVariable long caseId,
            @RequestBody(required = false) InsuranceRecalculateRequest request
    ) {
        return insuranceApi.recalculateInsurance(actor(authentication), caseId, request);
    }

    @PatchMapping("/insurance-cases/{caseId}/terms")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto updateInsuranceTerms(
            Authentication authentication,
            @PathVariable long caseId,
            @RequestBody InsuranceTermsRequest request
    ) {
        return insuranceApi.updateInsuranceTerms(actor(authentication), caseId, request);
    }

    @PostMapping("/insurance-cases/{caseId}/reject-recalculation")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto rejectInsurance(
            Authentication authentication,
            @PathVariable long caseId,
            @RequestBody(required = false) InsuranceRejectRequest request
    ) {
        return insuranceApi.rejectInsuranceRecalculation(actor(authentication), caseId, request);
    }

    @PostMapping("/insurance-cases/{caseId}/close")
    @PreAuthorize("hasAnyAuthority('ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_ADMINISTRATOR')")
    public InsuranceCaseDto closeInsurance(
            Authentication authentication,
            @PathVariable long caseId,
            @RequestBody(required = false) InsuranceCloseRequest request
    ) {
        return insuranceApi.closeInsuranceCase(actor(authentication), caseId, request);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyAuthority('ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public List<AuditEventDto> audit() {
        return authApi.auditSnapshot();
    }

    @GetMapping("/incidents/stream")
    @PreAuthorize("isAuthenticated()")
    public SseEmitter events(Authentication authentication) {
        return realtimeEventService.subscribe(actor(authentication));
    }

    @PostMapping("/simulation/desert-scenarios")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public DesertSimulationResult runDesertSimulation(
            Authentication authentication,
            @RequestBody(required = false) DesertSimulationRequest request
    ) {
        return simulationApi.runDesertScenario(actor(authentication), request);
    }

    @GetMapping(value = "/reports/missions.csv", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public ResponseEntity<String> missionReportCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=missions.csv")
                .contentType(MediaType.valueOf("text/csv; charset=UTF-8"))
                .body(reportingApi.missionReportCsv());
    }

    @GetMapping(value = "/reports/incidents.csv", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasAnyAuthority('ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public ResponseEntity<String> incidentReportCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incidents.csv")
                .contentType(MediaType.valueOf("text/csv; charset=UTF-8"))
                .body(reportingApi.incidentReportCsv());
    }

    private String actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof HsmsPrincipal principal) {
            return principal.login();
        }
        throw new IllegalStateException("Authenticated HSMS principal is missing");
    }
}

package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.api_gateway.HsmsMissionWorkflowService;
import com.hsms.backend.common.LaunchRequest;
import com.hsms.backend.common.MissionCloseRequest;
import com.hsms.backend.common.MissionCreateRequest;
import com.hsms.backend.common.MissionDto;
import com.hsms.backend.common.MissionPatchRequest;
import com.hsms.backend.common.MissionPlanDto;
import com.hsms.backend.common.MissionReportRequest;
import com.hsms.backend.common.MissionTimelineDto;
import com.hsms.backend.common.RiskCancelRequest;
import com.hsms.backend.common.RiskSnapshotDto;
import com.hsms.backend.mission.api.MissionApi;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MissionController extends HsmsControllerSupport {

    private final MissionApi missionApi;
    private final HsmsMissionWorkflowService missionWorkflowService;

    public MissionController(MissionApi missionApi, HsmsMissionWorkflowService missionWorkflowService) {
        this.missionApi = missionApi;
        this.missionWorkflowService = missionWorkflowService;
    }

    @GetMapping("/missions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public List<MissionDto> missions() {
        return missionApi.missions();
    }

    @PostMapping("/missions")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public MissionDto createMission(Authentication authentication, @RequestBody MissionCreateRequest request) {
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
        return missionWorkflowService.updateMission(actor(authentication), missionId, request);
    }

    @GetMapping("/missions/{missionId}/timeline")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_INSURANCE_CONTOUR_OPERATOR','ROLE_OPERATIONS_MANAGEMENT','ROLE_ADMINISTRATOR')")
    public MissionTimelineDto missionTimeline(@PathVariable long missionId) {
        return missionApi.missionTimeline(missionId);
    }

    @PostMapping("/missions/{missionId}/risk-assessments")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPPLY_MANAGER','ROLE_SECURITY_HEADQUARTERS_OPERATOR','ROLE_ADMINISTRATOR')")
    public RiskSnapshotDto assessRisk(Authentication authentication, @PathVariable long missionId) {
        return missionWorkflowService.assessRisk(actor(authentication), missionId);
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
        return missionWorkflowService.riskCancelMission(actor(authentication), missionId, request);
    }

    @GetMapping("/missions/{missionId}/plan")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_SUPPLY_MANAGER','ROLE_ADMINISTRATOR')")
    public MissionPlanDto missionPlan(Authentication authentication, @PathVariable long missionId) {
        return missionApi.missionPlan(actor(authentication), missionId);
    }

    @PostMapping("/missions/{missionId}/plan/ack")
    @PreAuthorize("hasAnyAuthority('ROLE_HARVESTER_CREW','ROLE_ADMINISTRATOR')")
    public MissionPlanDto acknowledgePlan(Authentication authentication, @PathVariable long missionId) {
        return missionApi.acknowledgePlan(actor(authentication), missionId);
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
        return missionWorkflowService.closeMission(actor(authentication), missionId, request);
    }
}

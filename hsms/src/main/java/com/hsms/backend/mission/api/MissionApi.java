package com.hsms.backend.mission.api;

import com.hsms.backend.common.LaunchRequest;
import com.hsms.backend.common.MissionCloseRequest;
import com.hsms.backend.common.MissionCreateRequest;
import com.hsms.backend.common.MissionDto;
import com.hsms.backend.common.MissionPatchRequest;
import com.hsms.backend.common.MissionPlanDto;
import com.hsms.backend.common.MissionReportRequest;
import com.hsms.backend.common.MissionTimelineDto;
import com.hsms.backend.common.RiskCancelRequest;

import java.time.Instant;
import java.util.List;

public interface MissionApi {

    boolean canAccessMission(Long missionId, Long userId);

    boolean isMissionOfThisCrew(Long missionId, Long userId);

    List<MissionDto> missions();

    MissionDto mission(long missionId);

    MissionDto createMission(String actorLogin, MissionCreateRequest request);

    MissionDto updateMission(String actorLogin, long missionId, MissionPatchRequest request);

    MissionDto launchMission(String actorLogin, long missionId, LaunchRequest request);

    MissionDto recordRiskAssessment(String actorLogin, long missionId, long riskSnapshotId, Instant calculatedAt);

    MissionDto riskCancelMission(String actorLogin, long missionId, RiskCancelRequest request);

    MissionPlanDto missionPlan(long missionId);

    MissionPlanDto missionPlan(String actorLogin, long missionId);

    MissionPlanDto acknowledgePlan(String actorLogin, long missionId);

    MissionDto submitMissionReport(String actorLogin, long missionId, MissionReportRequest request);

    MissionDto closeMission(String actorLogin, long missionId, MissionCloseRequest request);

    MissionTimelineDto missionTimeline(long missionId);

    String missionReportCsv();
}

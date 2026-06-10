package com.hsms.backend.mission.api;

import com.hsms.backend.common.HsmsDomain.LaunchRequest;
import com.hsms.backend.common.HsmsDomain.MissionCloseRequest;
import com.hsms.backend.common.HsmsDomain.MissionCreateRequest;
import com.hsms.backend.common.HsmsDomain.MissionDto;
import com.hsms.backend.common.HsmsDomain.MissionPatchRequest;
import com.hsms.backend.common.HsmsDomain.MissionPlanDto;
import com.hsms.backend.common.HsmsDomain.MissionReportRequest;
import com.hsms.backend.common.HsmsDomain.MissionTimelineDto;
import com.hsms.backend.common.HsmsDomain.RiskCancelRequest;

import java.time.Instant;
import java.util.List;

public interface MissionApi {
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

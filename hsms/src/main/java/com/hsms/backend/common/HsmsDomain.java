package com.hsms.backend.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HsmsDomain {

    private HsmsDomain() {
    }

    public enum RoleCode {
        ROLE_SUPPLY_MANAGER,
        ROLE_HARVESTER_CREW,
        ROLE_INSURANCE_CONTOUR_OPERATOR,
        ROLE_SECURITY_HEADQUARTERS_OPERATOR,
        ROLE_ADMINISTRATOR,
        ROLE_OPERATIONS_MANAGEMENT
    }

    public enum ResourceStatus {
        READY,
        BUSY,
        MAINTENANCE,
        LOST
    }

    public enum MissionStatus {
        DRAFT,
        READY_FOR_RISK,
        RISK_ASSESSED,
        ACTIVE,
        RISK_CANCELLED,
        CANCELLED,
        COMPLETED_PENDING_CLOSE,
        LOST,
        CLOSED
    }

    public enum DecisionZone {
        ALLOWED,
        WARNING,
        BLOCKING
    }

    public enum DataQuality {
        FRESH,
        DEGRADED,
        STALE
    }

    public enum FreshnessStatus {
        ACCEPTED,
        DUPLICATE,
        STALE,
        REJECTED
    }

    public enum IncidentStatus {
        OPEN,
        CLASSIFIED,
        EVACUATION_ORDERED,
        EVACUATION_ACKNOWLEDGED,
        MONITORING,
        CLOSED
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum EvacuationStatus {
        CREATED,
        SENT,
        DELIVERED,
        ACKNOWLEDGED,
        DELIVERY_FAILED,
        EXPIRED,
        CANCELLED
    }

    public enum InsuranceStatus {
        OPEN,
        WAITING_FOR_DATA,
        READY_FOR_RECALCULATION,
        RECALCULATED,
        TERMS_UPDATED,
        REJECTED,
        CLOSED
    }

    public enum InsuranceTrigger {
        RISK_CANCELLATION,
        INCIDENT,
        SLA_BREACH,
        MISSION_LOSS,
        MISSION_CLOSE
    }

    public enum InsuranceHistoryEvent {
        RECALCULATION,
        TERMS_UPDATED
    }

    public record HsmsUserDto(
            long id,
            String login,
            String displayName,
            String email,
            String phone,
            Set<RoleCode> roles
    ) {
    }

    public record LoginRequest(String login, String password) {
    }

    public record LoginResponse(String token, HsmsUserDto user) {
    }

    public record UserCreateRequest(
            String login,
            String password,
            String displayName,
            String email,
            String phone,
            Set<RoleCode> roles
    ) {
    }

    public record UserRoleUpdateRequest(Set<RoleCode> roles) {
    }

    public record MiningZoneDto(long id, String name, double riskLevel, String coordinates, boolean active) {
    }

    public record HarvesterDto(
            long id,
            String name,
            String type,
            ResourceStatus status,
            double noiseLevel,
            int capacity
    ) {
    }

    public record HarvesterCreateRequest(
            String name,
            String type,
            ResourceStatus status,
            Double noiseLevel,
            Integer capacity
    ) {
    }

    public record CrewDto(long id, String name, ResourceStatus status, String contactChannel, int memberCount, String assignedLogin) {
    }

    public record CrewCreateRequest(
            String name,
            ResourceStatus status,
            String contactChannel,
            Integer memberCount,
            String assignedLogin
    ) {
    }

    public record RoutePointDto(int seqNo, double lat, double lon) {
    }

    public static class MissionCreateRequest {
        public String title;
        public Long zoneId;
        public Long harvesterId;
        public Long crewId;
        public Instant plannedStart;
        public Instant plannedEnd;
        public List<RoutePointDto> route;

        public MissionCreateRequest() {
        }

        public String title() {
            return title;
        }

        public Long zoneId() {
            return zoneId;
        }

        public Long harvesterId() {
            return harvesterId;
        }

        public Long crewId() {
            return crewId;
        }

        public Instant plannedStart() {
            return plannedStart;
        }

        public Instant plannedEnd() {
            return plannedEnd;
        }

        public List<RoutePointDto> route() {
            return route;
        }
    }

    public static class MissionPatchRequest {
        public String title;
        public Long zoneId;
        public Long harvesterId;
        public Long crewId;
        public Instant plannedStart;
        public Instant plannedEnd;
        public List<RoutePointDto> route;

        public MissionPatchRequest() {
        }

        public String title() {
            return title;
        }

        public Long zoneId() {
            return zoneId;
        }

        public Long harvesterId() {
            return harvesterId;
        }

        public Long crewId() {
            return crewId;
        }

        public Instant plannedStart() {
            return plannedStart;
        }

        public Instant plannedEnd() {
            return plannedEnd;
        }

        public List<RoutePointDto> route() {
            return route;
        }
    }

    public record MissionDto(
            long id,
            String title,
            MissionStatus status,
            Long zoneId,
            String zoneName,
            Long harvesterId,
            String harvesterName,
            Long crewId,
            String crewName,
            Instant plannedStart,
            Instant plannedEnd,
            Instant actualStart,
            Instant closedAt,
            String closeReason,
            int routeVersion,
            List<RoutePointDto> route,
            RiskSnapshotDto risk,
            MissionPlanDto plan,
            MissionReportDto report,
            List<Long> incidentIds,
            Long insuranceCaseId,
            String closedBy,
            String draftMissingFields,
            int monitoringPriority,
            String monitoringContext,
            Instant riskReviewRequiredAt,
            String riskReviewReason,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record MissionPlanDto(
            long id,
            long missionId,
            int routeVersion,
            String safetyContact,
            Instant publishedAt,
            Instant acknowledgedAt,
            String acknowledgedBy
    ) {
    }

    public record LaunchRequest(boolean confirmWarning, String reason) {
    }

    public record RiskCancelRequest(String reason) {
    }

    public record RiskPolicyDto(
            long id,
            String version,
            int warningThreshold,
            int blockThreshold,
            String formulaDescription,
            Instant activeFrom
    ) {
    }

    public record RiskPolicyUpdateRequest(
            String version,
            Integer warningThreshold,
            Integer blockThreshold,
            String formulaDescription
    ) {
    }

    public record RiskSnapshotDto(
            long id,
            long missionId,
            String policyVersion,
            double pAttack,
            int riskScore,
            boolean launchAllowed,
            DecisionZone decisionZone,
            String blockingReason,
            Map<String, Double> factors,
            DataQuality dataQuality,
            Instant calculatedAt,
            int validForRouteVersion,
            boolean stale,
            String staleReason
    ) {
    }

    public record TelemetryRequest(
            String externalEventId,
            Instant eventTime,
            double lat,
            double lon,
            String equipmentStatus
    ) {
    }

    public record TelemetryResponse(
            long id,
            FreshnessStatus status,
            boolean riskMarkedStale,
            TelemetryEventDto event
    ) {
    }

    public record TelemetryEventDto(
            long id,
            String externalEventId,
            long missionId,
            long crewId,
            double lat,
            double lon,
            String equipmentStatus,
            Instant eventTime,
            Instant receivedAt,
            Instant processedAt,
            FreshnessStatus freshnessStatus
    ) {
    }

    public record AlarmRequest(String externalEventId, Instant eventTime, String reason) {
    }

    public record AlarmResponse(long alarmId, long incidentId, String acknowledgement, IncidentDto incident) {
    }

    public record IncidentDto(
            long id,
            long missionId,
            long alarmSignalId,
            IncidentStatus status,
            Severity severity,
            String classificationReason,
            Instant slaStartedAt,
            Instant slaDeadlineAt,
            boolean slaBreached,
            Instant closedAt,
            String closedBy,
            EvacuationCommandDto evacuationCommand
    ) {
    }

    public record ClassificationRequest(Severity severity, String reason) {
    }

    public record EvacuationRequest(String reason) {
    }

    public record EvacuationCommandDto(
            long id,
            long incidentId,
            long missionId,
            EvacuationStatus status,
            Instant sentAt,
            Instant deliveredAt,
            String sentBy,
            Instant acknowledgedAt,
            String acknowledgedBy,
            Instant expiresAt,
            String deliveryError
    ) {
    }

    public record MissionReportRequest(
            Instant actualStart,
            Instant actualEnd,
            BigDecimal spiceAmount,
            String harvesterFinalStatus,
            String abnormalSituations
    ) {
    }

    public record MissionReportDto(
            long id,
            long missionId,
            Instant actualStart,
            Instant actualEnd,
            BigDecimal spiceAmount,
            String harvesterFinalStatus,
            String abnormalSituations,
            String submittedBy,
            Instant submittedAt
    ) {
    }

    public record MissionCloseRequest(MissionStatus finalStatus, String reason) {
    }

    public record InsuranceCaseDto(
            long id,
            long missionId,
            Long incidentId,
            InsuranceStatus status,
            InsuranceTrigger triggerType,
            Instant openedAt,
            String openedBy,
            String triggerReason,
            Double triggerPAttack,
            Integer triggerRiskScore,
            Long triggerRiskSnapshotId,
            Instant triggerDecisionAt,
            String triggerDecisionBy,
            Severity incidentSeverity,
            Instant incidentRegisteredAt,
            Instant incidentClosedAt,
            Boolean incidentSlaBreached,
            String incidentOperator,
            String missingData,
            Integer finalRiskScore,
            BigDecimal finalPremium,
            Instant closedAt,
            String closedBy,
            List<InsuranceRecalculationDto> history
    ) {
    }

    public record InsuranceRecalculateRequest(String reason) {
    }

    public record InsuranceTermsRequest(BigDecimal premium, String reason) {
    }

    public record InsuranceRejectRequest(String reason) {
    }

    public record InsuranceCloseRequest(String reason) {
    }

    public record InsuranceCaseOpenRequest(
            long missionId,
            Long incidentId,
            InsuranceTrigger trigger,
            String reason,
            Double pAttack,
            Integer riskScore,
            Long riskSnapshotId,
            Instant decisionAt,
            String decisionBy,
            Severity incidentSeverity,
            Instant incidentRegisteredAt,
            Instant incidentClosedAt,
            Boolean incidentSlaBreached,
            String incidentOperator,
            MissionStatus missionStatus
    ) {
    }

    public record InsuranceRecalculationDto(
            long id,
            long insuranceCaseId,
            InsuranceHistoryEvent eventType,
            Long riskSnapshotId,
            BigDecimal oldPremium,
            BigDecimal newPremium,
            Integer oldRiskScore,
            Integer newRiskScore,
            String reason,
            Instant calculatedAt,
            String calculatedBy,
            String rejectedReason
    ) {
    }

    public record AuditEventDto(
            long id,
            String actorLogin,
            RoleCode actorRole,
            String action,
            String objectType,
            long objectId,
            Long missionId,
            Instant createdAt,
            Map<String, Object> details
    ) {
    }

    public record DashboardDto(
            long activeMissions,
            long closedMissions,
            long cancelledMissions,
            long openIncidents,
            long slaBreaches,
            double slaCompliancePercent,
            double averageReactionSeconds,
            long openInsuranceCases,
            BigDecimal averagePremium,
            Map<Severity, Long> incidentsBySeverity,
            Instant periodFrom,
            Instant periodTo
    ) {
    }

    public record BootstrapDto(
            HsmsUserDto user,
            List<HsmsUserDto> users,
            List<MiningZoneDto> zones,
            List<HarvesterDto> harvesters,
            List<CrewDto> crews,
            List<MissionDto> missions,
            List<IncidentDto> incidents,
            List<InsuranceCaseDto> insuranceCases,
            List<AuditEventDto> audit,
            RiskPolicyDto activeRiskPolicy,
            DashboardDto dashboard
    ) {
    }

    public record MissionTimelineDto(
            MissionDto mission,
            List<TelemetryEventDto> telemetry,
            List<IncidentDto> incidents,
            InsuranceCaseDto insuranceCase,
            List<AuditEventDto> audit
    ) {
    }

    public record ApiError(String message, String action, int status, Instant timestamp) {
    }

    public record DesertSimulationRequest(
            long zoneId,
            long harvesterId,
            long crewId,
            int telemetryPoints,
            Severity threatSeverity,
            boolean includeAlarm
    ) {
    }

    public record DesertSimulationResult(
            MissionDto mission,
            List<TelemetryEventDto> telemetry,
            AlarmResponse alarm,
            RiskSnapshotDto risk
    ) {
    }
}

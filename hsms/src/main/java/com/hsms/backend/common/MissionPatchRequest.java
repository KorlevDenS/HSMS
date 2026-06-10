package com.hsms.backend.common;

import java.time.Instant;
import java.util.List;

public final class MissionPatchRequest {
    private String title;
    private Long zoneId;
    private Long harvesterId;
    private Long crewId;
    private Instant plannedStart;
    private Instant plannedEnd;
    private List<RoutePointDto> route;

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long zoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long harvesterId() {
        return harvesterId;
    }

    public void setHarvesterId(Long harvesterId) {
        this.harvesterId = harvesterId;
    }

    public Long crewId() {
        return crewId;
    }

    public void setCrewId(Long crewId) {
        this.crewId = crewId;
    }

    public Instant plannedStart() {
        return plannedStart;
    }

    public void setPlannedStart(Instant plannedStart) {
        this.plannedStart = plannedStart;
    }

    public Instant plannedEnd() {
        return plannedEnd;
    }

    public void setPlannedEnd(Instant plannedEnd) {
        this.plannedEnd = plannedEnd;
    }

    public List<RoutePointDto> route() {
        return DomainCollections.immutableListOrNull(route);
    }

    public void setRoute(List<RoutePointDto> route) {
        this.route = DomainCollections.immutableListOrNull(route);
    }
}

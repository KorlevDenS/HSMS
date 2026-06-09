package com.hsms.backend.harvester.model;

import com.hsms.backend.common.HsmsDomain.FreshnessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "telemetry_event",
        uniqueConstraints = @UniqueConstraint(name = "telemetry_event_mission_external_unique", columnNames = {"mission_id", "external_event_id"})
)
public class TelemetryEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "external_event_id", nullable = false, length = 160)
    private String externalEventId;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(name = "equipment_status", nullable = false, length = 120)
    private String equipmentStatus;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "freshness_status", nullable = false, length = 40)
    private FreshnessStatus freshnessStatus;
}

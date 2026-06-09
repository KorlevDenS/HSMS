package com.hsms.backend.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "alarm_signal",
        uniqueConstraints = @UniqueConstraint(name = "alarm_signal_mission_external_unique", columnNames = {"mission_id", "external_event_id"})
)
public class AlarmSignal {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "external_event_id", nullable = false, length = 160)
    private String externalEventId;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(nullable = false, length = 80)
    private String sender;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;
}

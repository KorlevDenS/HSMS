package com.hsms.backend.security.model;

import com.hsms.backend.common.HsmsDomain.IncidentStatus;
import com.hsms.backend.common.HsmsDomain.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "incident")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "alarm_signal_id")
    private Long alarmSignalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Severity severity;

    @Column(name = "classification_reason", length = 1000)
    private String classificationReason;

    @Column(name = "sla_started_at", nullable = false)
    private Instant slaStartedAt;

    @Column(name = "sla_deadline_at", nullable = false)
    private Instant slaDeadlineAt;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached;

    @Column(name = "sla_breached_notified_at")
    private Instant slaBreachedNotifiedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 80)
    private String closedBy;

    @Column(name = "operator_login", length = 80)
    private String operatorLogin;

    @Column(name = "evacuation_command_id")
    private Long evacuationCommandId;
}

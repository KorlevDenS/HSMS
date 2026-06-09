package com.hsms.backend.mission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "mission_plan")
public class MissionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "mission_id", nullable = false, unique = true)
    private Long missionId;

    @Column(name = "route_version", nullable = false)
    private int routeVersion;

    @Column(name = "safety_contact", nullable = false, length = 160)
    private String safetyContact;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by", length = 80)
    private String acknowledgedBy;
}

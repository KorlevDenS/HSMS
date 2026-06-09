package com.hsms.backend.harvester.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "mission_report")
public class MissionReport {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "mission_id", nullable = false, unique = true)
    private Long missionId;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "actual_end", nullable = false)
    private Instant actualEnd;

    @Column(name = "spice_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal spiceAmount;

    @Column(name = "harvester_final_status", nullable = false, length = 80)
    private String harvesterFinalStatus;

    @Column(name = "abnormal_situations", nullable = false, length = 2000)
    private String abnormalSituations;

    @Column(name = "submitted_by", nullable = false, length = 80)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;
}

package com.hsms.backend.risk.model;

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
@Table(name = "risk_policy")
public class RiskPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String version;

    @Column(name = "warning_threshold", nullable = false)
    private int warningThreshold;

    @Column(name = "block_threshold", nullable = false)
    private int blockThreshold;

    @Column(name = "formula_description", nullable = false, length = 1000)
    private String formulaDescription;

    @Column(name = "active_from", nullable = false)
    private Instant activeFrom;

    @Column(name = "changed_by", nullable = false, length = 80)
    private String changedBy;

    @Column(name = "change_reason", nullable = false, length = 1000)
    private String changeReason;

    @Column(name = "validated_scenarios", nullable = false, length = 1000)
    private String validatedScenarios;

    @Column(name = "choam_impact", nullable = false, length = 1000)
    private String choamImpact;

    @Column(nullable = false)
    private boolean active;
}

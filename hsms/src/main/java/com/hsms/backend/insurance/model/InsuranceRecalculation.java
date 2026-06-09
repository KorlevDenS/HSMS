package com.hsms.backend.insurance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.hsms.backend.common.HsmsDomain.InsuranceHistoryEvent;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "insurance_recalculation")
public class InsuranceRecalculation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insurance_case_id", nullable = false)
    private InsuranceCase insuranceCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private InsuranceHistoryEvent eventType;

    @Column(name = "risk_snapshot_id")
    private Long riskSnapshotId;

    @Column(name = "old_premium", precision = 18, scale = 2)
    private BigDecimal oldPremium;

    @Column(name = "new_premium", nullable = false, precision = 18, scale = 2)
    private BigDecimal newPremium;

    @Column(name = "old_risk_score")
    private Integer oldRiskScore;

    @Column(name = "new_risk_score", nullable = false)
    private Integer newRiskScore;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "calculated_by", nullable = false, length = 80)
    private String calculatedBy;

    @Column(name = "rejected_reason", length = 1000)
    private String rejectedReason;
}

package com.hsms.backend.risk.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "risk_factor",
        uniqueConstraints = @UniqueConstraint(name = "risk_factor_snapshot_name_unique", columnNames = {"risk_snapshot_id", "factor_name"})
)
public class RiskFactor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_snapshot_id", nullable = false)
    private RiskScore riskScore;

    @Column(name = "factor_name", nullable = false, length = 80)
    private String factorName;

    @Column(name = "factor_value", nullable = false)
    private double factorValue;
}

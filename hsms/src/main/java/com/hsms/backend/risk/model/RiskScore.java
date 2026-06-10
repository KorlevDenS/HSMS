package com.hsms.backend.risk.model;

import com.hsms.backend.common.DataQuality;
import com.hsms.backend.common.DecisionZone;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "risk_snapshot")
public class RiskScore {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "policy_version", nullable = false, length = 40)
    private String policyVersion;

    @Column(name = "p_attack", nullable = false)
    private double pAttack;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "launch_allowed", nullable = false)
    private boolean launchAllowed;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_zone", nullable = false, length = 40)
    private DecisionZone decisionZone;

    @Column(name = "blocking_reason", length = 1000)
    private String blockingReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_quality", nullable = false, length = 40)
    private DataQuality dataQuality;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "valid_for_route_version", nullable = false)
    private int validForRouteVersion;

    @Column(nullable = false)
    private boolean stale;

    @Column(name = "stale_reason", length = 1000)
    private String staleReason;

    @OrderBy("factorName ASC")
    @OneToMany(mappedBy = "riskScore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RiskFactor> factors = new ArrayList<>();

    public List<RiskFactor> getFactors() {
        return List.copyOf(factors);
    }

    public void setFactors(List<RiskFactor> factors) {
        this.factors.clear();
        if (factors != null) {
            factors.forEach(factor -> {
                factor.setRiskScore(this);
                this.factors.add(factor);
            });
        }
    }

    public void addFactor(String name, double value) {
        RiskFactor factor = new RiskFactor();
        factor.setRiskScore(this);
        factor.setFactorName(name);
        factor.setFactorValue(value);
        factors.add(factor);
    }
}

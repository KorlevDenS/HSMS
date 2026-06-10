package com.hsms.backend.insurance.model;

import com.hsms.backend.common.HsmsDomain.InsuranceStatus;
import com.hsms.backend.common.HsmsDomain.InsuranceTrigger;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "insurance_case")
public class InsuranceCase {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "incident_id")
    private Long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private InsuranceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 60)
    private InsuranceTrigger triggerType;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "opened_by", nullable = false, length = 80)
    private String openedBy;

    @Column(name = "trigger_reason", length = 1000)
    private String triggerReason;

    @Column(name = "trigger_p_attack")
    private Double triggerPAttack;

    @Column(name = "trigger_risk_score")
    private Integer triggerRiskScore;

    @Column(name = "trigger_risk_snapshot_id")
    private Long triggerRiskSnapshotId;

    @Column(name = "trigger_decision_at")
    private Instant triggerDecisionAt;

    @Column(name = "trigger_decision_by", length = 80)
    private String triggerDecisionBy;

    @Column(name = "incident_severity", length = 40)
    private String incidentSeverity;

    @Column(name = "incident_registered_at")
    private Instant incidentRegisteredAt;

    @Column(name = "incident_closed_at")
    private Instant incidentClosedAt;

    @Column(name = "incident_sla_breached")
    private Boolean incidentSlaBreached;

    @Column(name = "incident_operator", length = 80)
    private String incidentOperator;

    @Column(name = "missing_data", length = 1000)
    private String missingData;

    @Column(name = "final_risk_score")
    private Integer finalRiskScore;

    @Column(name = "final_premium", precision = 18, scale = 2)
    private BigDecimal finalPremium;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 80)
    private String closedBy;

    @OrderBy("calculatedAt DESC, id DESC")
    @OneToMany(mappedBy = "insuranceCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InsuranceRecalculation> recalculations = new ArrayList<>();

    public List<InsuranceRecalculation> getRecalculations() {
        return List.copyOf(recalculations);
    }

    public void setRecalculations(List<InsuranceRecalculation> recalculations) {
        this.recalculations.clear();
        if (recalculations != null) {
            recalculations.forEach(this::addRecalculation);
        }
    }

    public void addRecalculation(InsuranceRecalculation recalculation) {
        recalculation.setInsuranceCase(this);
        recalculations.add(recalculation);
    }
}

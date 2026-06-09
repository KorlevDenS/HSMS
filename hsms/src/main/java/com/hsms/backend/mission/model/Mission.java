package com.hsms.backend.mission.model;

import com.hsms.backend.common.HsmsDomain.MissionStatus;
import com.hsms.backend.common.model.MiningZone;
import com.hsms.backend.harvester.model.Crew;
import com.hsms.backend.harvester.model.Harvester;
import jakarta.persistence.CascadeType;
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
@Table(name = "mission")
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 240)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private MissionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private MiningZone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "harvester_id")
    private Harvester harvester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id")
    private Crew crew;

    @Column(name = "planned_start")
    private Instant plannedStart;

    @Column(name = "planned_end")
    private Instant plannedEnd;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "close_reason", length = 1000)
    private String closeReason;

    @Column(name = "closed_by", length = 80)
    private String closedBy;

    @Column(name = "draft_missing_fields", length = 1000)
    private String draftMissingFields;

    @Column(name = "monitoring_priority", nullable = false)
    private int monitoringPriority;

    @Column(name = "monitoring_context", length = 1000)
    private String monitoringContext;

    @Column(name = "risk_review_required_at")
    private Instant riskReviewRequiredAt;

    @Column(name = "risk_review_reason", length = 1000)
    private String riskReviewReason;

    @Column(name = "route_version", nullable = false)
    private int routeVersion;

    @Column(name = "risk_snapshot_id")
    private Long riskSnapshotId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "insurance_case_id")
    private Long insuranceCaseId;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OrderBy("seqNo ASC")
    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MissionRoute> route = new ArrayList<>();

    public void replaceRoute(List<MissionRoute> points) {
        route.clear();
        points.forEach(point -> {
            point.setMission(this);
            route.add(point);
        });
    }
}

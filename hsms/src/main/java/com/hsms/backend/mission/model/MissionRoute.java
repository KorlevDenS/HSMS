package com.hsms.backend.mission.model;

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
        name = "mission_route_point",
        uniqueConstraints = @UniqueConstraint(name = "mission_route_point_mission_seq_unique", columnNames = {"mission_id", "seq_no"})
)
public class MissionRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "seq_no", nullable = false)
    private int seqNo;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;
}

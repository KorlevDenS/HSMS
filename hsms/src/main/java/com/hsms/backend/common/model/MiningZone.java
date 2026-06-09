package com.hsms.backend.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mining_zone")
public class MiningZone {
    @Id
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "risk_level", nullable = false)
    private double riskLevel;

    @Column(nullable = false, length = 80)
    private String coordinates;

    @Column(nullable = false)
    private boolean active;
}

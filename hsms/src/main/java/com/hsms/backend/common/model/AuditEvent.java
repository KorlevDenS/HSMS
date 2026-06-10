package com.hsms.backend.common.model;

import com.hsms.backend.common.HsmsDomain.RoleCode;
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
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hsms_domain_id_gen")
    @SequenceGenerator(name = "hsms_domain_id_gen", sequenceName = "hsms_domain_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "actor_login", nullable = false, length = 80)
    private String actorLogin;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 80)
    private RoleCode actorRole;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(name = "object_type", nullable = false, length = 80)
    private String objectType;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Column(name = "mission_id")
    private Long missionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OrderBy("id ASC")
    @OneToMany(mappedBy = "auditEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditDetail> details = new ArrayList<>();

    public List<AuditDetail> getDetails() {
        return List.copyOf(details);
    }

    public void setDetails(List<AuditDetail> details) {
        this.details.clear();
        if (details != null) {
            details.forEach(detail -> {
                detail.setAuditEvent(this);
                this.details.add(detail);
            });
        }
    }

    public void addDetail(String key, Object value) {
        AuditDetail detail = new AuditDetail();
        detail.setAuditEvent(this);
        detail.setDetailKey(key);
        detail.setDetailValue(String.valueOf(value));
        details.add(detail);
    }
}

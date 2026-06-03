package com.hsms.backend.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "permission")
public class Permission {
    @EmbeddedId
    @SequenceGenerator(name = "permission_id_gen", sequenceName = "hsms_user_id_seq", allocationSize = 1)
    private PermissionId id;

    @MapsId("client")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "client", nullable = false)
    private HsmsUser client;

    @MapsId("role")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role", nullable = false)
    private Role role;


}
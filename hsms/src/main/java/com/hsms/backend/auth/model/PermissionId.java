package com.hsms.backend.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class PermissionId implements Serializable {

    @Serial
    private static final long serialVersionUID = 6406430028362423915L;

    @NotNull
    @Column(name = "client", nullable = false)
    private Long client;

    @NotNull
    @Column(name = "role", nullable = false)
    private Long role;


}
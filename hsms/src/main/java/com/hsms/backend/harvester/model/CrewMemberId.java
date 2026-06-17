package com.hsms.backend.harvester.model;

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
public class CrewMemberId implements Serializable {
    @Serial
    private static final long serialVersionUID = 4402137841464649197L;
    @NotNull
    @Column(name = "client", nullable = false)
    private Long client;

    @NotNull
    @Column(name = "crew", nullable = false)
    private Long crew;


}
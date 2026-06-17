package com.hsms.backend.harvester.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crew_member")
public class CrewMember {
    @EmbeddedId
    private CrewMemberId id;


}
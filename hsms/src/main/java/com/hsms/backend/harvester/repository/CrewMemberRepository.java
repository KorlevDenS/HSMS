package com.hsms.backend.harvester.repository;

import com.hsms.backend.harvester.model.CrewMember;
import com.hsms.backend.harvester.model.CrewMemberId;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewMemberRepository extends JpaRepository<CrewMember, CrewMemberId> {

    List<CrewMember> findAllByIdClient(@NotNull Long idClient);

    boolean existsByIdClientAndIdCrew(@NotNull Long idClient, @NotNull Long idCrew);
}

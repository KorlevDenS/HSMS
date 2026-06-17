package com.hsms.backend.harvester.repository;

import com.hsms.backend.harvester.model.CrewMember;
import com.hsms.backend.harvester.model.CrewMemberId;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrewMemberRepository extends JpaRepository<CrewMember, CrewMemberId> {

    Optional<CrewMember> findByIdClient(@NotNull Long id_client);
}
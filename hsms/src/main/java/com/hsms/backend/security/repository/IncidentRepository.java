package com.hsms.backend.security.repository;

import com.hsms.backend.common.IncidentStatus;
import com.hsms.backend.security.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByOrderByIdAsc();

    List<Incident> findByMissionIdOrderByIdAsc(Long missionId);

    long countByStatusNot(IncidentStatus status);

    Optional<Incident> findFirstByMissionIdAndStatusNotOrderBySlaStartedAtAscIdAsc(Long missionId, IncidentStatus status);

    List<Incident> findByStatusNotOrderBySlaDeadlineAtAscIdAsc(IncidentStatus status);

    List<Incident> findByStatusIn(Collection<IncidentStatus> statuses);
}

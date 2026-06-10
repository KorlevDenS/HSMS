package com.hsms.backend.mission.repository;

import com.hsms.backend.common.MissionStatus;
import com.hsms.backend.mission.model.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findAllByOrderByIdAsc();

    long countByStatus(MissionStatus status);

    long countByStatusIn(Collection<MissionStatus> statuses);

    @Query("""
            select count(m)
            from Mission m
            where m.status in :statuses
              and (m.harvester.id = :harvesterId or m.crew.id = :crewId)
              and (:currentMissionId is null or m.id <> :currentMissionId)
            """)
    long countActiveResourceAssignments(
            @Param("statuses") Collection<MissionStatus> statuses,
            @Param("harvesterId") Long harvesterId,
            @Param("crewId") Long crewId,
            @Param("currentMissionId") Long currentMissionId
    );
}

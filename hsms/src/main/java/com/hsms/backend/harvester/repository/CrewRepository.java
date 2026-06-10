package com.hsms.backend.harvester.repository;

import com.hsms.backend.common.ResourceStatus;
import com.hsms.backend.harvester.model.Crew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewRepository extends JpaRepository<Crew, Long> {
    List<Crew> findAllByOrderByIdAsc();

    List<Crew> findByStatusOrderByIdAsc(ResourceStatus status);
}

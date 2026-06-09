package com.hsms.backend.harvester.repository;

import com.hsms.backend.common.HsmsDomain.ResourceStatus;
import com.hsms.backend.harvester.model.Harvester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HarvesterRepository extends JpaRepository<Harvester, Long> {
    List<Harvester> findAllByOrderByIdAsc();

    List<Harvester> findByStatusOrderByIdAsc(ResourceStatus status);
}

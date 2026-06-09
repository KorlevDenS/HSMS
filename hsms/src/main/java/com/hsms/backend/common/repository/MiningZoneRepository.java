package com.hsms.backend.common.repository;

import com.hsms.backend.common.model.MiningZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MiningZoneRepository extends JpaRepository<MiningZone, Long> {
    List<MiningZone> findAllByOrderByIdAsc();
}

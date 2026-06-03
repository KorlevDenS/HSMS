package com.hsms.backend.auth.repository;

import com.hsms.backend.auth.model.HsmsUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HsmsUserRepository extends JpaRepository<HsmsUser, Long> {
}
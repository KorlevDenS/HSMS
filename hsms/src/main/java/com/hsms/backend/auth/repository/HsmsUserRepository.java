package com.hsms.backend.auth.repository;

import com.hsms.backend.auth.model.HsmsUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HsmsUserRepository extends JpaRepository<HsmsUser, Long> {
    Optional<HsmsUser> findByLogin(String login);
}

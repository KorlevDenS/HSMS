package com.hsms.backend.auth.repository;

import com.hsms.backend.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
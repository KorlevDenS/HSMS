package com.hsms.backend.auth.repository;

import com.hsms.backend.auth.model.Permission;
import com.hsms.backend.auth.model.PermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, PermissionId> {
}
package com.hsms.backend.auth.api;

import com.hsms.backend.common.AuditEventDto;
import com.hsms.backend.common.BootstrapDto;
import com.hsms.backend.common.DashboardDto;
import com.hsms.backend.common.HsmsUserDto;
import com.hsms.backend.common.LoginRequest;
import com.hsms.backend.common.LoginResponse;
import com.hsms.backend.common.UserCreateRequest;
import com.hsms.backend.common.UserRoleUpdateRequest;

import java.time.Instant;
import java.util.List;

public interface AuthApi {
    List<RoleResponse> getAllRoles();

    LoginResponse login(LoginRequest request);

    HsmsUserDto currentUser(String actorLogin);

    HsmsUserDto createUser(String actorLogin, UserCreateRequest request);

    HsmsUserDto updateUserRoles(String actorLogin, long userId, UserRoleUpdateRequest request);

    BootstrapDto bootstrap(String actorLogin);

    DashboardDto dashboard();

    DashboardDto dashboard(Instant from, Instant to);

    List<AuditEventDto> auditSnapshot();
}

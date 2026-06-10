package com.hsms.backend.auth.api;

import com.hsms.backend.common.HsmsDomain.AuditEventDto;
import com.hsms.backend.common.HsmsDomain.BootstrapDto;
import com.hsms.backend.common.HsmsDomain.DashboardDto;
import com.hsms.backend.common.HsmsDomain.HsmsUserDto;
import com.hsms.backend.common.HsmsDomain.LoginRequest;
import com.hsms.backend.common.HsmsDomain.LoginResponse;
import com.hsms.backend.common.HsmsDomain.UserCreateRequest;
import com.hsms.backend.common.HsmsDomain.UserRoleUpdateRequest;

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

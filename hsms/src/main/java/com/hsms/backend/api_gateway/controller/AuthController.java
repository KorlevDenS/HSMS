package com.hsms.backend.api_gateway.controller;

import com.hsms.backend.auth.api.AuthApi;
import com.hsms.backend.auth.api.RoleResponse;
import com.hsms.backend.common.HsmsDomain.HsmsUserDto;
import com.hsms.backend.common.HsmsDomain.LoginRequest;
import com.hsms.backend.common.HsmsDomain.LoginResponse;
import com.hsms.backend.common.HsmsDomain.UserCreateRequest;
import com.hsms.backend.common.HsmsDomain.UserRoleUpdateRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AuthController extends HsmsControllerSupport {

    private final AuthApi authApi;

    public AuthController(AuthApi authApi) {
        this.authApi = authApi;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody(required = false) LoginRequest request) {
        return authApi.login(request);
    }

    @GetMapping("/auth/me")
    public HsmsUserDto me(Authentication authentication) {
        return authApi.currentUser(actor(authentication));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public List<RoleResponse> roles() {
        return authApi.getAllRoles();
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public HsmsUserDto createUser(Authentication authentication, @RequestBody UserCreateRequest request) {
        return authApi.createUser(actor(authentication), request);
    }

    @PatchMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMINISTRATOR')")
    public HsmsUserDto updateUserRoles(
            Authentication authentication,
            @PathVariable long userId,
            @RequestBody UserRoleUpdateRequest request
    ) {
        return authApi.updateUserRoles(actor(authentication), userId, request);
    }
}

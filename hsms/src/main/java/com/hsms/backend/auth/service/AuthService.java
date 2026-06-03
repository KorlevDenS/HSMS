package com.hsms.backend.auth.service;

import com.hsms.backend.auth.api.AuthApi;
import com.hsms.backend.auth.api.RoleResponse;
import com.hsms.backend.auth.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService implements AuthApi {

    private final RoleRepository roleRepository;

    public AuthService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<RoleResponse> getAllRoles() {
        return this.roleRepository.findAll()
                .stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .toList();
    }

}

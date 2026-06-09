package com.hsms.backend.common;

import com.hsms.backend.auth.model.HsmsUser;
import com.hsms.backend.auth.model.Role;
import com.hsms.backend.auth.repository.HsmsUserRepository;
import com.hsms.backend.common.HsmsDomain.HsmsUserDto;
import com.hsms.backend.common.HsmsDomain.RoleCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hsms.backend.common.HsmsOps.forbidden;
import static com.hsms.backend.common.HsmsOps.notFound;

@Service
public class HsmsAccessService {

    private final HsmsUserRepository userRepository;

    public HsmsAccessService(HsmsUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public HsmsUser requireUser(String login) {
        if (login == null || login.isBlank()) {
            throw forbidden("Требуется вход в систему", "Войдите пользователем с подходящей ролью.");
        }
        return userRepository.findByLogin(login.trim())
                .orElseThrow(() -> notFound("Пользователь не найден: " + login, "Проверьте учетную запись."));
    }

    @Transactional(readOnly = true)
    public HsmsUser requireAny(String login, RoleCode... allowedRoles) {
        HsmsUser user = requireUser(login);
        Set<RoleCode> allowed = Set.copyOf(Arrays.asList(allowedRoles));
        if (roles(user).stream().noneMatch(allowed::contains)) {
            throw forbidden("Недостаточно прав для операции", "Войдите пользователем с подходящей ролью.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public HsmsUser systemUser() {
        return userRepository.findByLogin("admin")
                .or(() -> userRepository.findByLogin("system"))
                .orElseThrow(() -> notFound("Системный пользователь не найден", "Проверьте seed-данные пользователей."));
    }

    public HsmsUserDto dto(HsmsUser user) {
        return new HsmsUserDto(
                user.getId(),
                user.getLogin(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhoneNumber(),
                roles(user)
        );
    }

    public Set<RoleCode> roles(HsmsUser user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .map(RoleCode::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }
}

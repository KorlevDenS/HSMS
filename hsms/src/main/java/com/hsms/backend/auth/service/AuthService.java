package com.hsms.backend.auth.service;

import com.hsms.backend.auth.api.AuthApi;
import com.hsms.backend.auth.api.RoleResponse;
import com.hsms.backend.auth.model.HsmsUser;
import com.hsms.backend.auth.model.Role;
import com.hsms.backend.auth.repository.HsmsUserRepository;
import com.hsms.backend.auth.repository.RoleRepository;
import com.hsms.backend.common.HsmsAccessService;
import com.hsms.backend.common.HsmsAuditService;
import com.hsms.backend.common.HsmsException;
import com.hsms.backend.common.AuditEventDto;
import com.hsms.backend.common.BootstrapDto;
import com.hsms.backend.common.CrewDto;
import com.hsms.backend.common.DashboardDto;
import com.hsms.backend.common.HarvesterDto;
import com.hsms.backend.common.HsmsUserDto;
import com.hsms.backend.common.IncidentDto;
import com.hsms.backend.common.InsuranceCaseDto;
import com.hsms.backend.common.LoginRequest;
import com.hsms.backend.common.LoginResponse;
import com.hsms.backend.common.MissionDto;
import com.hsms.backend.common.RoleCode;
import com.hsms.backend.common.UserCreateRequest;
import com.hsms.backend.common.UserRoleUpdateRequest;
import com.hsms.backend.harvester.api.HarvesterApi;
import com.hsms.backend.readmodel.HsmsDtoAssembler;
import com.hsms.backend.security.auth.HsmsPrincipal;
import com.hsms.backend.security.auth.HsmsTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hsms.backend.common.HsmsOps.*;

@Service
@Transactional
public class AuthService implements AuthApi {

    private final RoleRepository roleRepository;
    private final HsmsUserRepository userRepository;
    private final HsmsAccessService access;
    private final HsmsAuditService audit;
    private final HsmsDtoAssembler dto;
    private final HarvesterApi harvesterApi;
    private final PasswordEncoder passwordEncoder;
    private final HsmsTokenService tokenService;

    public AuthService(
            RoleRepository roleRepository,
            HsmsUserRepository userRepository,
            HsmsAccessService access,
            HsmsAuditService audit,
            HsmsDtoAssembler dto,
            HarvesterApi harvesterApi,
            PasswordEncoder passwordEncoder,
            HsmsTokenService tokenService
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.audit = audit;
        this.dto = dto;
        this.harvesterApi = harvesterApi;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.login() == null || request.login().isBlank() || request.password() == null) {
            throw new HsmsException(400, "Укажите логин и пароль", "Введите учетные данные пользователя.");
        }
        HsmsUser user = userRepository.findByLogin(request.login().trim())
                .orElseThrow(() -> new HsmsException(401, "Пользователь не найден", "Проверьте логин и пароль."));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new HsmsException(401, "Неверный пароль", "Проверьте логин и пароль.");
        }
        HsmsPrincipal principal = new HsmsPrincipal(user.getId(), user.getLogin(), user.getDisplayName(), access.roles(user));
        return new LoginResponse(tokenService.issue(principal), access.dto(user));
    }

    @Override
    @Transactional(readOnly = true)
    public HsmsUserDto currentUser(String actorLogin) {
        return access.dto(access.requireUser(actorLogin));
    }

    @Override
    public HsmsUserDto createUser(String actorLogin, UserCreateRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_ADMINISTRATOR);
        if (request == null || !hasText(request.login()) || !hasText(request.password())) {
            throw badRequest("Не указан логин или пароль пользователя", "Заполните обязательные поля учетной записи.");
        }
        String login = request.login().trim();
        userRepository.findByLogin(login).ifPresent(existing -> {
            throw badRequest("Пользователь с таким логином уже существует", "Выберите другой логин.");
        });
        HsmsUser user = new HsmsUser();
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(blankToDefault(request.displayName(), login));
        user.setEmail(blankToDefault(request.email(), login + "@hsms.local"));
        user.setPhoneNumber(blankToDefault(request.phone(), "+7-000-000"));
        user.setRoles(resolveRoles(request.roles()));
        HsmsUser saved = userRepository.save(user);
        audit.record(actor, "USER_CREATED", "hsms_user", saved.getId(), null, Map.of(
                "login", saved.getLogin(),
                "roles", saved.getRoles().stream().map(Role::getName).collect(Collectors.joining(","))
        ));
        return access.dto(saved);
    }

    @Override
    public HsmsUserDto updateUserRoles(String actorLogin, long userId, UserRoleUpdateRequest request) {
        HsmsUser actor = access.requireAny(actorLogin, RoleCode.ROLE_ADMINISTRATOR);
        if (request == null || request.roles() == null || request.roles().isEmpty()) {
            throw badRequest("Не указаны роли пользователя", "Выберите хотя бы одну роль.");
        }
        HsmsUser user = userRepository.findById(userId)
                .orElseThrow(() -> notFound("Пользователь не найден", "Проверьте идентификатор пользователя."));
        user.setRoles(resolveRoles(request.roles()));
        HsmsUser saved = userRepository.save(user);
        audit.record(actor, "USER_ROLES_UPDATED", "hsms_user", userId, null, Map.of(
                "login", saved.getLogin(),
                "roles", saved.getRoles().stream().map(Role::getName).collect(Collectors.joining(","))
        ));
        return access.dto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BootstrapDto bootstrap(String actorLogin) {
        HsmsUser user = access.requireUser(actorLogin);
        Set<RoleCode> roles = access.roles(user);
        List<CrewDto> visibleCrews = visibleCrews(user, roles);
        Set<Long> visibleCrewIds = visibleCrews.stream().map(CrewDto::id).collect(Collectors.toUnmodifiableSet());
        List<MissionDto> visibleMissions = visibleMissions(roles, visibleCrewIds);
        Set<Long> visibleMissionIds = visibleMissions.stream().map(MissionDto::id).collect(Collectors.toUnmodifiableSet());
        return new BootstrapDto(
                access.dto(user),
                hasAny(roles, RoleCode.ROLE_ADMINISTRATOR) ? userRepository.findAll().stream()
                        .sorted(Comparator.comparing(HsmsUser::getId))
                        .map(access::dto)
                        .toList() : List.of(),
                dto.zones(),
                visibleHarvesters(roles, visibleMissions),
                visibleCrews,
                visibleMissions,
                visibleIncidents(roles, visibleMissionIds),
                visibleInsuranceCases(roles, visibleMissionIds),
                hasAny(roles, RoleCode.ROLE_OPERATIONS_MANAGEMENT, RoleCode.ROLE_ADMINISTRATOR)
                        ? dto.auditSnapshot()
                        : List.of(),
                dto.activeRiskPolicy(),
                hasAny(roles, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_OPERATIONS_MANAGEMENT, RoleCode.ROLE_ADMINISTRATOR)
                        ? dto.dashboard(null, null)
                        : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        return dto.dashboard(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDto dashboard(Instant from, Instant to) {
        return dto.dashboard(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEventDto> auditSnapshot() {
        return dto.auditSnapshot();
    }

    private Set<Role> resolveRoles(Set<RoleCode> roleCodes) {
        Set<RoleCode> normalized = roleCodes == null || roleCodes.isEmpty()
                ? Set.of(RoleCode.ROLE_OPERATIONS_MANAGEMENT)
                : roleCodes;
        return normalized.stream()
                .map(role -> roleRepository.findByName(role.name())
                        .orElseThrow(() -> notFound("Роль не найдена: " + role.name(), "Проверьте справочник ролей.")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<CrewDto> visibleCrews(HsmsUser user, Set<RoleCode> roles) {
        if (hasAny(roles, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR)) {
            return dto.crews();
        }
        if (hasAny(roles, RoleCode.ROLE_HARVESTER_CREW)) {
            return harvesterApi.crewsByUser(user.getId());
        }
        return List.of();
    }

    private List<MissionDto> visibleMissions(Set<RoleCode> roles, Set<Long> visibleCrewIds) {
        if (hasAny(roles,
                RoleCode.ROLE_SUPPLY_MANAGER,
                RoleCode.ROLE_SECURITY_HEADQUARTERS_OPERATOR,
                RoleCode.ROLE_INSURANCE_CONTOUR_OPERATOR,
                RoleCode.ROLE_OPERATIONS_MANAGEMENT,
                RoleCode.ROLE_ADMINISTRATOR)) {
            return dto.missions();
        }
        if (hasAny(roles, RoleCode.ROLE_HARVESTER_CREW)) {
            return dto.missions().stream()
                    .filter(mission -> mission.crewId() != null && visibleCrewIds.contains(mission.crewId()))
                    .toList();
        }
        return List.of();
    }

    private List<HarvesterDto> visibleHarvesters(Set<RoleCode> roles, List<MissionDto> visibleMissions) {
        if (hasAny(roles, RoleCode.ROLE_SUPPLY_MANAGER, RoleCode.ROLE_ADMINISTRATOR)) {
            return dto.harvesters();
        }
        Set<Long> harvesterIds = visibleMissions.stream()
                .map(MissionDto::harvesterId)
                .filter(id -> id != null)
                .collect(Collectors.toUnmodifiableSet());
        if (harvesterIds.isEmpty()) {
            return List.of();
        }
        return dto.harvesters().stream()
                .filter(harvester -> harvesterIds.contains(harvester.id()))
                .toList();
    }

    private List<IncidentDto> visibleIncidents(Set<RoleCode> roles, Set<Long> visibleMissionIds) {
        if (hasAny(roles,
                RoleCode.ROLE_SECURITY_HEADQUARTERS_OPERATOR,
                RoleCode.ROLE_INSURANCE_CONTOUR_OPERATOR,
                RoleCode.ROLE_OPERATIONS_MANAGEMENT,
                RoleCode.ROLE_ADMINISTRATOR)) {
            return dto.incidents();
        }
        if (hasAny(roles, RoleCode.ROLE_HARVESTER_CREW)) {
            return dto.incidents().stream()
                    .filter(incident -> visibleMissionIds.contains(incident.missionId()))
                    .toList();
        }
        return List.of();
    }

    private List<InsuranceCaseDto> visibleInsuranceCases(Set<RoleCode> roles, Set<Long> visibleMissionIds) {
        if (hasAny(roles,
                RoleCode.ROLE_INSURANCE_CONTOUR_OPERATOR,
                RoleCode.ROLE_OPERATIONS_MANAGEMENT,
                RoleCode.ROLE_ADMINISTRATOR)) {
            return dto.insuranceCases();
        }
        if (hasAny(roles, RoleCode.ROLE_SUPPLY_MANAGER)) {
            return dto.insuranceCases().stream()
                    .filter(insurance -> visibleMissionIds.contains(insurance.missionId()))
                    .toList();
        }
        return List.of();
    }

    private boolean hasAny(Set<RoleCode> roles, RoleCode... expected) {
        return List.of(expected).stream().anyMatch(roles::contains);
    }
}

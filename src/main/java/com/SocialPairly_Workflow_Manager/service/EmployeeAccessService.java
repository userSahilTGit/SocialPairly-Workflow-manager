package com.SocialPairly_Workflow_Manager.service;

import com.SocialPairly_Workflow_Manager.constants.DashboardKeys;
import com.SocialPairly_Workflow_Manager.constants.EmployeeRoleCodes;
import com.SocialPairly_Workflow_Manager.dto.AssignEmployeeRoleRequest;
import com.SocialPairly_Workflow_Manager.dto.EmployeeRoleAssignmentDto;
import com.SocialPairly_Workflow_Manager.dto.RoleDashboardAccessDto;
import com.SocialPairly_Workflow_Manager.dto.UpdateEmployeeRoleRequest;
import com.SocialPairly_Workflow_Manager.dto.UpdateRoleDashboardAccessRequest;
import com.SocialPairly_Workflow_Manager.dto.UserDto;
import com.SocialPairly_Workflow_Manager.entity.Role;
import com.SocialPairly_Workflow_Manager.entity.RoleDashboardAccess;
import com.SocialPairly_Workflow_Manager.entity.StaffRoleAssignment;
import com.SocialPairly_Workflow_Manager.entity.User;
import com.SocialPairly_Workflow_Manager.entity.UserProfile;
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ForbiddenException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.RoleDashboardAccessRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffRoleAssignmentRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmployeeAccessService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeAccessService.class);

    private final StaffRoleAssignmentRepository assignmentRepository;
    private final RoleDashboardAccessRepository dashboardAccessRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public EmployeeAccessService(StaffRoleAssignmentRepository assignmentRepository,
                                 RoleDashboardAccessRepository dashboardAccessRepository,
                                 UserRepository userRepository,
                                 CurrentUserService currentUserService) {
        this.assignmentRepository = assignmentRepository;
        this.dashboardAccessRepository = dashboardAccessRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public User applyEmployeeAccessOnAuth(User user) {
        if (user == null || user.getEmail() == null) {
            return user;
        }
        String email = normalizeEmail(user.getEmail());

        Optional<StaffRoleAssignment> existing = assignmentRepository.findByEmailIgnoreCase(email);
        if (existing.isEmpty() && EmployeeRoleCodes.isSocialPairlyDomain(email)) {
            ensureDomainAutoAssignment(user);
        } else if (existing.isPresent()) {
            StaffRoleAssignment row = existing.get();
            if (row.getUserId() == null) {
                row.setUserId(user.getId());
                assignmentRepository.save(row);
            }
            syncCoarseRole(user, row.isActive());
        } else if (user.getRole() == Role.ADMIN) {
            // Legacy ADMIN without mapping keeps coarse ADMIN; dashboards fall back to full set.
            syncCoarseRole(user, true);
        }

        return userRepository.findById(user.getId()).orElse(user);
    }

    @Transactional
    public void ensureSeedSuperAdmin(User adminUser) {
        if (adminUser == null || adminUser.getEmail() == null) {
            return;
        }
        String email = normalizeEmail(adminUser.getEmail());
        Optional<StaffRoleAssignment> existing = assignmentRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            StaffRoleAssignment row = existing.get();
            if (row.getUserId() == null) {
                row.setUserId(adminUser.getId());
                assignmentRepository.save(row);
            }
            return;
        }
        StaffRoleAssignment row = new StaffRoleAssignment();
        row.setEmail(email);
        row.setUserId(adminUser.getId());
        row.setRoleCode(EmployeeRoleCodes.SUPER_ADMIN);
        row.setActive(true);
        row.setAssignmentSource(EmployeeRoleCodes.SOURCE_SEED);
        row.setNotes("Bootstrapped from app.admin seed");
        assignmentRepository.save(row);
        if (adminUser.getRole() != Role.ADMIN) {
            adminUser.setRole(Role.ADMIN);
            userRepository.save(adminUser);
        }
        log.info("Seeded SUPER_ADMIN employee assignment for {}", email);
    }

    public UserDto enrich(UserDto base) {
        if (base == null) {
            return null;
        }
        String employeeRole = resolveEmployeeRoleCode(base.email(), base.role());
        List<String> dashboards = resolveAllowedDashboards(base.email(), base.role(), employeeRole);
        return base.withEmployeeAccess(employeeRole, dashboards);
    }

    public UserDto toEnrichedDto(User user) {
        return enrich(UserDto.from(user));
    }

    public UserDto toEnrichedDto(User user, UserProfile profile) {
        return enrich(UserDto.from(user, profile));
    }

    public String resolveEmployeeRoleCode(String email, Role coarseRole) {
        if (email == null) {
            return null;
        }
        return assignmentRepository.findByEmailIgnoreCaseAndActiveTrue(normalizeEmail(email))
                .map(StaffRoleAssignment::getRoleCode)
                .orElse(null);
    }

    public List<String> resolveAllowedDashboards(String email, Role coarseRole, String employeeRole) {
        if (coarseRole != Role.ADMIN && (employeeRole == null || employeeRole.isBlank())) {
            return List.of();
        }
        if (employeeRole == null || employeeRole.isBlank()) {
            return orderedDashboardKeys(DashboardKeys.all());
        }
        List<RoleDashboardAccess> rows = dashboardAccessRepository.findByRoleCodeAndCanAccessTrue(employeeRole);
        if (rows.isEmpty() && coarseRole == Role.ADMIN) {
            // Mapping missing — keep legacy admins fully functional.
            return orderedDashboardKeys(DashboardKeys.all());
        }
        Set<String> keys = rows.stream()
                .map(RoleDashboardAccess::getDashboardKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return orderedDashboardKeys(keys);
    }

    public boolean hasDashboardAccess(User user, String dashboardKey) {
        if (user == null || user.getRole() != Role.ADMIN) {
            return false;
        }
        String key = DashboardKeys.normalize(dashboardKey);
        if (!DashboardKeys.isValid(key)) {
            return false;
        }
        String employeeRole = resolveEmployeeRoleCode(user.getEmail(), user.getRole());
        List<String> allowed = resolveAllowedDashboards(user.getEmail(), user.getRole(), employeeRole);
        return allowed.contains(key);
    }

    public void requireDashboard(String dashboardKey) {
        User user = currentUserService.getCurrentUser();
        if (!hasDashboardAccess(user, dashboardKey)) {
            throw new ForbiddenException("You do not have access to this dashboard");
        }
    }

    public void requireRoleAccessPage() {
        requireDashboard(DashboardKeys.ROLE_ACCESS);
    }

    @Transactional(readOnly = true)
    public List<EmployeeRoleAssignmentDto> listAssignments() {
        requireRoleAccessPage();
        List<StaffRoleAssignment> rows = assignmentRepository.findAllByOrderByUpdatedAtDesc();
        Map<Long, User> usersById = loadUsersByIds(rows);
        Map<String, User> usersByEmail = loadUsersByEmails(rows);
        List<EmployeeRoleAssignmentDto> out = new ArrayList<>(rows.size());
        for (StaffRoleAssignment row : rows) {
            User linked = row.getUserId() != null ? usersById.get(row.getUserId()) : null;
            if (linked == null) {
                linked = usersByEmail.get(normalizeEmail(row.getEmail()));
            }
            String first = linked != null ? linked.getFirstName() : null;
            String last = linked != null ? linked.getLastName() : null;
            out.add(EmployeeRoleAssignmentDto.from(row, first, last));
        }
        return out;
    }

    @Transactional
    public EmployeeRoleAssignmentDto assignRole(AssignEmployeeRoleRequest request) {
        requireRoleAccessPage();
        User actor = currentUserService.getCurrentUser();

        String email = normalizeEmail(request.email());
        String roleCode = EmployeeRoleCodes.normalize(request.roleCode());
        if (!EmployeeRoleCodes.isValid(roleCode)) {
            throw new BadRequestException("Invalid role code");
        }

        User target = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(
                        "User must be registered before an employee role can be assigned"));

        Optional<StaffRoleAssignment> existingOpt = assignmentRepository.findByEmailIgnoreCase(email);
        if (existingOpt.isPresent()) {
            StaffRoleAssignment existing = existingOpt.get();
            if (EmployeeRoleCodes.SUPER_ADMIN.equals(existing.getRoleCode())) {
                throw new ForbiddenException(
                        "SUPER_ADMIN assignments cannot be changed from the application. Update via DB if required.");
            }
            existing.setRoleCode(roleCode);
            existing.setActive(true);
            existing.setUserId(target.getId());
            existing.setAssignedByEmail(actor.getEmail());
            existing.setAssignmentSource(EmployeeRoleCodes.SOURCE_MANUAL);
            if (request.notes() != null) {
                existing.setNotes(request.notes());
            }
            StaffRoleAssignment saved = assignmentRepository.save(existing);
            syncCoarseRole(target, true);
            return EmployeeRoleAssignmentDto.from(saved, target.getFirstName(), target.getLastName());
        }

        StaffRoleAssignment row = new StaffRoleAssignment();
        row.setEmail(email);
        row.setUserId(target.getId());
        row.setRoleCode(roleCode);
        row.setActive(true);
        row.setAssignmentSource(EmployeeRoleCodes.SOURCE_MANUAL);
        row.setAssignedByEmail(actor.getEmail());
        row.setNotes(request.notes());
        StaffRoleAssignment saved = assignmentRepository.save(row);
        syncCoarseRole(target, true);
        return EmployeeRoleAssignmentDto.from(saved, target.getFirstName(), target.getLastName());
    }

    @Transactional
    public EmployeeRoleAssignmentDto updateAssignment(Long id, UpdateEmployeeRoleRequest request) {
        requireRoleAccessPage();
        User actor = currentUserService.getCurrentUser();

        StaffRoleAssignment row = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee role assignment not found"));

        if (EmployeeRoleCodes.SUPER_ADMIN.equals(row.getRoleCode())) {
            throw new ForbiddenException(
                    "SUPER_ADMIN assignments cannot be changed from the application. Update via DB if required.");
        }

        String roleCode = EmployeeRoleCodes.normalize(request.roleCode());
        if (!EmployeeRoleCodes.isValid(roleCode)) {
            throw new BadRequestException("Invalid role code");
        }
        if (EmployeeRoleCodes.SUPER_ADMIN.equals(roleCode)) {
            // Allow promoting to SUPER_ADMIN, but never demote/edit existing SUPER_ADMIN via API.
            // Once saved as SUPER_ADMIN, further edits require DB.
        }

        row.setRoleCode(roleCode);
        if (request.active() != null) {
            row.setActive(request.active());
        }
        if (request.notes() != null) {
            row.setNotes(request.notes());
        }
        row.setAssignedByEmail(actor.getEmail());
        row.setAssignmentSource(EmployeeRoleCodes.SOURCE_MANUAL);

        User target = userRepository.findByEmail(row.getEmail()).orElse(null);
        if (target != null) {
            row.setUserId(target.getId());
            syncCoarseRole(target, row.isActive());
        }

        StaffRoleAssignment saved = assignmentRepository.save(row);
        String first = target != null ? target.getFirstName() : null;
        String last = target != null ? target.getLastName() : null;
        return EmployeeRoleAssignmentDto.from(saved, first, last);
    }

    @Transactional(readOnly = true)
    public List<RoleDashboardAccessDto> listDashboardAccess() {
        requireRoleAccessPage();
        return dashboardAccessRepository.findAllByOrderByRoleCodeAscDashboardKeyAsc().stream()
                .map(RoleDashboardAccessDto::from)
                .toList();
    }

    @Transactional
    public RoleDashboardAccessDto updateDashboardAccess(UpdateRoleDashboardAccessRequest request) {
        requireRoleAccessPage();
        User actor = currentUserService.getCurrentUser();

        String roleCode = EmployeeRoleCodes.normalize(request.roleCode());
        String dashboardKey = DashboardKeys.normalize(request.dashboardKey());
        if (!EmployeeRoleCodes.isValid(roleCode)) {
            throw new BadRequestException("Invalid role code");
        }
        if (!DashboardKeys.isValid(dashboardKey)) {
            throw new BadRequestException("Invalid dashboard key");
        }
        if (EmployeeRoleCodes.SUPER_ADMIN.equals(roleCode) && Boolean.FALSE.equals(request.canAccess())) {
            throw new BadRequestException("SUPER_ADMIN dashboard access cannot be revoked from the application");
        }
        if (DashboardKeys.ROLE_ACCESS.equals(dashboardKey)
                && !EmployeeRoleCodes.SUPER_ADMIN.equals(roleCode)
                && Boolean.TRUE.equals(request.canAccess())) {
            throw new BadRequestException("ROLE_ACCESS dashboard is reserved for SUPER_ADMIN");
        }

        RoleDashboardAccess row = dashboardAccessRepository
                .findByRoleCodeAndDashboardKey(roleCode, dashboardKey)
                .orElseGet(() -> {
                    RoleDashboardAccess created = new RoleDashboardAccess();
                    created.setRoleCode(roleCode);
                    created.setDashboardKey(dashboardKey);
                    return created;
                });
        row.setCanAccess(Boolean.TRUE.equals(request.canAccess()));
        row.setUpdatedByEmail(actor.getEmail());
        return RoleDashboardAccessDto.from(dashboardAccessRepository.save(row));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> catalog() {
        requireRoleAccessPage();
        return Map.of(
                "roles", List.copyOf(EmployeeRoleCodes.all()),
                "dashboards", List.copyOf(DashboardKeys.all())
        );
    }

    private void ensureDomainAutoAssignment(User user) {
        String email = normalizeEmail(user.getEmail());
        if (assignmentRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        StaffRoleAssignment row = new StaffRoleAssignment();
        row.setEmail(email);
        row.setUserId(user.getId());
        row.setRoleCode(EmployeeRoleCodes.DEFAULT_DOMAIN_ROLE);
        row.setActive(true);
        row.setAssignmentSource(EmployeeRoleCodes.SOURCE_DOMAIN_AUTO);
        row.setNotes("Auto-assigned for @" + EmployeeRoleCodes.DOMAIN_SOCIALPAIRLY);
        assignmentRepository.save(row);
        syncCoarseRole(user, true);
        log.info("Auto-assigned ADMIN employee role for domain email {}", email);
    }

    private void syncCoarseRole(User user, boolean employeeActive) {
        if (employeeActive) {
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
            }
        } else if (user.getRole() == Role.ADMIN) {
            user.setRole(Role.USER);
            userRepository.save(user);
        }
    }

    private Map<Long, User> loadUsersByIds(List<StaffRoleAssignment> rows) {
        Set<Long> ids = rows.stream()
                .map(StaffRoleAssignment::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<String, User> loadUsersByEmails(List<StaffRoleAssignment> rows) {
        Set<String> emails = rows.stream()
                .map(StaffRoleAssignment::getEmail)
                .map(this::normalizeEmail)
                .collect(Collectors.toSet());
        if (emails.isEmpty()) {
            return Map.of();
        }
        Map<String, User> map = new java.util.HashMap<>();
        for (String email : emails) {
            userRepository.findByEmail(email).ifPresent(u -> map.put(email, u));
        }
        return map;
    }

    private List<String> orderedDashboardKeys(Set<String> keys) {
        List<String> order = List.of(
                DashboardKeys.DASHBOARD,
                DashboardKeys.QUESTIONS,
                DashboardKeys.MODERATION,
                DashboardKeys.IDENTITY_DOCS,
                DashboardKeys.PLANS,
                DashboardKeys.EVENTS,
                DashboardKeys.FINANCE,
                DashboardKeys.CUSTOMER_SUPPORT,
                DashboardKeys.ROLE_ACCESS
        );
        return order.stream().filter(keys::contains).toList();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

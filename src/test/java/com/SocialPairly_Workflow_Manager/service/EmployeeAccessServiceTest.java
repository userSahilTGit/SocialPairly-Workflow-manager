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
import com.SocialPairly_Workflow_Manager.exception.BadRequestException;
import com.SocialPairly_Workflow_Manager.exception.ForbiddenException;
import com.SocialPairly_Workflow_Manager.exception.ResourceNotFoundException;
import com.SocialPairly_Workflow_Manager.repository.RoleDashboardAccessRepository;
import com.SocialPairly_Workflow_Manager.repository.StaffRoleAssignmentRepository;
import com.SocialPairly_Workflow_Manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeAccessServiceTest {

    @Mock
    private StaffRoleAssignmentRepository assignmentRepository;

    @Mock
    private RoleDashboardAccessRepository dashboardAccessRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EmployeeAccessService service;

    private User actor;
    private User target;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(99L);
        actor.setEmail("super@socialpairly.com");
        actor.setRole(Role.ADMIN);

        target = new User();
        target.setId(7L);
        target.setEmail("member@example.com");
        target.setFirstName("Mem");
        target.setLastName("Ber");
        target.setRole(Role.USER);
    }

    @Test
    void applyEmployeeAccessOnAuthHandlesNullAndDomainAutoAssign() {
        assertNull(service.applyEmployeeAccessOnAuth(null));

        User noEmail = new User();
        noEmail.setId(1L);
        assertSame(noEmail, service.applyEmployeeAccessOnAuth(noEmail));

        User domainUser = new User();
        domainUser.setId(3L);
        domainUser.setEmail("Dev@SocialPairly.com");
        domainUser.setRole(Role.USER);

        when(assignmentRepository.findByEmailIgnoreCase("dev@socialpairly.com")).thenReturn(Optional.empty());
        when(assignmentRepository.existsByEmailIgnoreCase("dev@socialpairly.com")).thenReturn(false);
        when(assignmentRepository.save(any(StaffRoleAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(3L)).thenReturn(Optional.of(domainUser));

        User result = service.applyEmployeeAccessOnAuth(domainUser);
        assertEquals(Role.ADMIN, result.getRole());
        ArgumentCaptor<StaffRoleAssignment> captor = ArgumentCaptor.forClass(StaffRoleAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertEquals(EmployeeRoleCodes.ADMIN, captor.getValue().getRoleCode());
        assertEquals(EmployeeRoleCodes.SOURCE_DOMAIN_AUTO, captor.getValue().getAssignmentSource());
    }

    @Test
    void applyEmployeeAccessOnAuthLinksExistingAssignmentAndLegacyAdmin() {
        StaffRoleAssignment row = new StaffRoleAssignment();
        row.setEmail("member@example.com");
        row.setRoleCode(EmployeeRoleCodes.FINANCE);
        row.setActive(true);
        row.setUserId(null);

        when(assignmentRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(row));
        when(assignmentRepository.save(any(StaffRoleAssignment.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(7L)).thenReturn(Optional.of(target));

        User result = service.applyEmployeeAccessOnAuth(target);
        assertEquals(7L, row.getUserId());
        assertEquals(Role.ADMIN, result.getRole());

        User legacy = new User();
        legacy.setId(8L);
        legacy.setEmail("legacy@example.com");
        legacy.setRole(Role.ADMIN);
        when(assignmentRepository.findByEmailIgnoreCase("legacy@example.com")).thenReturn(Optional.empty());
        when(userRepository.findById(8L)).thenReturn(Optional.of(legacy));
        assertEquals(Role.ADMIN, service.applyEmployeeAccessOnAuth(legacy).getRole());
    }

    @Test
    void ensureSeedSuperAdminCreatesOrLinks() {
        service.ensureSeedSuperAdmin(null);

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@socialpairly.com");
        admin.setRole(Role.ADMIN);

        when(assignmentRepository.findByEmailIgnoreCase("admin@socialpairly.com")).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(StaffRoleAssignment.class))).thenAnswer(i -> i.getArgument(0));
        service.ensureSeedSuperAdmin(admin);
        verify(assignmentRepository).save(argThat(a ->
                EmployeeRoleCodes.SUPER_ADMIN.equals(a.getRoleCode())
                        && EmployeeRoleCodes.SOURCE_SEED.equals(a.getAssignmentSource())));

        StaffRoleAssignment existing = new StaffRoleAssignment();
        existing.setEmail("admin@socialpairly.com");
        existing.setUserId(null);
        when(assignmentRepository.findByEmailIgnoreCase("admin@socialpairly.com")).thenReturn(Optional.of(existing));
        service.ensureSeedSuperAdmin(admin);
        assertEquals(1L, existing.getUserId());
    }

    @Test
    void enrichAndDashboardResolution() {
        assertNull(service.enrich(null));

        UserDto base = UserDto.from(target);
        when(assignmentRepository.findByEmailIgnoreCaseAndActiveTrue("member@example.com"))
                .thenReturn(Optional.empty());
        UserDto enrichedUser = service.enrich(base);
        assertNull(enrichedUser.employeeRole());
        assertTrue(enrichedUser.allowedDashboards().isEmpty());

        StaffRoleAssignment assignment = new StaffRoleAssignment();
        assignment.setRoleCode(EmployeeRoleCodes.FINANCE);
        when(assignmentRepository.findByEmailIgnoreCaseAndActiveTrue("member@example.com"))
                .thenReturn(Optional.of(assignment));
        RoleDashboardAccess access = new RoleDashboardAccess();
        access.setDashboardKey(DashboardKeys.FINANCE);
        access.setCanAccess(true);
        when(dashboardAccessRepository.findByRoleCodeAndCanAccessTrue(EmployeeRoleCodes.FINANCE))
                .thenReturn(List.of(access));

        UserDto again = service.enrich(UserDto.from(target));
        assertEquals(EmployeeRoleCodes.FINANCE, again.employeeRole());
        assertTrue(again.allowedDashboards().contains(DashboardKeys.FINANCE));

        User admin = new User();
        admin.setEmail("legacy@example.com");
        admin.setRole(Role.ADMIN);
        when(assignmentRepository.findByEmailIgnoreCaseAndActiveTrue("legacy@example.com"))
                .thenReturn(Optional.empty());
        UserDto legacyDto = service.toEnrichedDto(admin);
        assertTrue(legacyDto.allowedDashboards().contains(DashboardKeys.DASHBOARD));
        assertTrue(legacyDto.allowedDashboards().contains(DashboardKeys.ROLE_ACCESS));
    }

    @Test
    void hasDashboardAndRequireDashboard() {
        User user = new User();
        user.setEmail("finance@example.com");
        user.setRole(Role.ADMIN);

        StaffRoleAssignment assignment = new StaffRoleAssignment();
        assignment.setRoleCode(EmployeeRoleCodes.FINANCE);
        when(assignmentRepository.findByEmailIgnoreCaseAndActiveTrue("finance@example.com"))
                .thenReturn(Optional.of(assignment));
        RoleDashboardAccess dash = new RoleDashboardAccess();
        dash.setDashboardKey(DashboardKeys.FINANCE);
        when(dashboardAccessRepository.findByRoleCodeAndCanAccessTrue(EmployeeRoleCodes.FINANCE))
                .thenReturn(List.of(dash));

        assertTrue(service.hasDashboardAccess(user, DashboardKeys.FINANCE));
        assertFalse(service.hasDashboardAccess(user, DashboardKeys.QUESTIONS));
        assertFalse(service.hasDashboardAccess(user, "NOPE"));
        assertFalse(service.hasDashboardAccess(null, DashboardKeys.FINANCE));

        when(currentUserService.getCurrentUser()).thenReturn(user);
        assertDoesNotThrow(() -> service.requireDashboard(DashboardKeys.FINANCE));
        assertThrows(ForbiddenException.class, () -> service.requireDashboard(DashboardKeys.QUESTIONS));
    }

    @Test
    void assignRoleValidatesRegistrationAndBlocksSuperAdminEdit() {
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        stubSuperAdminAccess();

        assertThrows(BadRequestException.class,
                () -> service.assignRole(new AssignEmployeeRoleRequest("missing@example.com", "ADMIN", null)));

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(target));
        assertThrows(BadRequestException.class,
                () -> service.assignRole(new AssignEmployeeRoleRequest("member@example.com", "BAD", null)));

        StaffRoleAssignment superRow = new StaffRoleAssignment();
        superRow.setEmail("member@example.com");
        superRow.setRoleCode(EmployeeRoleCodes.SUPER_ADMIN);
        when(assignmentRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(superRow));
        assertThrows(ForbiddenException.class,
                () -> service.assignRole(new AssignEmployeeRoleRequest("member@example.com", "ADMIN", "n")));
    }

    @Test
    void assignRoleCreatesAndUpdatesNonSuperAdmin() {
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        stubSuperAdminAccess();
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(target));
        when(assignmentRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(StaffRoleAssignment.class))).thenAnswer(i -> {
            StaffRoleAssignment row = i.getArgument(0);
            row.setId(11L);
            return row;
        });
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeRoleAssignmentDto created = service.assignRole(
                new AssignEmployeeRoleRequest("Member@Example.com", "finance", "note"));
        assertEquals(EmployeeRoleCodes.FINANCE, created.roleCode());
        assertEquals("Mem", created.firstName());
        assertEquals(Role.ADMIN, target.getRole());

        StaffRoleAssignment existing = new StaffRoleAssignment();
        existing.setId(12L);
        existing.setEmail("member@example.com");
        existing.setRoleCode(EmployeeRoleCodes.ADMIN);
        existing.setActive(false);
        when(assignmentRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(existing));
        when(assignmentRepository.save(existing)).thenReturn(existing);

        EmployeeRoleAssignmentDto updated = service.assignRole(
                new AssignEmployeeRoleRequest("member@example.com", "EVENT_TEAM", "updated"));
        assertEquals(EmployeeRoleCodes.EVENT_TEAM, updated.roleCode());
        assertTrue(existing.isActive());
    }

    @Test
    void updateAssignmentRules() {
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        stubSuperAdminAccess();

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateAssignment(99L, new UpdateEmployeeRoleRequest("ADMIN", true, null)));

        StaffRoleAssignment superRow = new StaffRoleAssignment();
        superRow.setId(1L);
        superRow.setRoleCode(EmployeeRoleCodes.SUPER_ADMIN);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(superRow));
        assertThrows(ForbiddenException.class,
                () -> service.updateAssignment(1L, new UpdateEmployeeRoleRequest("ADMIN", false, null)));

        StaffRoleAssignment row = new StaffRoleAssignment();
        row.setId(2L);
        row.setEmail("member@example.com");
        row.setRoleCode(EmployeeRoleCodes.ADMIN);
        row.setActive(true);
        when(assignmentRepository.findById(2L)).thenReturn(Optional.of(row));
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(target));
        when(assignmentRepository.save(row)).thenReturn(row);
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(BadRequestException.class,
                () -> service.updateAssignment(2L, new UpdateEmployeeRoleRequest("NOPE", true, null)));

        EmployeeRoleAssignmentDto dto = service.updateAssignment(2L,
                new UpdateEmployeeRoleRequest("marketing", false, "off"));
        assertEquals(EmployeeRoleCodes.MARKETING, dto.roleCode());
        assertFalse(row.isActive());
        assertEquals(Role.USER, target.getRole());
    }

    @Test
    void listAssignmentsAndDashboardMatrix() {
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        stubSuperAdminAccess();

        StaffRoleAssignment row = new StaffRoleAssignment();
        row.setId(5L);
        row.setEmail("member@example.com");
        row.setUserId(7L);
        row.setRoleCode(EmployeeRoleCodes.ADMIN);
        row.setActive(true);
        when(assignmentRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(row));
        when(userRepository.findAllById(any())).thenReturn(List.of(target));

        List<EmployeeRoleAssignmentDto> list = service.listAssignments();
        assertEquals(1, list.size());
        assertEquals("Mem", list.get(0).firstName());

        RoleDashboardAccess access = new RoleDashboardAccess();
        access.setId(1L);
        access.setRoleCode(EmployeeRoleCodes.ADMIN);
        access.setDashboardKey(DashboardKeys.DASHBOARD);
        access.setCanAccess(true);
        when(dashboardAccessRepository.findAllByOrderByRoleCodeAscDashboardKeyAsc()).thenReturn(List.of(access));
        assertEquals(1, service.listDashboardAccess().size());

        Map<String, Object> catalog = service.catalog();
        assertTrue(((List<?>) catalog.get("roles")).contains(EmployeeRoleCodes.ADMIN));
        assertTrue(((List<?>) catalog.get("dashboards")).contains(DashboardKeys.DASHBOARD));
    }

    @Test
    void updateDashboardAccessRules() {
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        stubSuperAdminAccess();

        assertThrows(BadRequestException.class, () -> service.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest("NOPE", DashboardKeys.DASHBOARD, true)));
        assertThrows(BadRequestException.class, () -> service.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest(EmployeeRoleCodes.ADMIN, "NOPE", true)));
        assertThrows(BadRequestException.class, () -> service.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest(EmployeeRoleCodes.SUPER_ADMIN, DashboardKeys.DASHBOARD, false)));
        assertThrows(BadRequestException.class, () -> service.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest(EmployeeRoleCodes.ADMIN, DashboardKeys.ROLE_ACCESS, true)));

        when(dashboardAccessRepository.findByRoleCodeAndDashboardKey(EmployeeRoleCodes.ADMIN, DashboardKeys.FINANCE))
                .thenReturn(Optional.empty());
        when(dashboardAccessRepository.save(any(RoleDashboardAccess.class))).thenAnswer(i -> {
            RoleDashboardAccess row = i.getArgument(0);
            row.setId(9L);
            return row;
        });

        RoleDashboardAccessDto created = service.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest("admin", "finance", true));
        assertEquals(EmployeeRoleCodes.ADMIN, created.roleCode());
        assertEquals(DashboardKeys.FINANCE, created.dashboardKey());
        assertTrue(created.canAccess());

        RoleDashboardAccess existing = new RoleDashboardAccess();
        existing.setId(3L);
        existing.setRoleCode(EmployeeRoleCodes.ADMIN);
        existing.setDashboardKey(DashboardKeys.FINANCE);
        existing.setCanAccess(true);
        when(dashboardAccessRepository.findByRoleCodeAndDashboardKey(EmployeeRoleCodes.ADMIN, DashboardKeys.FINANCE))
                .thenReturn(Optional.of(existing));
        when(dashboardAccessRepository.save(existing)).thenReturn(existing);

        RoleDashboardAccessDto updated = service.updateDashboardAccess(
                new UpdateRoleDashboardAccessRequest(EmployeeRoleCodes.ADMIN, DashboardKeys.FINANCE, false));
        assertFalse(updated.canAccess());
    }

    private void stubSuperAdminAccess() {
        StaffRoleAssignment assignment = new StaffRoleAssignment();
        assignment.setRoleCode(EmployeeRoleCodes.SUPER_ADMIN);
        when(assignmentRepository.findByEmailIgnoreCaseAndActiveTrue("super@socialpairly.com"))
                .thenReturn(Optional.of(assignment));
        RoleDashboardAccess access = new RoleDashboardAccess();
        access.setDashboardKey(DashboardKeys.ROLE_ACCESS);
        when(dashboardAccessRepository.findByRoleCodeAndCanAccessTrue(EmployeeRoleCodes.SUPER_ADMIN))
                .thenReturn(List.of(access));
    }
}

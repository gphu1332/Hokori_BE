package com.hokori.web.service;

import com.hokori.web.constants.RoleConstants;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private final AuthService authService = new AuthService();

    // =========================
    // ROLE CHECKING
    // =========================

    @Test
    void userHasRole_correctRole_true() {
        User user = userWithRole(RoleConstants.ADMIN);

        boolean result = authService.userHasRole(user, RoleConstants.ADMIN);

        assertTrue(result);
    }

    @Test
    void userHasRole_wrongRole_false() {
        User user = userWithRole(RoleConstants.LEARNER);

        boolean result = authService.userHasRole(user, RoleConstants.ADMIN);

        assertFalse(result);
    }

    @Test
    void userHasRole_nullRole_false() {
        User user = new User();

        boolean result = authService.userHasRole(user, RoleConstants.ADMIN);

        assertFalse(result);
    }

    @Test
    void isAdmin_adminRole_true() {
        User user = userWithRole(RoleConstants.ADMIN);

        assertTrue(authService.isAdmin(user));
    }

    @Test
    void isAdmin_nonAdmin_false() {
        User user = userWithRole(RoleConstants.LEARNER);

        assertFalse(authService.isAdmin(user));
    }

    @Test
    void isStaffOrAdmin_staff_true() {
        User user = userWithRole(RoleConstants.STAFF);

        assertTrue(authService.isStaffOrAdmin(user));
    }

    @Test
    void isStaffOrAdmin_admin_true() {
        User user = userWithRole(RoleConstants.ADMIN);

        assertTrue(authService.isStaffOrAdmin(user));
    }

    @Test
    void isStaffOrAdmin_learner_false() {
        User user = userWithRole(RoleConstants.LEARNER);

        assertFalse(authService.isStaffOrAdmin(user));
    }

    @Test
    void canCreateContent_teacher_true() {
        User user = userWithRole(RoleConstants.TEACHER);

        assertTrue(authService.canCreateContent(user));
    }

    @Test
    void canCreateContent_admin_true() {
        User user = userWithRole(RoleConstants.ADMIN);

        assertTrue(authService.canCreateContent(user));
    }

    @Test
    void canCreateContent_learner_false() {
        User user = userWithRole(RoleConstants.LEARNER);

        assertFalse(authService.canCreateContent(user));
    }

    // =========================
    // GET USER ROLES
    // =========================

    @Test
    void getUserRoles_validRole_uppercase() {
        User user = userWithRole("learner");

        List<String> roles = authService.getUserRoles(user);

        assertEquals(1, roles.size());
        assertEquals("LEARNER", roles.get(0));
    }

    @Test
    void getUserRoles_nullUser_empty() {
        List<String> roles = authService.getUserRoles(null);

        assertTrue(roles.isEmpty());
    }

    @Test
    void getUserRoles_nullRole_empty() {
        User user = new User();

        List<String> roles = authService.getUserRoles(user);

        assertTrue(roles.isEmpty());
    }

    @Test
    void getUserRoles_blankRole_empty() {
        User user = new User();
        Role role = new Role();
        role.setRoleName("   ");
        user.setRole(role);

        List<String> roles = authService.getUserRoles(user);

        assertTrue(roles.isEmpty());
    }

    // =========================
    // HELPERS
    // =========================

    private User userWithRole(String roleName) {
        User user = new User();
        Role role = new Role();
        role.setRoleName(roleName);
        user.setRole(role);
        return user;
    }
}
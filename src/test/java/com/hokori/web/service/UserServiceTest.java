package com.hokori.web.service;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService userService;

    /**
     * TC-USER-01
     * Kiểm tra userHasRole (case-insensitive)
     */
    @Test
    void userHasRole_correctRole_true() {
        User user = new User();
        Role role = new Role();
        role.setRoleName("ADMIN");
        user.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean result = userService.userHasRole(1L, "admin");

        assertTrue(result);
    }

    /**
     * TC-USER-02
     * Deactivate user
     */
    @Test
    void deactivateUser_success() {
        User user = new User();
        user.setIsActive(true);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        userService.deactivateUser(2L);

        assertFalse(user.getIsActive());
        verify(userRepository).save(user);
    }

    /**
     * TC-USER-03
     * Activate user
     */
    @Test
    void activateUser_success() {
        User user = new User();
        user.setIsActive(false);

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        userService.activateUser(3L);

        assertTrue(user.getIsActive());
        verify(userRepository).save(user);
    }

    /**
     * TC-USER-04
     * Submit teacher approval (NONE → PENDING)
     */
    @Test
    void submitTeacherApproval_firstTime_pending() {
        User user = new User();
        user.setApprovalStatus(ApprovalStatus.NONE);

        when(userRepository.findById(4L)).thenReturn(Optional.of(user));

        var res = userService.submitTeacherApproval(4L);

        assertEquals(ApprovalStatus.PENDING, user.getApprovalStatus());
        assertNull(user.getApprovedAt());
        assertNotNull(res.get("timestamp"));

        verify(userRepository).save(user);
    }

    /**
     * TC-USER-05
     * Submit teacher approval when already APPROVED
     */
    @Test
    void submitTeacherApproval_alreadyApproved_noChange() {
        User user = new User();
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setApprovedAt(LocalDateTime.now());

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        var res = userService.submitTeacherApproval(5L);

        assertEquals(ApprovalStatus.APPROVED, res.get("approvalStatus"));
        assertEquals(user.getApprovedAt(), res.get("approvedAt"));

        verify(userRepository, never()).save(any());
    }

    /**
     * TC-USER-06
     * Get user roles
     */
    @Test
    void getUserRoles_success() {
        User user = new User();
        Role role = new Role();
        role.setRoleName("TEACHER");
        user.setRole(role);

        when(userRepository.findById(6L)).thenReturn(Optional.of(user));

        List<String> roles = userService.getUserRoles(6L);

        assertEquals(1, roles.size());
        assertEquals("TEACHER", roles.get(0));
    }
}

package com.hokori.web.service;

import com.hokori.web.Enum.ApprovalStatus;
import com.hokori.web.constants.RoleConstants;
import com.hokori.web.dto.ApproveDecisionReq;
import com.hokori.web.dto.ApproveRequestDto;
import com.hokori.web.dto.SubmitApprovalReq;
import com.hokori.web.entity.ProfileApproveRequest;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.mapper.ApprovalMapper;
import com.hokori.web.repository.ProfileApproveRequestItemRepository;
import com.hokori.web.repository.ProfileApproveRequestRepository;
import com.hokori.web.repository.UserCertificateRepository;
import com.hokori.web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherApprovalServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private UserCertificateRepository certRepo;
    @Mock
    private ProfileApproveRequestRepository reqRepo;
    @Mock
    private ProfileApproveRequestItemRepository itemRepo;
    @Mock
    private ApprovalMapper mapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TeacherApprovalService service;

    private User teacher;
    private Role teacherRole;

    @BeforeEach
    void setup() {
        teacherRole = new Role();
        teacherRole.setRoleName(RoleConstants.TEACHER);

        teacher = new User();
        teacher.setId(1L);
        teacher.setRole(teacherRole);
        teacher.setApprovalStatus(ApprovalStatus.NONE);
    }

    /* ============================================================
       SUBMIT APPROVAL
       ============================================================ */

    @Test
    void submitApproval_success() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(teacher));
        when(reqRepo.existsByUser_IdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(false);
        when(certRepo.findByUser_Id(1L)).thenReturn(List.of(mockCertificate()));
        when(mapper.toDto(any(ProfileApproveRequest.class)))
                .thenReturn(mockApproveDto());

        SubmitApprovalReq req = new SubmitApprovalReq(null, null);

        ApproveRequestDto res = service.submitApproval(1L, req);

        assertNotNull(res);
        assertEquals(ApprovalStatus.PENDING, teacher.getApprovalStatus());
        verify(reqRepo).save(any(ProfileApproveRequest.class));
        verify(userRepo).save(teacher);
    }

    @Test
    void submitApproval_fail_whenNotTeacher() {
        teacherRole.setRoleName(RoleConstants.LEARNER);

        when(userRepo.findById(1L)).thenReturn(Optional.of(teacher));

        assertThrows(ResponseStatusException.class,
                () -> service.submitApproval(1L, new SubmitApprovalReq(null, null)));
    }

    @Test
    void submitApproval_fail_whenAlreadyPending() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(teacher));
        when(reqRepo.existsByUser_IdAndStatus(1L, ApprovalStatus.PENDING)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.submitApproval(1L, new SubmitApprovalReq(null, null)));
    }

    /* ============================================================
       ADMIN DECIDE
       ============================================================ */

    @Test
    void adminDecide_approved_updatesUserAndNotify() {
        ProfileApproveRequest req = new ProfileApproveRequest();
        req.setId(100L);
        req.setStatus(ApprovalStatus.PENDING);
        req.setUser(teacher);

        when(reqRepo.findById(100L)).thenReturn(Optional.of(req));
        when(mapper.toDto(any(ProfileApproveRequest.class)))
                .thenReturn(mockApproveDto());

        ApproveDecisionReq decision = new ApproveDecisionReq(
                ApprovalStatus.APPROVED,
                "Looks good"
        );

        service.adminDecide(100L, 99L, decision);

        assertEquals(ApprovalStatus.APPROVED, req.getStatus());
        assertEquals(ApprovalStatus.APPROVED, teacher.getApprovalStatus());
        verify(notificationService)
                .notifyProfileApproved(eq(1L), any());
    }

    @Test
    void adminDecide_rejected_updatesUserAndNotify() {
        ProfileApproveRequest req = new ProfileApproveRequest();
        req.setId(100L);
        req.setStatus(ApprovalStatus.PENDING);
        req.setUser(teacher);

        when(reqRepo.findById(100L)).thenReturn(Optional.of(req));
        when(mapper.toDto(any(ProfileApproveRequest.class)))
                .thenReturn(mockApproveDto());

        ApproveDecisionReq decision = new ApproveDecisionReq(
                ApprovalStatus.REJECTED,
                "Not enough certificates"
        );

        service.adminDecide(100L, 99L, decision);

        assertEquals(ApprovalStatus.REJECTED, req.getStatus());
        assertEquals(ApprovalStatus.REJECTED, teacher.getApprovalStatus());
        verify(notificationService)
                .notifyProfileRejected(eq(1L), any());
    }

    @Test
    void adminDecide_fail_whenAlreadyDecided() {
        ProfileApproveRequest req = new ProfileApproveRequest();
        req.setStatus(ApprovalStatus.APPROVED);

        when(reqRepo.findById(100L)).thenReturn(Optional.of(req));

        ApproveDecisionReq decision = new ApproveDecisionReq(
                ApprovalStatus.REJECTED,
                null
        );

        assertThrows(IllegalStateException.class,
                () -> service.adminDecide(100L, 99L, decision));
    }

    /* ============================================================
       HELPERS
       ============================================================ */

    private ApproveRequestDto mockApproveDto() {
        return mock(ApproveRequestDto.class);
    }

    private com.hokori.web.entity.UserCertificate mockCertificate() {
        com.hokori.web.entity.UserCertificate c =
                new com.hokori.web.entity.UserCertificate();
        c.setUser(teacher);
        return c;
    }
}
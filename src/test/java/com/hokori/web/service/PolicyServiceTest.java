package com.hokori.web.service;

import com.hokori.web.dto.policy.PolicyCreateReq;
import com.hokori.web.dto.policy.PolicyRes;
import com.hokori.web.dto.policy.PolicyUpdateReq;
import com.hokori.web.entity.Policy;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.PolicyRepository;
import com.hokori.web.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PolicyService policyService;

    /* =========================
       CREATE POLICY
       ========================= */

    @Test
    void createPolicy_success() {
        Role role = mockRole();
        User admin = mockAdmin();

        PolicyCreateReq req = new PolicyCreateReq();
        req.setRoleName("ADMIN");
        req.setTitle("Policy title");
        req.setContent("Policy content");

        when(roleRepository.findByRoleName("ADMIN"))
                .thenReturn(Optional.of(role));
        when(currentUserService.getCurrentUserOrThrow())
                .thenReturn(admin);
        when(policyRepository.save(any(Policy.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PolicyRes res = policyService.createPolicy(req);

        assertNotNull(res);
        assertEquals("ADMIN", res.getRoleName());
        assertEquals("Policy title", res.getTitle());
        assertEquals("Policy content", res.getContent());
        assertEquals(admin.getId(), res.getCreatedById());

        verify(policyRepository).save(any(Policy.class));
    }

    @Test
    void createPolicy_roleNotFound_throw404() {
        PolicyCreateReq req = new PolicyCreateReq();
        req.setRoleName("NOT_EXIST");

        when(roleRepository.findByRoleName("NOT_EXIST"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> policyService.createPolicy(req));
    }

    /* =========================
       UPDATE POLICY
       ========================= */

    @Test
    void updatePolicy_success() {
        Policy policy = mockPolicy();

        PolicyUpdateReq req = new PolicyUpdateReq();
        req.setTitle("Updated title");
        req.setContent("Updated content");

        when(policyRepository.findById(1L))
                .thenReturn(Optional.of(policy));
        when(policyRepository.save(policy))
                .thenReturn(policy);

        PolicyRes res = policyService.updatePolicy(1L, req);

        assertEquals("Updated title", res.getTitle());
        assertEquals("Updated content", res.getContent());
    }

    @Test
    void updatePolicy_notFound() {
        when(policyRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> policyService.updatePolicy(1L, new PolicyUpdateReq()));
    }

    /* =========================
       DELETE POLICY
       ========================= */

    @Test
    void deletePolicy_success() {
        Policy policy = mockPolicy();

        when(policyRepository.findById(1L))
                .thenReturn(Optional.of(policy));

        policyService.deletePolicy(1L);

        verify(policyRepository).delete(policy);
    }

    @Test
    void deletePolicy_notFound() {
        when(policyRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> policyService.deletePolicy(1L));
    }

    /* =========================
       GET POLICIES
       ========================= */

    @Test
    void getAllPolicies_success() {
        when(policyRepository.findAllWithRelations())
                .thenReturn(List.of(mockPolicy()));

        List<PolicyRes> res = policyService.getAllPolicies();

        assertEquals(1, res.size());
        assertEquals("ADMIN", res.get(0).getRoleName());
    }

    @Test
    void getPoliciesByRole_success() {
        when(policyRepository.findByRoleName("ADMIN"))
                .thenReturn(List.of(mockPolicy()));

        List<PolicyRes> res = policyService.getPoliciesByRole("ADMIN");

        assertEquals(1, res.size());
        assertEquals("ADMIN", res.get(0).getRoleName());
    }

    @Test
    void getPolicyById_success() {
        when(policyRepository.findById(1L))
                .thenReturn(Optional.of(mockPolicy()));

        PolicyRes res = policyService.getPolicyById(1L);

        assertNotNull(res);
        assertEquals("ADMIN", res.getRoleName());
    }

    /* =========================
       MOCK HELPERS
       ========================= */

    private Policy mockPolicy() {
        Policy p = new Policy();
        p.setId(1L);
        p.setTitle("Policy");
        p.setContent("Content");
        p.setRole(mockRole());
        p.setCreatedBy(mockAdmin());
        return p;
    }

    private Role mockRole() {
        Role r = new Role();
        r.setId(1L);
        r.setRoleName("ADMIN");
        r.setDescription("Admin role");
        return r;
    }

    private User mockAdmin() {
        User u = new User();
        u.setId(99L);
        u.setEmail("admin@hokori.com");
        return u;
    }
}

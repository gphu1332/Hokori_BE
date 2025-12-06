package com.hokori.web.service;

import com.hokori.web.dto.policy.PolicyCreateReq;
import com.hokori.web.dto.policy.PolicyRes;
import com.hokori.web.dto.policy.PolicyUpdateReq;
import com.hokori.web.entity.Policy;
import com.hokori.web.entity.Role;
import com.hokori.web.entity.User;
import com.hokori.web.repository.PolicyRepository;
import com.hokori.web.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserService currentUserService;

    /**
     * Tạo policy mới cho một role
     */
    @Transactional
    public PolicyRes createPolicy(PolicyCreateReq req) {
        // Tìm role
        Role role = roleRepository.findByRoleName(req.getRoleName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Role not found: " + req.getRoleName()
                ));

        // Lấy current user (admin)
        User currentUser = currentUserService.getCurrentUserOrThrow();

        // Tạo policy
        Policy policy = Policy.builder()
                .role(role)
                .title(req.getTitle())
                .content(req.getContent() != null ? req.getContent() : "")
                .createdBy(currentUser)
                .build();

        policy = policyRepository.save(policy);

        return toPolicyRes(policy);
    }

    /**
     * Update policy
     */
    @Transactional
    public PolicyRes updatePolicy(Long policyId, PolicyUpdateReq req) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Policy not found"
                ));

        // Update fields nếu có
        if (req.getTitle() != null && !req.getTitle().trim().isEmpty()) {
            policy.setTitle(req.getTitle());
        }
        if (req.getContent() != null) {
            policy.setContent(req.getContent());
        }

        policy = policyRepository.save(policy);

        return toPolicyRes(policy);
    }

    /**
     * Xóa policy
     */
    @Transactional
    public void deletePolicy(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Policy not found"
                ));

        policyRepository.delete(policy);
    }

    /**
     * Lấy tất cả policies
     */
    @Transactional(readOnly = true)
    public List<PolicyRes> getAllPolicies() {
        return policyRepository.findAllWithRelations().stream()
                .map(this::toPolicyRes)
                .collect(Collectors.toList());
    }

    /**
     * Lấy policies theo role
     */
    @Transactional(readOnly = true)
    public List<PolicyRes> getPoliciesByRole(String roleName) {
        return policyRepository.findByRoleName(roleName).stream()
                .map(this::toPolicyRes)
                .collect(Collectors.toList());
    }

    /**
     * Lấy policy theo ID
     */
    @Transactional(readOnly = true)
    public PolicyRes getPolicyById(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Policy not found"
                ));

        return toPolicyRes(policy);
    }

    /**
     * Convert Policy entity to PolicyRes DTO
     */
    private PolicyRes toPolicyRes(Policy policy) {
        return PolicyRes.builder()
                .id(policy.getId())
                .roleName(policy.getRole().getRoleName())
                .roleDescription(policy.getRole().getDescription())
                .title(policy.getTitle())
                .content(policy.getContent())
                .createdById(policy.getCreatedBy() != null ? policy.getCreatedBy().getId() : null)
                .createdByEmail(policy.getCreatedBy() != null ? policy.getCreatedBy().getEmail() : null)
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}


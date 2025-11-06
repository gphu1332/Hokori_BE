package com.hokori.web.service;

import com.hokori.web.entity.User;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;

/**
 * Service to get current authenticated user from JWT token.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    /** Get Authentication from SecurityContext. */
    private Authentication authOrNull() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** Email/username (principal) from Authentication. */
    private String principalOrThrow() {
        Authentication auth = authOrNull();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated");
        }
        return String.valueOf(auth.getPrincipal()); // bạn đang dùng email làm principal
    }

    /** Lấy User hiện tại (Optional). */
    public Optional<User> getCurrentUser() {
        try {
            String email = principalOrThrow();
            return userRepository.findByEmail(email);
        } catch (ResponseStatusException e) {
            return Optional.empty();
        }
    }

    /** Lấy User hiện tại hoặc throw 401/403. */
    public User getCurrentUserOrThrow() {
        String email = principalOrThrow();
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (Boolean.FALSE.equals(u.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");
        }
        return u;
    }

    /** Trả về userId hoặc null nếu chưa đăng nhập. */
    public Long getUserIdOrNull() {
        return getCurrentUser().map(User::getId).orElse(null);
    }

    /** Trả về userId hoặc throw – dùng đúng với chỗ bạn gọi. */
    public Long getUserIdOrThrow() {
        return getCurrentUserOrThrow().getId();
    }

    /** Kiểm tra role từ authorities; fallback sang field User.role nếu có. */
    public boolean hasRole(String roleName) {
        Authentication auth = authOrNull();
        if (auth != null) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            if (authorities != null && authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName))) {
                return true;
            }
        }
        return getCurrentUser()
                .map(u -> u.getRole() != null && roleName.equalsIgnoreCase(u.getRole().getRoleName()))
                .orElse(false);
    }

    public boolean isAdmin()   { return hasRole("ADMIN"); }
    public boolean isTeacher() { return hasRole("TEACHER"); }
    public boolean isLearner() { return hasRole("LEARNER"); }
}

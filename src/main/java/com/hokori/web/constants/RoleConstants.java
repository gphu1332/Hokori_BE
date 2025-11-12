package com.hokori.web.constants;

import java.util.Map;

/**
 * Constants for system roles.
 * Centralizes role names and descriptions to avoid hardcoding throughout the codebase.
 */
public final class RoleConstants {
    
    private RoleConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // Role names (always uppercase for consistency with PostgreSQL)
    public static final String LEARNER = "LEARNER";
    public static final String TEACHER = "TEACHER";
    public static final String STAFF = "STAFF";
    public static final String ADMIN = "ADMIN";
    
    // Default role descriptions
    public static final Map<String, String> ROLE_DESCRIPTIONS = Map.of(
        LEARNER, "Regular student/learner",
        TEACHER, "Teacher who can create content",
        STAFF, "Staff member with limited admin access",
        ADMIN, "Full system administrator"
    );
    
    // Default roles array (for initialization)
    public static final String[] DEFAULT_ROLE_NAMES = {LEARNER, TEACHER, STAFF, ADMIN};
    
    /**
     * Get description for a role name.
     * @param roleName Role name (will be normalized to uppercase)
     * @return Role description or null if role not found
     */
    public static String getDescription(String roleName) {
        if (roleName == null) return null;
        return ROLE_DESCRIPTIONS.get(roleName.trim().toUpperCase());
    }
    
    /**
     * Check if a role name is a valid default role.
     * @param roleName Role name to check
     * @return true if role is one of the default roles
     */
    public static boolean isDefaultRole(String roleName) {
        if (roleName == null) return false;
        String normalized = roleName.trim().toUpperCase();
        return normalized.equals(LEARNER) || 
               normalized.equals(TEACHER) || 
               normalized.equals(STAFF) || 
               normalized.equals(ADMIN);
    }
}


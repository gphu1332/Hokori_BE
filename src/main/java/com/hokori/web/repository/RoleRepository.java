package com.hokori.web.repository;

import com.hokori.web.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    // Find by role name
    Optional<Role> findByRoleName(String roleName);
    
    // Check if role exists by name
    boolean existsByRoleName(String roleName);
    
    // Find roles by description containing text
    List<Role> findByDescriptionContaining(String description);
    
    // Find roles with users
    @Query("SELECT r FROM Role r WHERE EXISTS (SELECT u FROM User u WHERE u.roleId = r.id)")
    List<Role> findRolesWithUsers();
    
    // Count users by role
    @Query("SELECT r.roleName, COUNT(u) FROM Role r " +
           "LEFT JOIN User u ON u.roleId = r.id " +
           "GROUP BY r.id, r.roleName")
    List<Object[]> countUsersByRole();
    
    // Find roles created after date
    @Query("SELECT r FROM Role r WHERE r.createdAt >= :date")
    List<Role> findRolesCreatedAfter(@Param("date") java.time.LocalDateTime date);
    
    // Find roles by name pattern
    @Query("SELECT r FROM Role r WHERE LOWER(r.roleName) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    List<Role> findRolesByNamePattern(@Param("pattern") String pattern);
    
    // Find roles with specific user count
    @Query("SELECT r FROM Role r " +
           "WHERE (SELECT COUNT(u) FROM User u WHERE u.roleId = r.id) = :userCount")
    List<Role> findRolesWithUserCount(@Param("userCount") long userCount);
    
    // Find roles with no users
    @Query("SELECT r FROM Role r " +
           "WHERE NOT EXISTS (SELECT u FROM User u WHERE u.roleId = r.id)")
    List<Role> findRolesWithNoUsers();
    
    // Find roles with most users
    @Query("SELECT r FROM Role r " +
           "WHERE (SELECT COUNT(u) FROM User u WHERE u.roleId = r.id) = " +
           "      (SELECT MAX((SELECT COUNT(u2) FROM User u2 WHERE u2.roleId = r2.id)) " +
           "       FROM Role r2)")
    List<Role> findRolesWithMostUsers();
}
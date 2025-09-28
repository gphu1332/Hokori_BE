package com.hokori.web.repository;

import com.hokori.web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Find by Firebase UID
    Optional<User> findByFirebaseUid(String firebaseUid);
    
    // Find by email
    Optional<User> findByEmail(String email);
    
    // Find by username
    Optional<User> findByUsername(String username);
    
    // Find active users
    List<User> findByIsActiveTrue();
    
    // Find verified users
    List<User> findByIsVerifiedTrue();
    
    // Find users by role
    @Query("SELECT u FROM User u WHERE u.roleId = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);
    
    // Find users by role name
    @Query("SELECT u FROM User u JOIN u.role r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    // Find users with specific JLPT level
    List<User> findByCurrentJlptLevel(User.JLPTLevel level);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Check if username exists
    boolean existsByUsername(String username);
    
    // Check if Firebase UID exists
    boolean existsByFirebaseUid(String firebaseUid);
    
    // Find users by country
    List<User> findByCountry(String country);
    
    // Find users by learning language
    List<User> findByLearningLanguage(String learningLanguage);
    
    // Find users created after date
    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findUsersCreatedAfter(@Param("date") java.time.LocalDateTime date);
    
    // Count users by role
    @Query("SELECT COUNT(u) FROM User u WHERE u.roleId = :roleId")
    long countByRoleId(@Param("roleId") Long roleId);
    
    // Search users by display name or username
    @Query("SELECT u FROM User u " +
           "WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);
    
    // Find users by verification status
    List<User> findByIsVerified(Boolean isVerified);
    
    // Find users by active status
    List<User> findByIsActive(Boolean isActive);
    
    // Find users with recent login
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :date")
    List<User> findUsersWithRecentLogin(@Param("date") java.time.LocalDateTime date);
}
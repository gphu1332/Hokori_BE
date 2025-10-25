package com.hokori.web.repository;

import com.hokori.web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByFirebaseUid(String firebaseUid);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByFirebaseUid(String firebaseUid);

    // ------------ Common filters ------------
    List<User> findByIsActiveTrue();
    List<User> findByIsVerifiedTrue();
    List<User> findByIsActive(Boolean isActive);
    List<User> findByIsVerified(Boolean isVerified);

    List<User> findByCountry(String country);
    List<User> findByLearningLanguage(String learningLanguage);

    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :date")
    List<User> findUsersWithRecentLogin(@Param("date") LocalDateTime date);

    // ------------ Fetch-join to avoid LazyInitialization on serialization ------------
    @Query("select distinct u from User u left join fetch u.role")
    List<User> findAllWithRole();

    @Query("SELECT u FROM User u JOIN FETCH u.role r WHERE r.id = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT u FROM User u JOIN FETCH u.role r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM User u JOIN u.role r WHERE r.id = :roleId")
    long countByRoleId(@Param("roleId") Long roleId);

    @Query("""
           SELECT u FROM User u
           LEFT JOIN FETCH u.role
           WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(u.username)    LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(u.email)       LIKE LOWER(CONCAT('%', :q, '%'))
           """)
    List<User> searchUsers(@Param("q") String searchTerm);

    @Query("select u from User u left join fetch u.role where u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);
}

package com.hokori.web.repository;

import com.hokori.web.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Case-insensitive search for PostgreSQL compatibility
    @Query("SELECT r FROM Role r WHERE UPPER(r.roleName) = UPPER(:roleName)")
    Optional<Role> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Role r WHERE UPPER(r.roleName) = UPPER(:roleName)")
    boolean existsByRoleName(@Param("roleName") String roleName);

    List<Role> findByDescriptionContaining(String description);

    // Roles có ít nhất 1 user
    @Query("""
           SELECT r FROM Role r
           WHERE EXISTS (SELECT u FROM User u WHERE u.role = r)
           """)
    List<Role> findRolesWithUsers();

    // Đếm user theo từng role (trả về Object[]: [0]=roleName(String), [1]=count(Long))
    @Query("""
           SELECT r.roleName, COUNT(u)
           FROM Role r
           LEFT JOIN r.users u
           GROUP BY r.id, r.roleName
           """)
    List<Object[]> countUsersByRole();

    // Roles tạo sau 1 thời điểm
    @Query("SELECT r FROM Role r WHERE r.createdAt >= :date")
    List<Role> findRolesCreatedAfter(@Param("date") LocalDateTime date);

    // Tìm theo pattern tên
    @Query("SELECT r FROM Role r WHERE LOWER(r.roleName) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    List<Role> findRolesByNamePattern(@Param("pattern") String pattern);

    // Roles có đúng số user = :userCount
    @Query("""
           SELECT r FROM Role r
           WHERE (SELECT COUNT(u) FROM User u WHERE u.role = r) = :userCount
           """)
    List<Role> findRolesWithUserCount(@Param("userCount") long userCount);

    // Roles không có user nào
    @Query("""
           SELECT r FROM Role r
           WHERE NOT EXISTS (SELECT u FROM User u WHERE u.role = r)
           """)
    List<Role> findRolesWithNoUsers();

    // Roles có nhiều user nhất
    @Query("""
           SELECT r FROM Role r
           WHERE (SELECT COUNT(u) FROM User u WHERE u.role = r) = (
               SELECT MAX(
                   (SELECT COUNT(u2) FROM User u2 WHERE u2.role = r2)
               )
               FROM Role r2
           )
           """)
    List<Role> findRolesWithMostUsers();
}

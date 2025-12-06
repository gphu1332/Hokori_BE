package com.hokori.web.repository;

import com.hokori.web.entity.Policy;
import com.hokori.web.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    /**
     * Tìm tất cả policies với JOIN FETCH để eager load role và createdBy
     */
    @Query("SELECT DISTINCT p FROM Policy p " +
           "LEFT JOIN FETCH p.role " +
           "LEFT JOIN FETCH p.createdBy " +
           "ORDER BY p.createdAt DESC")
    List<Policy> findAllWithRelations();

    /**
     * Tìm tất cả policies theo role
     */
    List<Policy> findByRole(Role role);

    /**
     * Tìm tất cả policies theo roleName (case-insensitive)
     * JOIN FETCH để eager load role và createdBy
     */
    @Query("SELECT p FROM Policy p " +
           "LEFT JOIN FETCH p.role " +
           "LEFT JOIN FETCH p.createdBy " +
           "WHERE UPPER(p.role.roleName) = UPPER(:roleName) " +
           "ORDER BY p.createdAt DESC")
    List<Policy> findByRoleName(@Param("roleName") String roleName);

    /**
     * Tìm policy theo ID và roleName (để verify ownership)
     * JOIN FETCH để eager load role và createdBy
     */
    @Query("SELECT p FROM Policy p " +
           "LEFT JOIN FETCH p.role " +
           "LEFT JOIN FETCH p.createdBy " +
           "WHERE p.id = :id AND UPPER(p.role.roleName) = UPPER(:roleName)")
    Optional<Policy> findByIdAndRoleName(@Param("id") Long id, @Param("roleName") String roleName);

    /**
     * Đếm số policies theo role
     */
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.role = :role")
    long countByRole(@Param("role") Role role);

    /**
     * Tìm policies theo createdBy user
     * JOIN FETCH để eager load role và createdBy
     */
    @Query("SELECT p FROM Policy p " +
           "LEFT JOIN FETCH p.role " +
           "LEFT JOIN FETCH p.createdBy " +
           "WHERE p.createdBy.id = :userId")
    List<Policy> findByCreatedBy_Id(@Param("userId") Long userId);
}


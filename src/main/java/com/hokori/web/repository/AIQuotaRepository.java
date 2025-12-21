package com.hokori.web.repository;

import com.hokori.web.entity.AIQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIQuotaRepository extends JpaRepository<AIQuota, Long> {
    
    /**
     * Find unified quota for user (one quota per user)
     * Returns the first one if multiple exist (for migration compatibility)
     */
    @Query("SELECT q FROM AIQuota q WHERE q.user.id = :userId ORDER BY q.id ASC")
    List<AIQuota> findAllByUser_Id(@Param("userId") Long userId);
    
    /**
     * Find first quota for user (helper method)
     */
    default Optional<AIQuota> findByUser_Id(Long userId) {
        List<AIQuota> quotas = findAllByUser_Id(userId);
        return quotas.isEmpty() ? Optional.empty() : Optional.of(quotas.get(0));
    }
    
    boolean existsByUser_Id(Long userId);
}


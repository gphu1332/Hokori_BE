package com.hokori.web.repository;

import com.hokori.web.entity.AIQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIQuotaRepository extends JpaRepository<AIQuota, Long> {
    
    /**
     * Find unified quota for user (one quota per user)
     */
    Optional<AIQuota> findByUser_Id(Long userId);
    
    boolean existsByUser_Id(Long userId);
}


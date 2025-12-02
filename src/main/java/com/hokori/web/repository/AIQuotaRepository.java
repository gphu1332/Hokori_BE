package com.hokori.web.repository;

import com.hokori.web.Enum.AIServiceType;
import com.hokori.web.entity.AIQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIQuotaRepository extends JpaRepository<AIQuota, Long> {
    
    Optional<AIQuota> findByUser_IdAndServiceType(Long userId, AIServiceType serviceType);
    
    List<AIQuota> findByUser_Id(Long userId);
    
    boolean existsByUser_IdAndServiceType(Long userId, AIServiceType serviceType);
}


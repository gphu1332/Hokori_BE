package com.hokori.web.repository;

import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.entity.AIPackagePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AIPackagePurchaseRepository extends JpaRepository<AIPackagePurchase, Long> {
    
    /**
     * Find active purchase for a user
     */
    Optional<AIPackagePurchase> findByUser_IdAndIsActiveTrue(Long userId);
    
    /**
     * Find all purchases for a user (for history)
     */
    List<AIPackagePurchase> findByUser_IdOrderByPurchasedAtDesc(Long userId);
    
    /**
     * Find purchases by payment status
     */
    List<AIPackagePurchase> findByUser_IdAndPaymentStatus(Long userId, PaymentStatus status);
    
    /**
     * Find expired purchases that need to be deactivated
     */
    @Query("SELECT p FROM AIPackagePurchase p WHERE p.isActive = true AND p.expiresAt < :now")
    List<AIPackagePurchase> findExpiredActivePurchases(@Param("now") Instant now);
}


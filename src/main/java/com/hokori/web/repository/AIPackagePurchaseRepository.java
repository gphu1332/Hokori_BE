package com.hokori.web.repository;

import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.entity.AIPackagePurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AIPackagePurchaseRepository extends JpaRepository<AIPackagePurchase, Long> {
    
    /**
     * Find active purchase for a user with eager fetch of aiPackage
     * If multiple active purchases exist, returns the most recent one
     */
    @Query("SELECT p FROM AIPackagePurchase p " +
           "LEFT JOIN FETCH p.aiPackage " +
           "WHERE p.user.id = :userId AND p.isActive = true " +
           "ORDER BY p.purchasedAt DESC NULLS LAST, p.id DESC")
    List<AIPackagePurchase> findByUser_IdAndIsActiveTrue(@Param("userId") Long userId);
    
    /**
     * Find the first active purchase for a user (most recent if multiple exist)
     * Helper method to avoid "Query did not return a unique result" error
     */
    default java.util.Optional<AIPackagePurchase> findFirstByUser_IdAndIsActiveTrue(Long userId) {
        List<AIPackagePurchase> purchases = findByUser_IdAndIsActiveTrue(userId);
        return purchases.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(purchases.get(0));
    }
    
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
    
    /**
     * Count active purchases for a package
     */
    long countByAiPackage_IdAndIsActiveTrue(Long packageId);
}


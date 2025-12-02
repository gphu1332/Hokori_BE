package com.hokori.web.service;

import com.hokori.web.Enum.AIServiceType;
import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.dto.ai.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for AI Package management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AIPackageService {

    private final AIPackageRepository packageRepo;
    private final AIPackagePurchaseRepository purchaseRepo;
    private final AIQuotaRepository quotaRepo;
    private final UserRepository userRepo;

    // =========================
    // 1. List AI Packages
    // =========================
    
    @Transactional(readOnly = true)
    public List<AIPackageResponse> listPackages() {
        List<AIPackage> packages = packageRepo.findByIsActiveTrueOrderByDisplayOrderAsc();
        return packages.stream()
                .map(this::toPackageResponse)
                .collect(Collectors.toList());
    }

    // =========================
    // 2. Check if user has AI package
    // =========================
    
    @Transactional(readOnly = true)
    public MyAIPackageResponse getMyPackage(Long userId) {
        Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findByUser_IdAndIsActiveTrue(userId);
        
        if (purchaseOpt.isEmpty()) {
            return MyAIPackageResponse.builder()
                    .hasPackage(false)
                    .isActive(false)
                    .isExpired(false)
                    .build();
        }

        AIPackagePurchase purchase = purchaseOpt.get();
        Instant now = Instant.now();
        boolean isExpired = purchase.getExpiresAt() != null && purchase.getExpiresAt().isBefore(now);

        return MyAIPackageResponse.builder()
                .hasPackage(true)
                .packageId(purchase.getAiPackage().getId())
                .packageName(purchase.getAiPackage().getName())
                .purchasedAt(purchase.getPurchasedAt())
                .expiresAt(purchase.getExpiresAt())
                .paymentStatus(purchase.getPaymentStatus().name())
                .isActive(purchase.getIsActive() && !isExpired)
                .isExpired(isExpired)
                .build();
    }

    // =========================
    // 3. Check quota for each service
    // =========================
    
    @Transactional(readOnly = true)
    public AIQuotaResponse getMyQuota(Long userId) {
        List<AIQuota> quotas = quotaRepo.findByUser_Id(userId);
        
        Map<String, AIQuotaResponse.QuotaInfo> quotaMap = new HashMap<>();
        
        // Initialize all service types
        for (AIServiceType serviceType : AIServiceType.values()) {
            AIQuota quota = quotas.stream()
                    .filter(q -> q.getServiceType() == serviceType)
                    .findFirst()
                    .orElse(null);
            
            AIQuotaResponse.QuotaInfo quotaInfo = AIQuotaResponse.QuotaInfo.builder()
                    .remainingQuota(quota != null ? quota.getRemainingQuota() : null)
                    .totalQuota(quota != null ? quota.getTotalQuota() : null)
                    .hasQuota(quota == null || quota.hasQuota())
                    .build();
            
            quotaMap.put(serviceType.name(), quotaInfo);
        }
        
        return AIQuotaResponse.builder()
                .quotas(quotaMap)
                .build();
    }

    // =========================
    // 4. Create purchase (without payment)
    // =========================
    
    public AIPackagePurchaseResponse createPurchase(Long userId, AIPackagePurchaseRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        AIPackage aiPackage = packageRepo.findById(request.getPackageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI Package not found"));
        
        if (!aiPackage.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI Package is not active");
        }

        // Check if user already has an active purchase
        Optional<AIPackagePurchase> existingPurchase = purchaseRepo.findByUser_IdAndIsActiveTrue(userId);
        if (existingPurchase.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "User already has an active AI package. Please wait for it to expire or cancel it first.");
        }

        // Create purchase with PENDING status (payment will be integrated later)
        AIPackagePurchase purchase = new AIPackagePurchase();
        purchase.setUser(user);
        purchase.setAiPackage(aiPackage);
        purchase.setPurchasePriceCents(aiPackage.getPriceCents());
        purchase.setPaymentStatus(PaymentStatus.PENDING);
        purchase.setPurchasedAt(Instant.now());
        
        // Calculate expiry date
        Instant expiresAt = Instant.now().plusSeconds(aiPackage.getDurationDays() * 24L * 60L * 60L);
        purchase.setExpiresAt(expiresAt);
        purchase.setIsActive(false);  // Will be activated after payment
        
        purchase = purchaseRepo.save(purchase);
        
        log.info("Created AI package purchase: userId={}, packageId={}, purchaseId={}", 
                userId, request.getPackageId(), purchase.getId());
        
        return toPurchaseResponse(purchase);
    }

    // =========================
    // 5. Use AI service (deduct quota)
    // =========================
    
    public void useAIService(Long userId, AIServiceType serviceType, int amount) {
        AIQuota quota = quotaRepo.findByUser_IdAndServiceType(userId, serviceType)
                .orElse(null);
        
        if (quota == null) {
            // User doesn't have quota for this service
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "No quota available for service: " + serviceType.name());
        }
        
        if (!quota.hasQuota()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Quota exhausted for service: " + serviceType.name());
        }
        
        // Deduct quota
        quota.useQuota(amount);
        quotaRepo.save(quota);
        
        log.info("Used AI service quota: userId={}, serviceType={}, amount={}, remaining={}", 
                userId, serviceType, amount, quota.getRemainingQuota());
    }

    // =========================
    // Helper methods
    // =========================
    
    private AIPackageResponse toPackageResponse(AIPackage pkg) {
        return AIPackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .priceCents(pkg.getPriceCents())
                .currency(pkg.getCurrency())
                .durationDays(pkg.getDurationDays())
                .grammarQuota(pkg.getGrammarQuota())
                .kaiwaQuota(pkg.getKaiwaQuota())
                .pronunQuota(pkg.getPronunQuota())
                .isActive(pkg.getIsActive())
                .displayOrder(pkg.getDisplayOrder())
                .build();
    }
    
    private AIPackagePurchaseResponse toPurchaseResponse(AIPackagePurchase purchase) {
        return AIPackagePurchaseResponse.builder()
                .id(purchase.getId())
                .packageId(purchase.getAiPackage().getId())
                .packageName(purchase.getAiPackage().getName())
                .purchasePriceCents(purchase.getPurchasePriceCents())
                .paymentStatus(purchase.getPaymentStatus().name())
                .purchasedAt(purchase.getPurchasedAt())
                .expiresAt(purchase.getExpiresAt())
                .isActive(purchase.getIsActive())
                .transactionId(purchase.getTransactionId())
                .build();
    }
}


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
import java.util.List;
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
        Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findFirstByUser_IdAndIsActiveTrue(userId);
        
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
        // Get unified quota for user
        Optional<AIQuota> quotaOpt = quotaRepo.findByUser_Id(userId);
        
        if (quotaOpt.isEmpty()) {
            // No quota record - return unlimited
            return AIQuotaResponse.builder()
                    .totalRequests(null)
                    .usedRequests(0)
                    .remainingRequests(null)
                    .hasQuota(true)
                    .build();
        }
        
        AIQuota quota = quotaOpt.get();
        return AIQuotaResponse.builder()
                .totalRequests(quota.getTotalRequests())
                .usedRequests(quota.getUsedRequests() != null ? quota.getUsedRequests() : 0)
                .remainingRequests(quota.getRemainingRequests())
                .hasQuota(quota.hasQuota())
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
        Optional<AIPackagePurchase> existingPurchase = purchaseRepo.findFirstByUser_IdAndIsActiveTrue(userId);
        if (existingPurchase.isPresent()) {
            var purchase = existingPurchase.get();
            Instant now = Instant.now();
            
            // Check if package is expired
            boolean isExpired = purchase.getExpiresAt() != null && purchase.getExpiresAt().isBefore(now);
            
            // Check if quota is exhausted
            boolean quotaExhausted = checkQuotaExhausted(userId);
            
            if (!isExpired && !quotaExhausted) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "User already has an active AI package. Please wait for it to expire or quota to be exhausted before purchasing a new package.");
            }
            
            // If expired or quota exhausted, allow purchase
            if (isExpired) {
                log.info("User {} has expired package {}, allowing new purchase via createPurchase", userId, purchase.getAiPackage().getName());
            }
            if (quotaExhausted) {
                log.info("User {} has exhausted quota for package {}, allowing new purchase via createPurchase", userId, purchase.getAiPackage().getName());
            }
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
    // 5. Check if user can use AI service (has quota or is MODERATOR)
    // =========================
    
    @Transactional(readOnly = true)
    public boolean canUseAIService(Long userId, AIServiceType serviceType) {
        // Check if user is MODERATOR (MODERATOR has unlimited access)
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (user.getRole() != null && "MODERATOR".equalsIgnoreCase(user.getRole().getRoleName())) {
            return true; // MODERATOR has unlimited access
        }
        
        // Check if user has active package purchase
        Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findFirstByUser_IdAndIsActiveTrue(userId);
        
        if (purchaseOpt.isPresent()) {
            // User has active package - check unified quota
            AIPackagePurchase purchase = purchaseOpt.get();
            Instant now = Instant.now();
            
            // Check if purchase is expired
            if (purchase.getExpiresAt() != null && purchase.getExpiresAt().isBefore(now)) {
                // Package expired - fall back to free tier
                return checkFreeTierQuota(userId);
            }
            
            // Check if payment is successful
            if (purchase.getPaymentStatus() != PaymentStatus.PAID) {
                // Payment not completed - fall back to free tier
                return checkFreeTierQuota(userId);
            }
            
            // Check unified quota
            Optional<AIQuota> quotaOpt = quotaRepo.findByUser_Id(userId);
            if (quotaOpt.isEmpty()) {
                // No quota record means unlimited (if package allows)
                AIPackage aiPackage = purchase.getAiPackage();
                return aiPackage.getTotalRequests() == null; // null = unlimited
            }
            
            AIQuota quota = quotaOpt.get();
            return quota.hasQuota(); // Check if remaining requests > 0
        } else {
            // No active package - check free tier quota
            return checkFreeTierQuota(userId);
        }
    }
    
    /**
     * Check free tier quota for user
     * Free tier: 50 unified requests per month
     */
    private boolean checkFreeTierQuota(Long userId) {
        int freeTierLimit = 50; // Unified requests per month
        
        Optional<AIQuota> quotaOpt = quotaRepo.findByUser_Id(userId);
        
        if (quotaOpt.isEmpty()) {
            // No quota record - allow first use, quota will be created when used
            return true;
        }
        
        AIQuota quota = quotaOpt.get();
        
        // Check if quota needs monthly reset (free tier resets monthly)
        Instant now = Instant.now();
        if (quota.getLastResetAt() == null || 
            quota.getLastResetAt().isBefore(now.minusSeconds(30L * 24 * 60 * 60))) { // 30 days ago
            // Reset free tier quota monthly
            quota.initializeQuota(freeTierLimit);
            quotaRepo.save(quota);
            return true;
        }
        
        // Check if user has free tier quota remaining
        return quota.hasQuota();
    }
    
    /**
     * Check if quota is exhausted for user's active package
     * Public method để các service khác có thể gọi
     */
    public boolean checkQuotaExhausted(Long userId) {
        Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findFirstByUser_IdAndIsActiveTrue(userId);
        if (purchaseOpt.isEmpty()) {
            return false; // No active package, not exhausted
        }
        
        AIPackagePurchase purchase = purchaseOpt.get();
        if (purchase.getPaymentStatus() != PaymentStatus.PAID) {
            return false; // Payment not completed, not exhausted
        }
        
        Optional<AIQuota> quotaOpt = quotaRepo.findByUser_Id(userId);
        if (quotaOpt.isEmpty()) {
            // No quota record means unlimited (not exhausted)
            return false;
        }
        
        AIQuota quota = quotaOpt.get();
        // If remaining requests > 0, not exhausted
        return quota.getRemainingRequests() != null && quota.getRemainingRequests() <= 0;
    }
    
    // =========================
    // 6. Use AI service (deduct quota)
    // =========================
    
    /**
     * Get cost (in requests) for a service type
     * Conversation costs more because it uses multiple API calls
     */
    private int getServiceCost(AIServiceType serviceType) {
        return switch (serviceType) {
            case GRAMMAR -> 1;      // 1 request
            case KAIWA -> 1;        // 1 request
            case PRONUN -> 1;       // 1 request
            case CONVERSATION -> 3; // 3 requests (more complex, uses multiple APIs)
        };
    }
    
    public void useAIService(Long userId, AIServiceType serviceType, int amount) {
        // Calculate actual cost based on service type
        int serviceCost = getServiceCost(serviceType);
        int totalCost = serviceCost * amount; // Multiply by amount if needed
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Optional<AIQuota> quotaOpt = quotaRepo.findByUser_Id(userId);
        AIQuota quota;
        
        if (quotaOpt.isEmpty()) {
            // No quota record - check if user has active package or use free tier
            Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findFirstByUser_IdAndIsActiveTrue(userId);
            
            if (purchaseOpt.isPresent() && purchaseOpt.get().getPaymentStatus() == PaymentStatus.PAID) {
                // User has paid package - allocate quota from package
                AIPackagePurchase purchase = purchaseOpt.get();
                AIPackage aiPackage = purchase.getAiPackage();
                Integer packageRequests = aiPackage.getTotalRequests();
                
                if (packageRequests == null) {
                    // Unlimited quota - no need to track
                    log.info("Used AI service (unlimited): userId={}, serviceType={}, amount={}", 
                            userId, serviceType, amount);
                    return;
                }
                
                // Create unified quota from package
                quota = new AIQuota();
                quota.setUser(user);
                quota.initializeQuota(packageRequests);
                quota.setDeletedFlag(false);
            } else {
                // Use free tier - initialize with 50 unified requests per month
                int freeTierLimit = 50;
                quota = new AIQuota();
                quota.setUser(user);
                quota.initializeQuota(freeTierLimit);
                quota.setDeletedFlag(false);
            }
        } else {
            quota = quotaOpt.get();
            
            // Check if free tier quota needs monthly reset
            Instant now = Instant.now();
            int freeTierLimit = 50;
            if (quota.getTotalRequests() != null && quota.getTotalRequests() == freeTierLimit && 
                quota.getLastResetAt() != null &&
                quota.getLastResetAt().isBefore(now.minusSeconds(30L * 24 * 60 * 60))) {
                // Reset free tier quota monthly
                quota.initializeQuota(freeTierLimit);
            }
        }
        
        if (!quota.hasQuota()) {
            // Check if user has active package
            Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findFirstByUser_IdAndIsActiveTrue(userId);
            if (purchaseOpt.isEmpty() || purchaseOpt.get().getPaymentStatus() != PaymentStatus.PAID) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Free tier quota exhausted. Please purchase an AI package to continue using this feature.");
            } else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Request quota exhausted. Please upgrade your package.");
            }
        }
        
        // Deduct requests (use calculated cost)
        quota.useRequests(totalCost);
        quotaRepo.save(quota);
        
        log.info("Used AI service requests: userId={}, serviceType={}, serviceCost={}, amount={}, totalCost={}, remaining={}, total={}", 
                userId, serviceType, serviceCost, amount, totalCost, quota.getRemainingRequests(), quota.getTotalRequests());
    }

    // =========================
    // 6. Admin: Create AI Package
    // =========================
    
    public AIPackageResponse createPackage(AIPackageCreateReq req) {
        AIPackage aiPackage = new AIPackage();
        aiPackage.setName(req.getName());
        aiPackage.setDescription(req.getDescription());
        aiPackage.setPriceCents(req.getPriceCents());
        aiPackage.setCurrency(req.getCurrency() != null ? req.getCurrency() : "VND");
        aiPackage.setDurationDays(req.getDurationDays());
        aiPackage.setTotalRequests(req.getTotalRequests());
        aiPackage.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        aiPackage.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
        
        aiPackage = packageRepo.save(aiPackage);
        
        log.info("Created AI package: id={}, name={}", aiPackage.getId(), aiPackage.getName());
        
        return toPackageResponse(aiPackage);
    }

    // =========================
    // 7. Admin: Update AI Package
    // =========================
    
    public AIPackageResponse updatePackage(Long packageId, AIPackageUpdateReq req) {
        AIPackage aiPackage = packageRepo.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI Package not found"));
        
        if (req.getName() != null) {
            aiPackage.setName(req.getName());
        }
        if (req.getDescription() != null) {
            aiPackage.setDescription(req.getDescription());
        }
        if (req.getPriceCents() != null) {
            aiPackage.setPriceCents(req.getPriceCents());
        }
        if (req.getCurrency() != null) {
            aiPackage.setCurrency(req.getCurrency());
        }
        if (req.getDurationDays() != null) {
            aiPackage.setDurationDays(req.getDurationDays());
        }
        if (req.getTotalRequests() != null) {
            aiPackage.setTotalRequests(req.getTotalRequests());
        }
        if (req.getIsActive() != null) {
            aiPackage.setIsActive(req.getIsActive());
        }
        if (req.getDisplayOrder() != null) {
            aiPackage.setDisplayOrder(req.getDisplayOrder());
        }
        
        aiPackage = packageRepo.save(aiPackage);
        
        log.info("Updated AI package: id={}, name={}", aiPackage.getId(), aiPackage.getName());
        
        return toPackageResponse(aiPackage);
    }

    // =========================
    // 8. Admin: Delete AI Package
    // =========================
    
    public void deletePackage(Long packageId) {
        AIPackage aiPackage = packageRepo.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI Package not found"));
        
        // Check if there are active purchases
        long activePurchases = purchaseRepo.countByAiPackage_IdAndIsActiveTrue(packageId);
        if (activePurchases > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Cannot delete package with active purchases. Please deactivate it instead.");
        }
        
        // Check total purchases (including expired)
        long totalPurchases = purchaseRepo.countByAiPackage_Id(packageId);
        if (totalPurchases > 0) {
            log.warn("Deleting package {} with {} expired purchases (no active purchases)", 
                    packageId, totalPurchases);
        }
        
        packageRepo.delete(aiPackage);
        
        log.info("Deleted AI package: id={}, name={}", packageId, aiPackage.getName());
    }

    // =========================
    // 9. Admin: Get all packages (including inactive)
    // =========================
    
    @Transactional(readOnly = true)
    public List<AIPackageResponse> getAllPackages() {
        List<AIPackage> packages = packageRepo.findAllByOrderByDisplayOrderAsc();
        return packages.stream()
                .map(this::toPackageResponse)
                .collect(Collectors.toList());
    }

    // =========================
    // 10. Admin: Get package by ID
    // =========================
    
    @Transactional(readOnly = true)
    public AIPackageResponse getPackageById(Long packageId) {
        AIPackage aiPackage = packageRepo.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI Package not found"));
        
        return toPackageResponse(aiPackage);
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
                .totalRequests(pkg.getTotalRequests())
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


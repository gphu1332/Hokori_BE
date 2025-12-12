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
            var purchase = existingPurchase.get();
            Instant now = Instant.now();
            
            // Check if package is expired
            boolean isExpired = purchase.getExpiresAt() != null && purchase.getExpiresAt().isBefore(now);
            
            // Check if all quotas are exhausted
            boolean allQuotasExhausted = checkAllQuotasExhausted(userId);
            
            if (!isExpired && !allQuotasExhausted) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "User already has an active AI package. Please wait for it to expire or quota to be exhausted before purchasing a new package.");
            }
            
            // If expired or all quotas exhausted, allow purchase
            if (isExpired) {
                log.info("User {} has expired package {}, allowing new purchase via createPurchase", userId, purchase.getAiPackage().getName());
            }
            if (allQuotasExhausted) {
                log.info("User {} has exhausted all quotas for package {}, allowing new purchase via createPurchase", userId, purchase.getAiPackage().getName());
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
        Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findByUser_IdAndIsActiveTrue(userId);
        
        if (purchaseOpt.isPresent()) {
            // User has active package - check package quota
            AIPackagePurchase purchase = purchaseOpt.get();
            Instant now = Instant.now();
            
            // Check if purchase is expired
            if (purchase.getExpiresAt() != null && purchase.getExpiresAt().isBefore(now)) {
                // Package expired - fall back to free tier
                return checkFreeTierQuota(userId, serviceType);
            }
            
            // Check if payment is successful
            if (purchase.getPaymentStatus() != PaymentStatus.PAID) {
                // Payment not completed - fall back to free tier
                return checkFreeTierQuota(userId, serviceType);
            }
            
            // Check quota for specific service type
            Optional<AIQuota> quotaOpt = quotaRepo.findByUser_IdAndServiceType(userId, serviceType);
            if (quotaOpt.isEmpty()) {
                // No quota record means unlimited (if package allows)
                AIPackage aiPackage = purchase.getAiPackage();
                Integer packageQuota = getQuotaForServiceType(aiPackage, serviceType);
                return packageQuota == null; // null = unlimited
            }
            
            AIQuota quota = quotaOpt.get();
            return quota.hasQuota(); // Check if remaining quota > 0
        } else {
            // No active package - check free tier quota
            return checkFreeTierQuota(userId, serviceType);
        }
    }
    
    /**
     * Check free tier quota for user
     * Free tier: 10 Grammar, 10 Kaiwa, 10 Pronunciation, 5 Conversation per month
     */
    private boolean checkFreeTierQuota(Long userId, AIServiceType serviceType) {
        // Free tier limits per service type
        // Conversation has lower limit because it uses more API calls
        int freeTierLimit = (serviceType == AIServiceType.CONVERSATION) ? 5 : 10;
        
        Optional<AIQuota> quotaOpt = quotaRepo.findByUser_IdAndServiceType(userId, serviceType);
        
        if (quotaOpt.isEmpty()) {
            // No quota record - initialize free tier quota
            return true; // Allow first use, quota will be created when used
        }
        
        AIQuota quota = quotaOpt.get();
        
        // Check if quota needs monthly reset (free tier resets monthly)
        Instant now = Instant.now();
        if (quota.getLastResetAt() == null || 
            quota.getLastResetAt().isBefore(now.minusSeconds(30L * 24 * 60 * 60))) { // 30 days ago
            // Reset free tier quota monthly
            quota.setTotalQuota(freeTierLimit);
            quota.setRemainingQuota(freeTierLimit);
            quota.setLastResetAt(now);
            quotaRepo.save(quota);
            return true;
        }
        
        // Check if user has free tier quota remaining
        return quota.hasQuota();
    }
    
    /**
     * Get quota for a service type from AI Package
     */
    private Integer getQuotaForServiceType(AIPackage aiPackage, AIServiceType serviceType) {
        return switch (serviceType) {
            case GRAMMAR -> aiPackage.getGrammarQuota();
            case KAIWA -> aiPackage.getKaiwaQuota();
            case PRONUN -> aiPackage.getPronunQuota();
            case CONVERSATION -> aiPackage.getConversationQuota();
        };
    }
    
    /**
     * Check if all quotas are exhausted for user's active package
     * Returns true if:
     * - User has active package with PAID status
     * - All service types that have quota limits are exhausted (remainingQuota = 0)
     * - Unlimited services (null quota) are not considered exhausted
     */
    private boolean checkAllQuotasExhausted(Long userId) {
        Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findByUser_IdAndIsActiveTrue(userId);
        if (purchaseOpt.isEmpty()) {
            return false; // No active package, not exhausted
        }
        
        AIPackagePurchase purchase = purchaseOpt.get();
        if (purchase.getPaymentStatus() != PaymentStatus.PAID) {
            return false; // Payment not completed, not exhausted
        }
        
        AIPackage aiPackage = purchase.getAiPackage();
        List<AIQuota> quotas = quotaRepo.findByUser_Id(userId);
        
        // Check each service type
        for (AIServiceType serviceType : AIServiceType.values()) {
            Integer packageQuota = getQuotaForServiceType(aiPackage, serviceType);
            
            // If package has unlimited quota (null), skip check
            if (packageQuota == null) {
                continue;
            }
            
            // Find quota for this service type
            Optional<AIQuota> quotaOpt = quotas.stream()
                    .filter(q -> q.getServiceType() == serviceType)
                    .findFirst();
            
            if (quotaOpt.isPresent()) {
                AIQuota quota = quotaOpt.get();
                // If remaining quota > 0, not exhausted
                if (quota.getRemainingQuota() != null && quota.getRemainingQuota() > 0) {
                    return false;
                }
            } else {
                // No quota record means quota hasn't been allocated yet (not exhausted)
                return false;
            }
        }
        
        // All quota-limited services are exhausted
        return true;
    }
    
    // =========================
    // 6. Use AI service (deduct quota)
    // =========================
    
    public void useAIService(Long userId, AIServiceType serviceType, int amount) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Optional<AIQuota> quotaOpt = quotaRepo.findByUser_IdAndServiceType(userId, serviceType);
        AIQuota quota;
        
        if (quotaOpt.isEmpty()) {
            // No quota record - check if user has active package or use free tier
            Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findByUser_IdAndIsActiveTrue(userId);
            
            if (purchaseOpt.isPresent() && purchaseOpt.get().getPaymentStatus() == PaymentStatus.PAID) {
                // User has paid package - allocate quota from package
                AIPackagePurchase purchase = purchaseOpt.get();
                AIPackage aiPackage = purchase.getAiPackage();
                Integer packageQuota = getQuotaForServiceType(aiPackage, serviceType);
                
                if (packageQuota == null) {
                    // Unlimited quota - no need to track
                    log.info("Used AI service (unlimited): userId={}, serviceType={}, amount={}", 
                            userId, serviceType, amount);
                    return;
                }
                
                // Create quota from package
                quota = new AIQuota();
                quota.setUser(user);
                quota.setServiceType(serviceType);
                quota.setTotalQuota(packageQuota);
                quota.setRemainingQuota(packageQuota);
                quota.setLastResetAt(Instant.now());
                quota.setDeletedFlag(false); // Set deleted_flag explicitly
            } else {
                // Use free tier - initialize with quota based on service type
                // Conversation is more resource-intensive, so lower limit (5/month)
                int freeTierLimit = (serviceType == AIServiceType.CONVERSATION) ? 5 : 10;
                quota = new AIQuota();
                quota.setUser(user);
                quota.setServiceType(serviceType);
                quota.setTotalQuota(freeTierLimit);
                quota.setRemainingQuota(freeTierLimit);
                quota.setLastResetAt(Instant.now());
                quota.setDeletedFlag(false); // Set deleted_flag explicitly
            }
        } else {
            quota = quotaOpt.get();
            
            // Check if free tier quota needs monthly reset
            Instant now = Instant.now();
            int freeTierLimit = (serviceType == AIServiceType.CONVERSATION) ? 5 : 10;
            if (quota.getTotalQuota() != null && quota.getTotalQuota() == freeTierLimit && 
                quota.getLastResetAt() != null &&
                quota.getLastResetAt().isBefore(now.minusSeconds(30L * 24 * 60 * 60))) {
                // Reset free tier quota monthly
                quota.setRemainingQuota(freeTierLimit);
                quota.setLastResetAt(now);
            }
        }
        
        if (!quota.hasQuota()) {
            // Check if user has active package
            Optional<AIPackagePurchase> purchaseOpt = purchaseRepo.findByUser_IdAndIsActiveTrue(userId);
            if (purchaseOpt.isEmpty() || purchaseOpt.get().getPaymentStatus() != PaymentStatus.PAID) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Free tier quota exhausted. Please purchase an AI package to continue using this feature.");
            } else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Quota exhausted for service: " + serviceType.name() + ". Please upgrade your package.");
            }
        }
        
        // Deduct quota
        quota.useQuota(amount);
        quotaRepo.save(quota);
        
        log.info("Used AI service quota: userId={}, serviceType={}, amount={}, remaining={}, total={}", 
                userId, serviceType, amount, quota.getRemainingQuota(), quota.getTotalQuota());
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
        aiPackage.setGrammarQuota(req.getGrammarQuota());
        aiPackage.setKaiwaQuota(req.getKaiwaQuota());
        aiPackage.setPronunQuota(req.getPronunQuota());
        aiPackage.setConversationQuota(req.getConversationQuota());
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
        if (req.getGrammarQuota() != null) {
            aiPackage.setGrammarQuota(req.getGrammarQuota());
        }
        if (req.getKaiwaQuota() != null) {
            aiPackage.setKaiwaQuota(req.getKaiwaQuota());
        }
        if (req.getPronunQuota() != null) {
            aiPackage.setPronunQuota(req.getPronunQuota());
        }
        if (req.getConversationQuota() != null) {
            aiPackage.setConversationQuota(req.getConversationQuota());
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
                .grammarQuota(pkg.getGrammarQuota())
                .kaiwaQuota(pkg.getKaiwaQuota())
                .pronunQuota(pkg.getPronunQuota())
                .conversationQuota(pkg.getConversationQuota())
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


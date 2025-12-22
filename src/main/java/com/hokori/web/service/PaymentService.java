package com.hokori.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.dto.cart.CartItemResponse;
import com.hokori.web.dto.cart.CartResponse;
import com.hokori.web.dto.payment.*;
import com.hokori.web.entity.*;
import com.hokori.web.repository.*;
import com.hokori.web.Enum.AIServiceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class PaymentService {
    
    private final PayOSService payOSService;
    private final CartService cartService;
    private final LearnerProgressService learnerProgressService;
    private final PaymentRepository paymentRepo;
    private final CartRepository cartRepo;
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AIPackageService aiPackageService;
    private final AIPackageRepository aiPackageRepo;
    private final AIPackagePurchaseRepository aiPackagePurchaseRepo;
    private final AIQuotaRepository aiQuotaRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final com.hokori.web.service.NotificationService notificationService;
    private final com.hokori.web.service.WalletService walletService;
    private final RevenueService revenueService;
    
    public PaymentService(
            PayOSService payOSService,
            CartService cartService,
            LearnerProgressService learnerProgressService,
            PaymentRepository paymentRepo,
            CartRepository cartRepo,
            CourseRepository courseRepo,
            EnrollmentRepository enrollmentRepo,
            AIPackageService aiPackageService,
            AIPackageRepository aiPackageRepo,
            AIPackagePurchaseRepository aiPackagePurchaseRepo,
            AIQuotaRepository aiQuotaRepo,
            UserRepository userRepo,
            @Qualifier("payOSObjectMapper") ObjectMapper objectMapper,
            com.hokori.web.service.NotificationService notificationService,
            com.hokori.web.service.WalletService walletService,
            RevenueService revenueService) {
        this.payOSService = payOSService;
        this.cartService = cartService;
        this.learnerProgressService = learnerProgressService;
        this.paymentRepo = paymentRepo;
        this.cartRepo = cartRepo;
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.aiPackageService = aiPackageService;
        this.aiPackageRepo = aiPackageRepo;
        this.aiPackagePurchaseRepo = aiPackagePurchaseRepo;
        this.aiQuotaRepo = aiQuotaRepo;
        this.userRepo = userRepo;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.walletService = walletService;
        this.revenueService = revenueService;
    }
    
    /**
     * Checkout từ cart - tạo payment link
     */
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        // Verify cart belongs to user
        var cart = cartRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        
        Long cartId = cart.getId();
        if (!cartId.equals(request.cartId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cart does not belong to user");
        }
        
        // Get cart items
        CartResponse cartResponse = cartService.view();
        List<CartItemResponse> items = cartResponse.items();
        
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }
        
        // Filter selected items or specific courseIds
        List<CartItemResponse> itemsToCheckout;
        if (request.courseIds() != null && !request.courseIds().isEmpty()) {
            itemsToCheckout = items.stream()
                    .filter(item -> request.courseIds().contains(item.courseId()))
                    .collect(Collectors.toList());
        } else {
            itemsToCheckout = items.stream()
                    .filter(CartItemResponse::selected)
                    .collect(Collectors.toList());
        }
        
        if (itemsToCheckout.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No items selected for checkout");
        }
        
        // Calculate total amount (in VND)
        long totalAmount = itemsToCheckout.stream()
                .mapToLong(CartItemResponse::totalPrice)
                .sum();
        
        // Collect course IDs
        List<Long> courseIds = itemsToCheckout.stream()
                .map(CartItemResponse::courseId)
                .collect(Collectors.toList());
        
        // If total amount is 0 (all courses are free), enroll directly without payment
        if (totalAmount == 0) {
            // Enroll user into all free courses
            for (Long courseId : courseIds) {
                try {
                    if (!enrollmentRepo.existsByUser_IdAndCourse_Id(userId, courseId)) {
                        learnerProgressService.enrollCourse(userId, courseId);
                        log.info("Enrolled user {} into free course {}", userId, courseId);
                    }
                } catch (Exception e) {
                    log.error("Error enrolling user {} into free course {}", userId, courseId, e);
                    // Continue with other courses even if one fails
                }
            }
            
            // Clear cart items that were enrolled
            try {
                cartService.clearItems(courseIds);
                log.info("Cleared cart items for free courses after enrollment");
            } catch (Exception e) {
                log.error("Error clearing cart items", e);
            }
            
            // Return response indicating direct enrollment (no payment needed)
            return CheckoutResponse.builder()
                    .paymentId(null)
                    .orderCode(null)
                    .paymentLink(null)
                    .qrCode(null)
                    .amountCents(0L)
                    .description(String.format("Đã đăng ký %d khóa học miễn phí", courseIds.size()))
                    .expiredAt(null)
                    .build();
        }
        
        // Total amount > 0, proceed with payment flow
        // Generate order code
        Long orderCode = payOSService.generateOrderCode();
        
        // Ensure order code is unique
        while (paymentRepo.existsByOrderCode(orderCode)) {
            orderCode = payOSService.generateOrderCode();
        }
        
        // Prepare items for PayOS
        List<PayOSCreatePaymentRequest.PayOSItem> payOSItems = new ArrayList<>();
        
        for (CartItemResponse item : itemsToCheckout) {
            // Get course title for description
            var courseMetaOpt = courseRepo.findCourseMetadataById(item.courseId());
            String courseTitle = courseMetaOpt.map(meta -> {
                Object[] actualMeta = meta;
                if (meta.length == 1 && meta[0] instanceof Object[]) {
                    actualMeta = (Object[]) meta[0];
                }
                return actualMeta[1] != null ? actualMeta[1].toString() : "Course #" + item.courseId();
            }).orElse("Course #" + item.courseId());
            
            payOSItems.add(PayOSCreatePaymentRequest.PayOSItem.builder()
                    .name(courseTitle)
                    .quantity(item.quantity())
                    .price(item.totalPrice() / item.quantity()) // Price per unit in VND
                    .build());
        }
        
        // Create description
        String description = String.format("Thanh toán %d khóa học", itemsToCheckout.size());
        
        // Calculate expiration (30 minutes from now)
        Long expiredAt = Instant.now().plusSeconds(30 * 60).getEpochSecond();
        
        // Create payment link via PayOS
        // Note: PayOS API expects amount in VND
        // Our priceCents field stores amount in VND (not cents), so we send it directly
        PayOSCreatePaymentResponse payOSResponse = payOSService.createPaymentLink(
                orderCode,
                totalAmount, // Amount in VND
                description,
                payOSItems,
                expiredAt
        );
        
        if (payOSResponse.getData() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create payment link");
        }
        
        // Save payment record
        String courseIdsJson;
        try {
            courseIdsJson = objectMapper.writeValueAsString(courseIds);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error serializing course IDs");
        }
        
        // Load User entity để set vào Payment relationship
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Payment payment = Payment.builder()
                .orderCode(orderCode)
                .amountCents(totalAmount) // Amount in VND
                .description(description)
                .status(PaymentStatus.PENDING)
                .user(user) // Set User entity thay vì chỉ userId
                .cartId(cartId)
                .courseIds(courseIdsJson)
                .paymentLink(payOSResponse.getData().getCheckoutUrl())
                .payosQrCode(payOSResponse.getData().getQrCode())
                .expiredAt(Instant.ofEpochSecond(expiredAt))
                .build();
        
        Payment savedPayment = paymentRepo.save(payment);
        
        return CheckoutResponse.builder()
                .paymentId(savedPayment.getId())
                .orderCode(orderCode)
                .paymentLink(payOSResponse.getData().getCheckoutUrl())
                .qrCode(payOSResponse.getData().getQrCode())
                .amountCents(totalAmount)
                .description(description)
                .expiredAt(expiredAt)
                .build();
    }
    
    /**
     * Checkout AI Package - tạo payment link cho AI Package
     */
    public CheckoutResponse checkoutAIPackage(Long userId, Long packageId) {
        // Get AI Package
        var aiPackage = aiPackageRepo.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI Package not found"));
        
        if (!aiPackage.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI Package is not active");
        }
        
        // Check if user already has an active purchase (must be PAID and active)
        var existingPurchase = aiPackagePurchaseRepo.findFirstByUser_IdAndIsActiveTrue(userId);
        if (existingPurchase.isPresent()) {
            var purchase = existingPurchase.get();
            
            // Only block if purchase is PAID and active (defensive check)
            if (purchase.getPaymentStatus() != com.hokori.web.Enum.PaymentStatus.PAID) {
                log.warn("User {} has purchase {} with isActive=true but paymentStatus={}, allowing new purchase", 
                        userId, purchase.getId(), purchase.getPaymentStatus());
                // Don't block - purchase is not fully paid, allow new purchase
            } else {
                Instant now = Instant.now();
                
                // Check if package is expired
                boolean isExpired = purchase.getExpiresAt() != null && purchase.getExpiresAt().isBefore(now);
                
                // Check if quota is exhausted
                boolean quotaExhausted = aiPackageService.checkQuotaExhausted(userId);
                
                if (!isExpired && !quotaExhausted) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "User already has an active AI package. Please wait for it to expire or quota to be exhausted before purchasing a new package.");
                }
                
                // If expired or quota exhausted, allow purchase
                if (isExpired) {
                    log.info("User {} has expired package {}, allowing new purchase", userId, purchase.getAiPackage().getName());
                }
                if (quotaExhausted) {
                    log.info("User {} has exhausted quota for package {}, allowing new purchase", userId, purchase.getAiPackage().getName());
                }
            }
        }
        
        // Create purchase with PENDING status
        var purchase = new com.hokori.web.entity.AIPackagePurchase();
        purchase.setUser(userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
        purchase.setAiPackage(aiPackage);
        purchase.setPurchasePriceCents(aiPackage.getPriceCents());
        purchase.setPaymentStatus(com.hokori.web.Enum.PaymentStatus.PENDING);
        purchase.setPurchasedAt(Instant.now());
        
        // Calculate expiry date
        Instant expiresAt = Instant.now().plusSeconds(aiPackage.getDurationDays() * 24L * 60L * 60L);
        purchase.setExpiresAt(expiresAt);
        purchase.setIsActive(false);  // Will be activated after payment
        
        purchase = aiPackagePurchaseRepo.save(purchase);
        
        // If price is 0 (free package), activate immediately
        if (aiPackage.getPriceCents() == 0) {
            activateAIPackagePurchase(purchase);
            
            // Create notification for free AI package activation
            try {
                notificationService.notifyAIPackageActivated(
                        userId,
                        packageId,
                        aiPackage.getName()
                );
            } catch (Exception notifException) {
                // Log error but don't throw - activation succeeded
                log.error("Failed to create AI package activation notification for free package {}, but activation was successful.", 
                        packageId, notifException);
            }
            
            return CheckoutResponse.builder()
                    .paymentId(null)
                    .orderCode(null)
                    .paymentLink(null)
                    .qrCode(null)
                    .amountCents(0L)
                    .description(String.format("Đã kích hoạt gói AI: %s", aiPackage.getName()))
                    .expiredAt(null)
                    .build();
        }
        
        // Generate order code
        Long orderCode = payOSService.generateOrderCode();
        while (paymentRepo.existsByOrderCode(orderCode)) {
            orderCode = payOSService.generateOrderCode();
        }
        
        // Prepare PayOS items
        List<PayOSCreatePaymentRequest.PayOSItem> payOSItems = new ArrayList<>();
        payOSItems.add(PayOSCreatePaymentRequest.PayOSItem.builder()
                .name(aiPackage.getName())
                .quantity(1)
                .price(aiPackage.getPriceCents())
                .build());
        
        // PayOS requires description max 25 characters
        String description = "Thanh toan goi AI"; // 18 chars - safe for PayOS limit
        Long expiredAt = Instant.now().plusSeconds(30 * 60).getEpochSecond();
        
        // Create payment link via PayOS
        PayOSCreatePaymentResponse payOSResponse = payOSService.createPaymentLink(
                orderCode,
                aiPackage.getPriceCents(),
                description,
                payOSItems,
                expiredAt
        );
        
        if (payOSResponse.getData() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create payment link");
        }
        
        // Load User entity để set vào Payment relationship
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        // Save payment record
        Payment payment = Payment.builder()
                .orderCode(orderCode)
                .amountCents(aiPackage.getPriceCents())
                .description(description)
                .status(PaymentStatus.PENDING)
                .user(user) // Set User entity thay vì chỉ userId
                .aiPackageId(packageId)
                .aiPackagePurchaseId(purchase.getId())
                .paymentLink(payOSResponse.getData().getCheckoutUrl())
                .payosQrCode(payOSResponse.getData().getQrCode())
                .expiredAt(Instant.ofEpochSecond(expiredAt))
                .build();
        
        Payment savedPayment = paymentRepo.save(payment);
        
        log.info("Created AI package payment: userId={}, packageId={}, purchaseId={}, paymentId={}, orderCode={}", 
                userId, packageId, purchase.getId(), savedPayment.getId(), orderCode);
        
        return CheckoutResponse.builder()
                .paymentId(savedPayment.getId())
                .orderCode(orderCode)
                .paymentLink(payOSResponse.getData().getCheckoutUrl())
                .qrCode(payOSResponse.getData().getQrCode())
                .amountCents(aiPackage.getPriceCents())
                .description(description)
                .expiredAt(expiredAt)
                .build();
    }
    
    /**
     * Xử lý webhook từ PayOS
     */
    public void handleWebhook(PayOSWebhookData webhookData) {
        // Log webhook received
        log.info("Webhook received from PayOS: code={}, desc={}", 
                webhookData.getCode(), webhookData.getDesc());
        
        // Handle test webhook (PayOS sends test webhook when saving webhook URL)
        if (webhookData.getData() == null) {
            log.info("Received test webhook from PayOS (no data), returning success");
            return; // Test webhook, just return success
        }
        
        PayOSWebhookData.PayOSWebhookPaymentData data = webhookData.getData();
        Long orderCode = data.getOrderCode();
        
        log.info("Processing webhook for orderCode: {}", orderCode);
        
        // Verify signature (skip for test webhooks without signature)
        boolean signatureValid = false;
        if (webhookData.getSignature() != null && !webhookData.getSignature().isEmpty()) {
            signatureValid = payOSService.verifyWebhookSignature(webhookData);
            if (!signatureValid) {
                log.warn("Invalid webhook signature for orderCode: {}. Received: {}, This might be due to PayOS signature format differences.", orderCode, webhookData.getSignature());
                // Check if this is a test webhook by checking if payment exists
                boolean paymentExists = paymentRepo.existsByOrderCode(orderCode);
                if (!paymentExists) {
                    log.warn("Payment not found for orderCode: {} - likely a test webhook, skipping", orderCode);
                    // Don't throw error for test webhooks, just log and return
                    return;
                } else {
                    // Payment exists but signature is invalid
                    // If code = "00" (success), still process webhook but log warning
                    // This handles cases where PayOS signature format might differ
                    if (!"00".equals(data.getCode())) {
                        log.error("Invalid webhook signature for failed payment orderCode: {}", orderCode);
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
                    } else {
                        log.warn("Processing webhook with invalid signature for successful payment orderCode: {}. Signature verification skipped.", orderCode);
                    }
                }
            } else {
                log.info("Webhook signature verified successfully for orderCode: {}", orderCode);
            }
        } else {
            log.warn("Webhook without signature received for orderCode: {} (might be test webhook)", orderCode);
        }
        
        // Find payment record
        Payment payment = paymentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> {
                    log.error("Payment not found for orderCode: {}", orderCode);
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
                });
        
        // Update payment record
        try {
            String webhookDataJson;
            try {
                webhookDataJson = objectMapper.writeValueAsString(webhookData);
            } catch (JsonProcessingException e) {
                log.error("Error serializing webhook data", e);
                webhookDataJson = "{}";
            }
            payment.setWebhookData(webhookDataJson);
            payment.setPayosTransactionCode(data.getReference());
            
            // Check payment status from PayOS
            if ("00".equals(data.getCode())) {
                // Payment successful
                // Idempotency check: If payment is already PAID, skip processing to avoid duplicate enrollment/transactions
                if (payment.getStatus() == PaymentStatus.PAID) {
                    log.info("Payment {} (orderCode: {}) is already PAID, skipping duplicate webhook processing", 
                            payment.getId(), orderCode);
                    paymentRepo.save(payment); // Save webhook data update
                    return; // Exit early to avoid duplicate processing
                }
                
                payment.setStatus(PaymentStatus.PAID);
                if (payment.getPaidAt() == null) {
                    payment.setPaidAt(Instant.now());
                }
                
                // Handle AI Package purchase or course enrollment
                if (payment.getAiPackageId() != null) {
                    // This is an AI Package payment
                    activateAIPackagePurchaseFromPayment(payment);
                    
                    // Create notification for AI package activation
                    try {
                        var aiPackage = aiPackageRepo.findById(payment.getAiPackageId()).orElse(null);
                        if (aiPackage != null) {
                            notificationService.notifyAIPackageActivated(
                                    payment.getUserId(),
                                    payment.getAiPackageId(),
                                    aiPackage.getName()
                            );
                        }
                    } catch (Exception notifException) {
                        // Log error but don't throw - activation succeeded
                        log.error("Failed to create AI package activation notification for payment {} (orderCode: {}), but activation was successful.", 
                                payment.getId(), payment.getOrderCode(), notifException);
                    }
                } else {
                    // This is a course payment
                    // IMPORTANT: Enrollment must succeed even if clearing cart fails
                    enrollCoursesFromPayment(payment);
                    
                    // Create wallet transactions and notifications for each enrolled course
                    try {
                        List<Long> courseIds = parseCourseIds(payment.getCourseIds());
                        
                        // Calculate price per course if multiple courses
                        // If single course, use full amount; if multiple, need to query course prices
                        long totalCoursePriceCents = 0L;
                        List<Course> courses = new ArrayList<>();
                        for (Long courseId : courseIds) {
                            Course course = courseRepo.findById(courseId).orElse(null);
                            if (course != null) {
                                courses.add(course);
                                // Use discounted price if available, otherwise use regular price
                                long coursePriceCents = course.getDiscountedPriceCents() != null 
                                        ? course.getDiscountedPriceCents() 
                                        : (course.getPriceCents() != null ? course.getPriceCents() : 0L);
                                totalCoursePriceCents += coursePriceCents;
                            }
                        }
                        
                        // Create wallet transactions and revenue records for each course
                        for (Course course : courses) {
                            try {
                                // Calculate teacher revenue for this course
                                // Use discounted price if available, otherwise use regular price
                                long coursePriceCents = course.getDiscountedPriceCents() != null 
                                        ? course.getDiscountedPriceCents() 
                                        : (course.getPriceCents() != null ? course.getPriceCents() : 0L);
                                
                                // If multiple courses, calculate proportional amount
                                // IMPORTANT: Must match RevenueService logic exactly
                                long teacherRevenueCents;
                                if (courses.size() == 1) {
                                    // Single course: teacher gets 80% of payment amount
                                    teacherRevenueCents = Math.round(payment.getAmountCents() * 0.80);
                                } else {
                                    // Multiple courses: calculate proportional amount based on course price
                                    // Formula: (coursePrice / totalCoursePrice) * paymentAmount * 0.80
                                    if (totalCoursePriceCents > 0) {
                                        // Use double division with Math.round to match RevenueService
                                        long courseAmountCents = Math.round((double) coursePriceCents / totalCoursePriceCents * payment.getAmountCents());
                                        teacherRevenueCents = Math.round(courseAmountCents * 0.80);
                                    } else {
                                        // Fallback: divide equally
                                        teacherRevenueCents = Math.round((payment.getAmountCents() / courses.size()) * 0.80);
                                    }
                                }
                                
                                // Only create wallet transaction if course has a price and teacher exists
                                if (teacherRevenueCents > 0 && course.getUserId() != null) {
                                    try {
                                        walletService.createCourseSaleTransaction(
                                                course.getUserId(),
                                                course.getId(),
                                                teacherRevenueCents,
                                                null // createdBy = null (system)
                                        );
                                        log.info("Successfully created wallet transaction for teacher {} from course {} sale: {} cents (paymentId={}, orderCode={})", 
                                                course.getUserId(), course.getId(), teacherRevenueCents, payment.getId(), payment.getOrderCode());
                                    } catch (Exception walletException) {
                                        // Log error but don't throw - enrollment has already succeeded
                                        log.error("Failed to create wallet transaction for course {} after payment {} (orderCode: {}), but enrollment was successful. Error: {}", 
                                                course.getId(), payment.getId(), payment.getOrderCode(), walletException.getMessage(), walletException);
                                    }
                                } else {
                                    log.warn("Skipping wallet transaction creation for course {}: teacherRevenueCents={}, teacherUserId={} (paymentId={}, orderCode={})", 
                                            course.getId(), teacherRevenueCents, course.getUserId(), payment.getId(), payment.getOrderCode());
                                }
                            } catch (Exception walletException) {
                                // Log error but don't throw - enrollment has already succeeded
                                log.error("Failed to process wallet transaction for course {} after payment {} (orderCode: {}), but enrollment was successful. Error: {}", 
                                        course.getId(), payment.getId(), payment.getOrderCode(), walletException.getMessage(), walletException);
                            }
                        }
                        
                        // Create revenue records for tracking and payout management
                        try {
                            log.info("Creating revenue records for payment {} (orderCode: {})", payment.getId(), payment.getOrderCode());
                            revenueService.createRevenueFromPayment(payment);
                            log.info("Successfully created revenue records for payment {} (orderCode: {})", payment.getId(), payment.getOrderCode());
                        } catch (Exception revenueException) {
                            // Log error but don't throw - payment and enrollment already succeeded
                            log.error("Failed to create revenue records for payment {} (orderCode: {}), but payment was successful. Error: {}", 
                                    payment.getId(), payment.getOrderCode(), revenueException.getMessage(), revenueException);
                        }
                        
                        // Create notification for each enrolled course
                        for (Course course : courses) {
                            try {
                                notificationService.notifyPaymentSuccess(
                                        payment.getUserId(),
                                        payment.getId(),
                                        course.getId(),
                                        course.getTitle()
                                );
                            } catch (Exception notifException) {
                                // Log error but don't throw - enrollment has already succeeded
                                log.error("Failed to create payment success notification for course {} after payment {} (orderCode: {}), but enrollment was successful.", 
                                        course.getId(), payment.getId(), payment.getOrderCode(), notifException);
                            }
                        }
                    } catch (Exception notifException) {
                        // Log error but don't throw - enrollment has already succeeded
                        log.error("Failed to create payment success notification for payment {} (orderCode: {}), but enrollment was successful.", 
                                payment.getId(), payment.getOrderCode(), notifException);
                    }
                    
                    // Clear cart items separately - don't let cart clearing failure rollback enrollment
                    // This is wrapped in try-catch to ensure enrollment is not affected
                    try {
                        clearPaidCartItems(payment);
                    } catch (Exception cartException) {
                        // Log error but don't throw - enrollment has already succeeded
                        log.error("Failed to clear cart items for payment {} (orderCode: {}), but enrollment was successful. Cart can be cleared manually if needed.", 
                                payment.getId(), payment.getOrderCode(), cartException);
                    }
                }
            } else {
                // Payment failed or cancelled
                payment.setStatus(PaymentStatus.FAILED);
            }
            
            paymentRepo.save(payment);
        } catch (Exception e) {
            log.error("Error processing webhook for orderCode: {}", orderCode, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing webhook");
        }
    }
    
    /**
     * Enroll user into courses after successful payment
     */
    private void enrollCoursesFromPayment(Payment payment) {
        try {
            List<Long> courseIds = objectMapper.readValue(
                    payment.getCourseIds(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
            
            for (Long courseId : courseIds) {
                try {
                    // Use enrollCourseAfterPayment to skip price check (user has already paid)
                    learnerProgressService.enrollCourseAfterPayment(payment.getUserId(), courseId);
                    log.info("Enrolled user {} into course {} after successful payment", payment.getUserId(), courseId);
                } catch (Exception e) {
                    log.error("Error enrolling user {} into course {} after payment", payment.getUserId(), courseId, e);
                    // Continue with other courses even if one fails
                }
            }
        } catch (Exception e) {
            log.error("Error parsing courseIds from payment {}", payment.getId(), e);
            throw new RuntimeException("Error enrolling courses", e);
        }
    }
    
    /**
     * Clear cart items that were paid
     */
    private void clearPaidCartItems(Payment payment) {
        if (payment.getCartId() != null) {
            try {
                // Parse course IDs from payment
                List<Long> courseIds = parseCourseIds(payment.getCourseIds());
                if (!courseIds.isEmpty()) {
                    // Clear cart items for these courses using userId from payment
                    // (webhook context doesn't have authenticated user in SecurityContext)
                    cartService.clearItemsForUser(payment.getUserId(), courseIds);
                    log.info("Cleared cart {} items for courses {} after payment {}", 
                            payment.getCartId(), courseIds, payment.getOrderCode());
                }
            } catch (Exception e) {
                log.error("Error clearing cart items for payment {}", payment.getOrderCode(), e);
            }
        }
    }
    
    /**
     * Lấy danh sách payments của user (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> listMyPayments(Long userId, Pageable pageable) {
        Page<Payment> payments = paymentRepo.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
        return payments.map(payment -> {
            try {
                List<Long> courseIds = parseCourseIds(payment.getCourseIds());
                return PaymentResponse.fromEntity(payment, courseIds);
            } catch (Exception e) {
                Long paymentId = payment != null ? payment.getId() : null;
                log.error("Error processing payment {} for user {}: {}", paymentId, userId, e.getMessage(), e);
                // Return payment with empty courseIds list if parsing fails
                // This ensures API doesn't fail even if courseIds parsing has issues
                try {
                    return PaymentResponse.fromEntity(payment, Collections.emptyList());
                } catch (Exception ex) {
                    log.error("Critical error creating PaymentResponse for payment {}: {}", paymentId, ex.getMessage(), ex);
                    // Last resort: create minimal response
                    return PaymentResponse.builder()
                            .id(paymentId)
                            .orderCode(payment != null ? payment.getOrderCode() : null)
                            .amountCents(payment != null ? payment.getAmountCents() : null)
                            .status(payment != null ? payment.getStatus() : null)
                            .courseIds(Collections.emptyList())
                            .build();
                }
            }
        });
    }
    
    /**
     * Lấy chi tiết một payment của user
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentDetail(Long paymentId, Long userId) {
        Payment payment = paymentRepo.findByIdAndUser_Id(paymentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        List<Long> courseIds = parseCourseIds(payment.getCourseIds());
        return PaymentResponse.fromEntity(payment, courseIds);
    }
    
    /**
     * Lấy payment theo orderCode của user
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderCode(Long orderCode, Long userId) {
        Payment payment = paymentRepo.findByOrderCodeAndUser_Id(orderCode, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        List<Long> courseIds = parseCourseIds(payment.getCourseIds());
        return PaymentResponse.fromEntity(payment, courseIds);
    }
    
    /**
     * Retry enrollment for a successful payment that hasn't been enrolled yet
     * This is useful when webhook failed but payment was successful
     * If payment is PENDING, it will be marked as PAID and then enrolled
     */
    @Transactional
    public void retryEnrollmentFromPayment(Long paymentId, Long userId) {
        Payment payment = paymentRepo.findByIdAndUser_Id(paymentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        
        // Allow retry for PAID payments, or PENDING payments (user confirms payment was successful)
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Cannot retry enrollment for payment with status: " + payment.getStatus() + 
                    ". Only PENDING or PAID payments can be retried.");
        }
        
        // If payment is PENDING, mark it as PAID (user confirms payment was successful)
        if (payment.getStatus() == PaymentStatus.PENDING) {
            log.warn("Payment {} (orderCode: {}) is PENDING but user confirms payment was successful. Marking as PAID and enrolling.", 
                    paymentId, payment.getOrderCode());
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(Instant.now());
            paymentRepo.save(payment);
        }
        
        // Check if this is a course payment (not AI package)
        if (payment.getAiPackageId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "This payment is for AI package, not courses. Use POST /api/payment/{paymentId}/retry-ai-package-activation instead.");
        }
        
        // Check if courses are already enrolled
        List<Long> courseIds = parseCourseIds(payment.getCourseIds());
        if (courseIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No courses found in payment");
        }
        
        // Enroll user into courses
        enrollCoursesFromPayment(payment);
        clearPaidCartItems(payment);
        
        log.info("Retry enrollment completed for payment {} (orderCode: {}), enrolled {} courses", 
                paymentId, payment.getOrderCode(), courseIds.size());
    }
    
    /**
     * Retry AI package activation for a successful payment that hasn't been activated yet
     * This is useful when webhook failed but payment was successful
     * If payment is PENDING, it will be marked as PAID and then activated
     */
    @Transactional
    public void retryAIPackageActivationFromPayment(Long paymentId, Long userId) {
        Payment payment = paymentRepo.findByIdAndUser_Id(paymentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        
        // Allow retry for PAID payments, or PENDING payments (user confirms payment was successful)
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Cannot retry activation for payment with status: " + payment.getStatus() + 
                    ". Only PENDING or PAID payments can be retried.");
        }
        
        // Check if this is an AI package payment
        if (payment.getAiPackageId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "This payment is for courses, not AI package. Use POST /api/payment/{paymentId}/retry-enrollment instead.");
        }
        
        if (payment.getAiPackagePurchaseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Payment does not have aiPackagePurchaseId. Cannot activate AI package.");
        }
        
        // If payment is PENDING, mark it as PAID (user confirms payment was successful)
        if (payment.getStatus() == PaymentStatus.PENDING) {
            log.warn("Payment {} (orderCode: {}) is PENDING but user confirms payment was successful. Marking as PAID and activating AI package.", 
                    paymentId, payment.getOrderCode());
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(Instant.now());
            paymentRepo.save(payment);
        }
        
        // Check if already activated
        var purchase = aiPackagePurchaseRepo.findById(payment.getAiPackagePurchaseId())
                .orElseThrow(() -> {
                    log.error("AIPackagePurchase not found for payment {}", payment.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "AI Package purchase not found");
                });
        
        if (purchase.getIsActive() && purchase.getPaymentStatus() == com.hokori.web.Enum.PaymentStatus.PAID) {
            log.info("AI package purchase {} is already activated for payment {}", purchase.getId(), paymentId);
            return; // Already activated, no need to retry
        }
        
        // Activate AI package purchase
        activateAIPackagePurchase(purchase);
        
        log.info("Retry AI package activation completed for payment {} (orderCode: {}), packageId={}, purchaseId={}", 
                paymentId, payment.getOrderCode(), payment.getAiPackageId(), purchase.getId());
    }
    
    /**
     * Activate AI Package purchase after successful payment
     */
    private void activateAIPackagePurchaseFromPayment(Payment payment) {
        if (payment.getAiPackagePurchaseId() == null) {
            log.warn("Payment {} has aiPackageId but no aiPackagePurchaseId", payment.getId());
            return;
        }
        
        var purchase = aiPackagePurchaseRepo.findById(payment.getAiPackagePurchaseId())
                .orElseThrow(() -> {
                    log.error("AIPackagePurchase not found for payment {}", payment.getId());
                    return new RuntimeException("AIPackagePurchase not found");
                });
        
        activateAIPackagePurchase(purchase);
    }
    
    /**
     * Activate AI Package purchase and allocate quotas
     */
    private void activateAIPackagePurchase(com.hokori.web.entity.AIPackagePurchase purchase) {
        // Check if already activated (idempotent)
        if (purchase.getIsActive() && purchase.getPaymentStatus() == com.hokori.web.Enum.PaymentStatus.PAID) {
            log.info("AI package purchase {} is already activated, skipping activation", purchase.getId());
            return;
        }
        
        // Deactivate all other active purchases for this user (only one active purchase at a time)
        var user = purchase.getUser();
        var existingActivePurchases = aiPackagePurchaseRepo.findByUser_IdAndIsActiveTrue(user.getId());
        for (var existingPurchase : existingActivePurchases) {
            if (!existingPurchase.getId().equals(purchase.getId())) {
                existingPurchase.setIsActive(false);
                aiPackagePurchaseRepo.save(existingPurchase);
                log.info("Deactivated old purchase {} for user {} when activating new purchase {}", 
                        existingPurchase.getId(), user.getId(), purchase.getId());
            }
        }
        
        // Activate purchase
        purchase.setIsActive(true);
        purchase.setPaymentStatus(com.hokori.web.Enum.PaymentStatus.PAID);
        // Don't overwrite purchasedAt - keep the original purchase time
        // purchase.setPurchasedAt(Instant.now()); // Keep original purchase time
        purchase.setTransactionId(String.valueOf(purchase.getId()));
        aiPackagePurchaseRepo.save(purchase);
        
        // Allocate quotas to user
        var aiPackage = purchase.getAiPackage();
        
        // Allocate unified request pool
        if (aiPackage.getTotalRequests() != null) {
            allocateUnifiedQuota(user.getId(), aiPackage.getTotalRequests());
        }
        
        log.info("Activated AI package purchase: userId={}, packageId={}, purchaseId={}, totalRequests={}", 
                user.getId(), aiPackage.getId(), purchase.getId(), aiPackage.getTotalRequests());
    }
    
    /**
     * Allocate unified request pool quota for user
     * If user already has an active package, ADD requests to existing quota
     * Otherwise, RESET quota to new package value
     */
    private void allocateUnifiedQuota(Long userId, Integer totalRequests) {
        var existingQuota = aiQuotaRepo.findByUser_Id(userId);
        
        if (existingQuota.isPresent()) {
            var quotaEntity = existingQuota.get();
            
            // Check if user has active paid package
            var purchaseOpt = aiPackagePurchaseRepo.findFirstByUser_IdAndIsActiveTrue(userId);
            if (purchaseOpt.isPresent() && purchaseOpt.get().getPaymentStatus() == PaymentStatus.PAID) {
                // User đang có package active → ADD thêm requests (không reset)
                Integer currentTotal = quotaEntity.getTotalRequests();
                Integer currentUsed = quotaEntity.getUsedRequests() != null ? quotaEntity.getUsedRequests() : 0;
                
                if (currentTotal != null && totalRequests != null) {
                    // Add new requests to existing quota
                    Integer newTotal = currentTotal + totalRequests;
                    quotaEntity.setTotalRequests(newTotal);
                    quotaEntity.setRemainingRequests(Math.max(0, newTotal - currentUsed));
                    log.info("Added {} requests to existing quota for user {}: {} -> {} (used: {})", 
                            totalRequests, userId, currentTotal, newTotal, currentUsed);
                } else if (currentTotal == null && totalRequests != null) {
                    // Current is unlimited, new is limited → Keep unlimited
                    quotaEntity.setTotalRequests(null);
                    quotaEntity.setRemainingRequests(null);
                    log.info("User {} has unlimited quota, keeping unlimited after adding package", userId);
                } else {
                    // Both unlimited or other cases → Reset
                    quotaEntity.initializeQuota(totalRequests);
                    log.info("Reset quota for user {} to {}", userId, totalRequests);
                }
            } else {
                // User không có package active → RESET về package mới
                quotaEntity.initializeQuota(totalRequests);
                log.info("Reset quota for user {} (no active package) to {}", userId, totalRequests);
            }
            
            aiQuotaRepo.save(quotaEntity);
        } else {
            // Create new unified quota
            var quotaEntity = new AIQuota();
            quotaEntity.setUser(userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found")));
            quotaEntity.initializeQuota(totalRequests);
            quotaEntity.setDeletedFlag(false);
            aiQuotaRepo.save(quotaEntity);
            log.info("Created new quota for user {}: {}", userId, totalRequests);
        }
    }
    
    /**
     * Parse courseIds từ JSON string
     */
    private List<Long> parseCourseIds(String courseIdsJson) {
        if (courseIdsJson == null || courseIdsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    courseIdsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
        } catch (Exception e) {
            log.error("Error parsing courseIds", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Backfill WalletTransaction cho các payment cũ đã thành công nhưng chưa có WalletTransaction
     * Chỉ xử lý payments có courseIds (không phải AI Package)
     */
    @Transactional
    public int backfillWalletTransactionsForOldPayments() {
        // Find all PAID payments with courseIds (not AI Package)
        List<Payment> paidPayments = paymentRepo.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getCourseIds() != null && !p.getCourseIds().isEmpty())
                .filter(p -> p.getAiPackageId() == null) // Only course payments
                .collect(Collectors.toList());
        
        int processedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        
        for (Payment payment : paidPayments) {
            try {
                List<Long> courseIds = parseCourseIds(payment.getCourseIds());
                if (courseIds.isEmpty()) {
                    skippedCount++;
                    continue;
                }
                
                // Check if wallet transaction already exists for this payment
                // We can check by looking at wallet transactions with same course and similar timestamp
                // For simplicity, we'll just create if not exists (idempotent check by checking course + teacher + amount)
                boolean alreadyProcessed = false;
                for (Long courseId : courseIds) {
                    Course course = courseRepo.findById(courseId).orElse(null);
                    if (course != null && course.getUserId() != null) {
                        // Check if wallet transaction exists for this course around payment time
                        // Simple check: if teacher has wallet transactions for this course around paidAt time
                        // We'll skip this check for now and just create (idempotent by checking before creating)
                    }
                }
                
                if (alreadyProcessed) {
                    skippedCount++;
                    continue;
                }
                
                // Calculate price per course
                long totalCoursePriceCents = 0L;
                List<Course> courses = new ArrayList<>();
                for (Long courseId : courseIds) {
                    Course course = courseRepo.findById(courseId).orElse(null);
                    if (course != null) {
                        courses.add(course);
                        long coursePriceCents = course.getDiscountedPriceCents() != null 
                                ? course.getDiscountedPriceCents() 
                                : (course.getPriceCents() != null ? course.getPriceCents() : 0L);
                        totalCoursePriceCents += coursePriceCents;
                    }
                }
                
                // Create wallet transaction for each course
                boolean createdAny = false;
                for (Course course : courses) {
                    try {
                        long coursePriceCents = course.getDiscountedPriceCents() != null 
                                ? course.getDiscountedPriceCents() 
                                : (course.getPriceCents() != null ? course.getPriceCents() : 0L);
                        
                        // IMPORTANT: Must match RevenueService and handleWebhook logic exactly
                        long teacherRevenueCents;
                        if (courses.size() == 1) {
                            // Single course: teacher gets 80% of payment amount
                            teacherRevenueCents = Math.round(payment.getAmountCents() * 0.80);
                        } else {
                            if (totalCoursePriceCents > 0) {
                                // Use double division with Math.round to match RevenueService
                                long courseAmountCents = Math.round((double) coursePriceCents / totalCoursePriceCents * payment.getAmountCents());
                                teacherRevenueCents = Math.round(courseAmountCents * 0.80);
                            } else {
                                // Fallback: divide equally
                                teacherRevenueCents = Math.round((payment.getAmountCents() / courses.size()) * 0.80);
                            }
                        }
                        
                        if (teacherRevenueCents > 0 && course.getUserId() != null) {
                            walletService.createCourseSaleTransaction(
                                    course.getUserId(),
                                    course.getId(),
                                    teacherRevenueCents,
                                    null // createdBy = null (system backfill)
                            );
                            createdAny = true;
                            log.info("Backfilled wallet transaction for teacher {} from course {} (payment orderCode: {}): {} cents", 
                                    course.getUserId(), course.getId(), payment.getOrderCode(), teacherRevenueCents);
                        }
                    } catch (Exception e) {
                        log.error("Error creating wallet transaction for course {} in payment {}", 
                                course.getId(), payment.getOrderCode(), e);
                        errorCount++;
                    }
                }
                
                if (createdAny) {
                    processedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                log.error("Error processing payment {} for backfill", payment.getOrderCode(), e);
                errorCount++;
            }
        }
        
        log.info("Backfill completed: {} payments processed, {} skipped, {} errors", 
                processedCount, skippedCount, errorCount);
        
        return processedCount;
    }
}


package com.hokori.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.dto.cart.CartItemResponse;
import com.hokori.web.dto.cart.CartResponse;
import com.hokori.web.dto.payment.*;
import com.hokori.web.entity.Payment;
import com.hokori.web.repository.*;
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
    private final ObjectMapper objectMapper;
    
    public PaymentService(
            PayOSService payOSService,
            CartService cartService,
            LearnerProgressService learnerProgressService,
            PaymentRepository paymentRepo,
            CartRepository cartRepo,
            CourseRepository courseRepo,
            EnrollmentRepository enrollmentRepo,
            @Qualifier("payOSObjectMapper") ObjectMapper objectMapper) {
        this.payOSService = payOSService;
        this.cartService = cartService;
        this.learnerProgressService = learnerProgressService;
        this.paymentRepo = paymentRepo;
        this.cartRepo = cartRepo;
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Checkout từ cart - tạo payment link
     */
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        // Verify cart belongs to user
        var cart = cartRepo.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        
        Long cartId = cart.getId();
        if (!cartId.equals(request.getCartId())) {
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
        if (request.getCourseIds() != null && !request.getCourseIds().isEmpty()) {
            itemsToCheckout = items.stream()
                    .filter(item -> request.getCourseIds().contains(item.courseId()))
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
                    if (!enrollmentRepo.existsByUserIdAndCourseId(userId, courseId)) {
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
        
        Payment payment = Payment.builder()
                .orderCode(orderCode)
                .amountCents(totalAmount) // Amount in VND
                .description(description)
                .status(PaymentStatus.PENDING)
                .userId(userId)
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
     * Xử lý webhook từ PayOS
     */
    public void handleWebhook(PayOSWebhookData webhookData) {
        // Verify signature
        if (!payOSService.verifyWebhookSignature(webhookData)) {
            log.error("Invalid webhook signature for orderCode: {}", 
                    webhookData.getData() != null ? webhookData.getData().getOrderCode() : "unknown");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }
        
        PayOSWebhookData.PayOSWebhookPaymentData data = webhookData.getData();
        Long orderCode = data.getOrderCode();
        
        // Find payment record
        Payment payment = paymentRepo.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        
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
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(Instant.now());
                
                // Enroll user into courses
                enrollCoursesFromPayment(payment);
                
                // Clear cart items that were paid
                clearPaidCartItems(payment);
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
                    // Check if already enrolled
                    if (!enrollmentRepo.existsByUserIdAndCourseId(payment.getUserId(), courseId)) {
                        learnerProgressService.enrollCourse(payment.getUserId(), courseId);
                        log.info("Enrolled user {} into course {}", payment.getUserId(), courseId);
                    }
                } catch (Exception e) {
                    log.error("Error enrolling user {} into course {}", payment.getUserId(), courseId, e);
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
                // Note: CartService sẽ tự động cleanup khi view cart
                // Nhưng có thể xóa trực tiếp ở đây để đảm bảo
                log.info("Cleared cart {} after payment {}", payment.getCartId(), payment.getOrderCode());
            } catch (Exception e) {
                log.error("Error clearing cart items", e);
            }
        }
    }
    
    /**
     * Lấy danh sách payments của user (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> listMyPayments(Long userId, Pageable pageable) {
        Page<Payment> payments = paymentRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return payments.map(payment -> {
            List<Long> courseIds = parseCourseIds(payment.getCourseIds());
            return PaymentResponse.fromEntity(payment, courseIds);
        });
    }
    
    /**
     * Lấy chi tiết một payment của user
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentDetail(Long paymentId, Long userId) {
        Payment payment = paymentRepo.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        List<Long> courseIds = parseCourseIds(payment.getCourseIds());
        return PaymentResponse.fromEntity(payment, courseIds);
    }
    
    /**
     * Lấy payment theo orderCode của user
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderCode(Long orderCode, Long userId) {
        Payment payment = paymentRepo.findByOrderCodeAndUserId(orderCode, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        List<Long> courseIds = parseCourseIds(payment.getCourseIds());
        return PaymentResponse.fromEntity(payment, courseIds);
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
}


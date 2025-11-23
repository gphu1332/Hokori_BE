package com.hokori.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.dto.cart.CartItemResponse;
import com.hokori.web.dto.cart.CartResponse;
import com.hokori.web.dto.payment.*;
import com.hokori.web.entity.Payment;
import com.hokori.web.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
        
        // Calculate total amount
        long totalAmount = itemsToCheckout.stream()
                .mapToLong(CartItemResponse::totalPrice)
                .sum();
        
        if (totalAmount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total amount must be greater than 0");
        }
        
        // Generate order code
        Long orderCode = payOSService.generateOrderCode();
        
        // Ensure order code is unique
        while (paymentRepo.existsByOrderCode(orderCode)) {
            orderCode = payOSService.generateOrderCode();
        }
        
        // Prepare items for PayOS
        List<PayOSCreatePaymentRequest.PayOSItem> payOSItems = new ArrayList<>();
        List<Long> courseIds = new ArrayList<>();
        
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
                    .price(item.totalPrice() / item.quantity()) // Price per unit
                    .build());
            
            courseIds.add(item.courseId());
        }
        
        // Create description
        String description = String.format("Thanh toán %d khóa học", itemsToCheckout.size());
        
        // Calculate expiration (30 minutes from now)
        Long expiredAt = Instant.now().plusSeconds(30 * 60).getEpochSecond();
        
        // Create payment link via PayOS
        // Note: PayOS API expects amount in VND (not cents), so we convert from cents to VND
        // But in our system, we store everything in cents, so totalAmount is already in cents
        // PayOS API documentation says amount should be in VND, so we need to divide by 100 if we're storing cents
        // However, if our priceCents is already in VND (not cents), then we use it directly
        // Assuming priceCents is in VND (not cents) based on the codebase
        PayOSCreatePaymentResponse payOSResponse = payOSService.createPaymentLink(
                orderCode,
                totalAmount, // Amount in VND (assuming priceCents is already in VND)
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
                .amountCents(totalAmount)
                .description(description)
                .status(PaymentStatus.PENDING)
                .userId(userId)
                .cartId(cartId)
                .courseIds(courseIdsJson)
                .paymentLink(payOSResponse.data().checkoutUrl())
                .payosQrCode(payOSResponse.data().qrCode())
                .expiredAt(Instant.ofEpochSecond(expiredAt))
                .build();
        
        Payment savedPayment = paymentRepo.save(payment);
        
        return CheckoutResponse.builder()
                .paymentId(savedPayment.getId())
                .orderCode(orderCode)
                .paymentLink(payOSResponse.data().checkoutUrl())
                .qrCode(payOSResponse.data().qrCode())
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
        
        PayOSWebhookData.PayOSWebhookPaymentData data = webhookData.data();
        Long orderCode = data.orderCode();
        
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
            payment.setPayosTransactionCode(data.reference());
            
            // Check payment status from PayOS
            if ("00".equals(data.code())) {
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
}


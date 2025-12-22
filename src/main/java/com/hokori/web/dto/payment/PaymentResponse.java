package com.hokori.web.dto.payment;

import com.hokori.web.Enum.PaymentStatus;
import com.hokori.web.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    Long id;
    Long orderCode;
    Long amountCents;
    String description;
    PaymentStatus status;
    Long userId;
    Long cartId;
    List<Long> courseIds; // Course payments
    Long aiPackageId; // AI Package payments
    Long aiPackagePurchaseId; // AI Package Purchase ID
    String paymentLink;
    String payosTransactionCode;
    String payosQrCode;
    Instant expiredAt;
    Instant paidAt;
    Instant createdAt;
    Instant updatedAt;
    
    public static PaymentResponse fromEntity(Payment payment, List<Long> courseIds) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderCode(payment.getOrderCode())
                .amountCents(payment.getAmountCents())
                .description(payment.getDescription())
                .status(payment.getStatus())
                .userId(payment.getUserId())
                .cartId(payment.getCartId())
                .courseIds(courseIds)
                .aiPackageId(payment.getAiPackageId())
                .aiPackagePurchaseId(payment.getAiPackagePurchaseId())
                .paymentLink(payment.getPaymentLink())
                .payosTransactionCode(payment.getPayosTransactionCode())
                .payosQrCode(payment.getPayosQrCode())
                .expiredAt(payment.getExpiredAt())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}


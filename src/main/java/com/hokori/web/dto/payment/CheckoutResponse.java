package com.hokori.web.dto.payment;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckoutResponse {
    Long paymentId;
    Long orderCode;
    String paymentLink;
    String qrCode;
    Long amountCents;
    String description;
    Long expiredAt; // Unix timestamp (seconds)
}


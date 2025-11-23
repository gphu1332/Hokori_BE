package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.payment.CheckoutRequest;
import com.hokori.web.dto.payment.CheckoutResponse;
import com.hokori.web.dto.payment.PayOSWebhookData;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment APIs using PayOS")
public class PaymentController {
    
    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;
    
    @Operation(
            summary = "Checkout từ cart",
            description = "Tạo payment link từ cart để thanh toán các khóa học đã chọn"
    )
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        Long userId = currentUserService.getUserIdOrThrow();
        CheckoutResponse response = paymentService.checkout(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Payment link created", response));
    }
    
    @Operation(
            summary = "Webhook từ PayOS",
            description = "Endpoint để nhận webhook callback từ PayOS khi có thay đổi trạng thái thanh toán"
    )
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody PayOSWebhookData webhookData) {
        try {
            paymentService.handleWebhook(webhookData);
            // PayOS expects response in specific format
            return ResponseEntity.ok(new WebhookResponse(0, "Success", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new WebhookResponse(-1, "Error: " + e.getMessage(), null));
        }
    }
    
    // Response format expected by PayOS
    private record WebhookResponse(int code, String desc, Object data) {}
}


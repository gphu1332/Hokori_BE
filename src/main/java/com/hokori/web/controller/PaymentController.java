package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.payment.CheckoutRequest;
import com.hokori.web.dto.payment.CheckoutResponse;
import com.hokori.web.dto.payment.PaymentResponse;
import com.hokori.web.dto.payment.PayOSWebhookData;
import com.hokori.web.service.CurrentUserService;
import com.hokori.web.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    @Operation(
            summary = "Lịch sử thanh toán của tôi",
            description = "Lấy danh sách các payment của user hiện tại, sắp xếp theo thời gian tạo mới nhất"
    )
    @GetMapping("/my-payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> listMyPayments(
            @Parameter(description = "Phân trang: page, size, sort (ví dụ: sort=createdAt,desc)")
            Pageable pageable) {
        Long userId = currentUserService.getUserIdOrThrow();
        Page<PaymentResponse> payments = paymentService.listMyPayments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved", payments));
    }
    
    @Operation(
            summary = "Chi tiết payment",
            description = "Lấy chi tiết một payment cụ thể của user hiện tại"
    )
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentDetail(
            @Parameter(description = "Payment ID")
            @PathVariable Long paymentId) {
        Long userId = currentUserService.getUserIdOrThrow();
        PaymentResponse payment = paymentService.getPaymentDetail(paymentId, userId);
        return ResponseEntity.ok(ApiResponse.success("Payment detail retrieved", payment));
    }
    
    @Operation(
            summary = "Chi tiết payment theo orderCode",
            description = "Lấy chi tiết payment theo orderCode (PayOS order code) của user hiện tại"
    )
    @GetMapping("/order/{orderCode}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderCode(
            @Parameter(description = "PayOS Order Code")
            @PathVariable Long orderCode) {
        Long userId = currentUserService.getUserIdOrThrow();
        PaymentResponse payment = paymentService.getPaymentByOrderCode(orderCode, userId);
        return ResponseEntity.ok(ApiResponse.success("Payment detail retrieved", payment));
    }
    
    // Response format expected by PayOS
    private record WebhookResponse(int code, String desc, Object data) {}
}


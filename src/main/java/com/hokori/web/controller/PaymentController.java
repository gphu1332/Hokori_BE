package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.payment.AIPackageCheckoutRequest;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
            summary = "Checkout AI Package",
            description = "Tạo payment link để thanh toán gói AI (Plus, Pro, etc.)"
    )
    @PostMapping("/ai-package/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkoutAIPackage(
            @Valid @RequestBody AIPackageCheckoutRequest request) {
        Long userId = currentUserService.getUserIdOrThrow();
        CheckoutResponse response = paymentService.checkoutAIPackage(userId, request.packageId());
        return ResponseEntity.ok(ApiResponse.success("Payment link created", response));
    }
    
    @Operation(
            summary = "Webhook từ PayOS",
            description = "Endpoint để nhận webhook callback từ PayOS khi có thay đổi trạng thái thanh toán"
    )
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody(required = false) PayOSWebhookData webhookData) {
        try {
            // Handle empty body (PayOS test webhook)
            if (webhookData == null) {
                return ResponseEntity.ok(new WebhookResponse(0, "Webhook endpoint is active", null));
            }
            
            paymentService.handleWebhook(webhookData);
            // PayOS expects response in specific format
            return ResponseEntity.ok(new WebhookResponse(0, "Success", null));
        } catch (ResponseStatusException e) {
            // For test webhooks, always return success to allow PayOS to save webhook URL
            // PayOS will retry webhook when actual payment happens
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return ResponseEntity.ok(new WebhookResponse(0, "Webhook endpoint is active (test mode)", null));
            }
            return ResponseEntity.status(e.getStatusCode())
                    .body(new WebhookResponse(-1, "Error: " + e.getReason(), null));
        } catch (Exception e) {
            // For any error during test, return success to allow PayOS to save webhook URL
            // PayOS will retry webhook when actual payment happens
            return ResponseEntity.ok(new WebhookResponse(0, "Webhook endpoint is active (test mode)", null));
        }
    }
    
    /**
     * GET endpoint for PayOS webhook test (some systems use GET to test webhook)
     */
    @GetMapping("/webhook")
    public ResponseEntity<?> webhookTest() {
        return ResponseEntity.ok(new WebhookResponse(0, "Webhook endpoint is active", null));
    }
    
    @Operation(
            summary = "Lịch sử thanh toán của tôi",
            description = "Lấy danh sách các payment của user hiện tại, sắp xếp theo thời gian tạo mới nhất. " +
                    "Valid sort fields: id, orderCode, amountCents, status, createdAt, updatedAt, paidAt, expiredAt. " +
                    "Example: sort=createdAt,desc"
    )
    @GetMapping("/my-payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> listMyPayments(
            @Parameter(description = "Phân trang: page, size, sort (ví dụ: sort=createdAt,desc). " +
                    "Valid sort fields: id, orderCode, amountCents, status, createdAt, updatedAt, paidAt, expiredAt")
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        Long userId = currentUserService.getUserIdOrThrow();
        
        // Valid sort fields for Payment entity
        String[] validSortFields = {"id", "orderCode", "amountCents", "status", "createdAt", "updatedAt", "paidAt", "expiredAt"};
        
        // Validate and build Pageable
        Pageable pageable;
        if (sort != null && !sort.isEmpty()) {
            // Parse sort parameter (format: "property,direction" or just "property")
            String[] sortParts = sort.split(",");
            String sortProperty = sortParts[0].trim();
            Sort.Direction direction = Sort.Direction.DESC; // Default to DESC
            
            if (sortParts.length > 1) {
                String dirStr = sortParts[1].trim().toUpperCase();
                direction = "ASC".equals(dirStr) ? Sort.Direction.ASC : Sort.Direction.DESC;
            }
            
            // Validate sort property
            boolean isValid = false;
            for (String validField : validSortFields) {
                if (validField.equalsIgnoreCase(sortProperty)) {
                    isValid = true;
                    sortProperty = validField; // Use exact field name
                    break;
                }
            }
            
            if (isValid) {
                pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));
            } else {
                // Invalid sort property, use default sort (createdAt DESC)
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            }
        } else {
            // No sort specified, use default sort (createdAt DESC)
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        
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
    
    @Operation(
            summary = "Retry enrollment từ payment",
            description = "Retry enrollment cho payment đã thành công nhưng chưa được enroll (dùng khi webhook failed). Chỉ dùng cho course payments."
    )
    @PostMapping("/{paymentId}/retry-enrollment")
    public ResponseEntity<ApiResponse<String>> retryEnrollment(
            @Parameter(description = "Payment ID")
            @PathVariable Long paymentId) {
        Long userId = currentUserService.getUserIdOrThrow();
        paymentService.retryEnrollmentFromPayment(paymentId, userId);
        return ResponseEntity.ok(ApiResponse.success("Enrollment retry completed successfully", null));
    }
    
    @Operation(
            summary = "Retry AI package activation từ payment",
            description = "Retry activation cho AI package payment đã thành công nhưng chưa được activate (dùng khi webhook failed). Chỉ dùng cho AI package payments."
    )
    @PostMapping("/{paymentId}/retry-ai-package-activation")
    public ResponseEntity<ApiResponse<String>> retryAIPackageActivation(
            @Parameter(description = "Payment ID")
            @PathVariable Long paymentId) {
        Long userId = currentUserService.getUserIdOrThrow();
        paymentService.retryAIPackageActivationFromPayment(paymentId, userId);
        return ResponseEntity.ok(ApiResponse.success("AI package activation retry completed successfully", null));
    }
    
    // Response format expected by PayOS
    private record WebhookResponse(int code, String desc, Object data) {}
}


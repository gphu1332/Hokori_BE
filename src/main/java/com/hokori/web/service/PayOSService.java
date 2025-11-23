package com.hokori.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hokori.web.config.PayOSConfig;
import com.hokori.web.dto.payment.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSService {
    
    private final PayOSConfig payOSConfig;
    
    @Qualifier("payOSRestTemplate")
    private final RestTemplate restTemplate;
    
    @Qualifier("payOSObjectMapper")
    private final ObjectMapper objectMapper;
    
    /**
     * Tạo payment link từ PayOS
     */
    public PayOSCreatePaymentResponse createPaymentLink(
            Long orderCode,
            Long amount,
            String description,
            List<PayOSCreatePaymentRequest.PayOSItem> items,
            Long expiredAtSeconds) {
        
        if (!payOSConfig.isEnabled()) {
            throw new IllegalStateException("PayOS is not enabled");
        }
        
        // Validate required config
        if (payOSConfig.getClientId() == null || payOSConfig.getClientId().isEmpty()) {
            throw new IllegalStateException("PayOS Client ID is not configured. Please set PAYOS_CLIENT_ID environment variable.");
        }
        if (payOSConfig.getApiKey() == null || payOSConfig.getApiKey().isEmpty()) {
            throw new IllegalStateException("PayOS API Key is not configured. Please set PAYOS_API_KEY environment variable.");
        }
        if (payOSConfig.getCancelUrl() == null || payOSConfig.getCancelUrl().isEmpty()) {
            throw new IllegalStateException("PayOS Cancel URL is not configured. Please set PAYOS_CANCEL_URL environment variable.");
        }
        if (payOSConfig.getReturnUrl() == null || payOSConfig.getReturnUrl().isEmpty()) {
            throw new IllegalStateException("PayOS Return URL is not configured. Please set PAYOS_RETURN_URL environment variable.");
        }
        
        PayOSCreatePaymentRequest request = PayOSCreatePaymentRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(description)
                .items(items)
                .cancelUrl(payOSConfig.getCancelUrl())
                .returnUrl(payOSConfig.getReturnUrl())
                .expiredAt(expiredAtSeconds)
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", payOSConfig.getClientId());
        headers.set("x-api-key", payOSConfig.getApiKey());
        
        HttpEntity<PayOSCreatePaymentRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            ResponseEntity<PayOSCreatePaymentResponse> response = restTemplate.exchange(
                    payOSConfig.getApiUrl() + "/payment-requests",
                    HttpMethod.POST,
                    entity,
                    PayOSCreatePaymentResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                PayOSCreatePaymentResponse body = response.getBody();
                if (body.error() != null && body.error() != 0) {
                    log.error("PayOS API error: {} - {}", body.error(), body.message());
                    throw new RuntimeException("PayOS API error: " + body.message());
                }
                return body;
            } else {
                throw new RuntimeException("Failed to create payment link");
            }
        } catch (Exception e) {
            log.error("Error calling PayOS API", e);
            throw new RuntimeException("Failed to create payment link: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify webhook signature từ PayOS
     */
    public boolean verifyWebhookSignature(PayOSWebhookData webhookData) {
        try {
            // Validate checksum key is configured
            if (payOSConfig.getChecksumKey() == null || payOSConfig.getChecksumKey().isEmpty()) {
                log.error("PayOS Checksum Key is not configured");
                return false;
            }
            
            // Tạo data string để verify
            PayOSWebhookData.PayOSWebhookPaymentData data = webhookData.data();
            if (data == null) {
                log.error("Webhook data is null");
                return false;
            }
            
            String cancelUrl = payOSConfig.getCancelUrl() != null ? payOSConfig.getCancelUrl() : "";
            String returnUrl = payOSConfig.getReturnUrl() != null ? payOSConfig.getReturnUrl() : "";
            
            String dataString = String.format(
                    "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    data.amount(),
                    cancelUrl,
                    data.description() != null ? data.description() : "",
                    data.orderCode(),
                    returnUrl
            );
            
            // Tạo HMAC SHA256 signature
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    payOSConfig.getChecksumKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmacSha256.init(secretKey);
            byte[] hash = hmacSha256.doFinal(dataString.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);
            
            // So sánh signature
            return calculatedSignature.equals(webhookData.signature());
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature", e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error verifying webhook signature", e);
            return false;
        }
    }
    
    /**
     * Tạo order code unique (sử dụng timestamp + random)
     */
    public Long generateOrderCode() {
        // PayOS yêu cầu orderCode là số nguyên dương từ 1 đến 9,999,999,999
        // Sử dụng timestamp (seconds) * 1000 + random 3 digits để đảm bảo unique
        long timestamp = Instant.now().getEpochSecond();
        int random = (int) (Math.random() * 1000);
        return timestamp * 1000L + random;
    }
}


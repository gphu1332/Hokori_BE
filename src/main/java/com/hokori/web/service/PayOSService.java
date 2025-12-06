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
@Slf4j
public class PayOSService {
    
    private final PayOSConfig payOSConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public PayOSService(
            PayOSConfig payOSConfig,
            @Qualifier("payOSRestTemplate") RestTemplate restTemplate,
            @Qualifier("payOSObjectMapper") ObjectMapper objectMapper) {
        this.payOSConfig = payOSConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
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
        
        // Retry mechanism for DNS resolution issues
        int maxRetries = 3;
        int retryCount = 0;
        long retryDelayMs = 1000; // Start with 1 second delay
        
        while (retryCount < maxRetries) {
            try {
                String apiUrl = payOSConfig.getApiUrl() != null ? payOSConfig.getApiUrl().trim() : "";
                String url = apiUrl + "/payment-requests";
                log.info("Calling PayOS API (attempt {}/{}): {}", retryCount + 1, maxRetries, url);
                String apiUrlForLog = payOSConfig.getApiUrl() != null ? payOSConfig.getApiUrl().trim() : "NULL";
                log.info("PayOS Config - Client ID: {}, API URL: {}", 
                        payOSConfig.getClientId() != null ? payOSConfig.getClientId().substring(0, 8) + "..." : "NULL",
                        apiUrlForLog);
                log.debug("PayOS request: orderCode={}, amount={}, description={}", orderCode, amount, description);
                
                ResponseEntity<PayOSCreatePaymentResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        PayOSCreatePaymentResponse.class
                );
                
                log.info("PayOS API response status: {}", response.getStatusCode());
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    PayOSCreatePaymentResponse body = response.getBody();
                    if (body.getError() != null && body.getError() != 0) {
                        log.error("PayOS API error: {} - {}", body.getError(), body.getMessage());
                        throw new RuntimeException("PayOS API error: " + body.getMessage());
                    }
                    log.info("PayOS payment link created successfully for orderCode: {}", orderCode);
                    return body;
                } else {
                    log.error("PayOS API returned non-2xx status: {}", response.getStatusCode());
                    throw new RuntimeException("Failed to create payment link: HTTP " + response.getStatusCode());
                }
            } catch (org.springframework.web.client.ResourceAccessException e) {
                // DNS resolution or connection issues - retry with exponential backoff
                retryCount++;
                String rootCause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                
                if (retryCount < maxRetries) {
                    log.warn("Network error calling PayOS API (attempt {}/{}). Retrying in {}ms. Error: {}", 
                            retryCount, maxRetries, retryDelayMs, rootCause);
                    try {
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff: 1s, 2s, 4s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    continue; // Retry
                } else {
                    // Max retries reached
                    String apiUrl = payOSConfig.getApiUrl() != null ? payOSConfig.getApiUrl().trim() : "";
                    log.error("Network error calling PayOS API after {} attempts. URL: {}, Error: {}, Root cause: {}", 
                            maxRetries, apiUrl + "/payment-requests", e.getMessage(), rootCause, e);
                    throw new RuntimeException(
                            String.format("Failed to connect to PayOS API at %s after %d attempts. Error: %s. Please check: 1) Network connectivity from Railway, 2) PayOS API status, 3) DNS resolution", 
                                    apiUrl, maxRetries, rootCause), e);
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // HTTP 4xx errors - don't retry
                log.error("HTTP error calling PayOS API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("PayOS API returned error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                // HTTP 5xx errors - don't retry
                log.error("PayOS server error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
                throw new RuntimeException("PayOS server error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
            } catch (Exception e) {
                // Other errors - don't retry
                log.error("Unexpected error calling PayOS API", e);
                throw new RuntimeException("Failed to create payment link: " + e.getMessage(), e);
            }
        }
        
        // Should never reach here, but compiler needs it
        throw new RuntimeException("Failed to create payment link after " + maxRetries + " attempts");
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
            PayOSWebhookData.PayOSWebhookPaymentData data = webhookData.getData();
            if (data == null) {
                log.error("Webhook data is null");
                return false;
            }
            
            String cancelUrl = payOSConfig.getCancelUrl() != null ? payOSConfig.getCancelUrl() : "";
            String returnUrl = payOSConfig.getReturnUrl() != null ? payOSConfig.getReturnUrl() : "";
            
            String dataString = String.format(
                    "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                    data.getAmount(),
                    cancelUrl,
                    data.getDescription() != null ? data.getDescription() : "",
                    data.getOrderCode(),
                    returnUrl
            );
            
            log.debug("Webhook signature verification - dataString: {}", dataString);
            log.debug("Webhook signature verification - received signature: {}", webhookData.getSignature());
            
            // Tạo HMAC SHA256 signature
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    payOSConfig.getChecksumKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmacSha256.init(secretKey);
            byte[] hash = hmacSha256.doFinal(dataString.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);
            
            log.debug("Webhook signature verification - calculated signature: {}", calculatedSignature);
            log.debug("Webhook signature verification - signatures match: {}", calculatedSignature.equals(webhookData.getSignature()));
            
            // So sánh signature
            boolean isValid = calculatedSignature.equals(webhookData.getSignature());
            if (!isValid) {
                log.warn("Webhook signature mismatch for orderCode: {}. Received: {}, Calculated: {}", 
                        data.getOrderCode(), webhookData.getSignature(), calculatedSignature);
            }
            return isValid;
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


package com.hokori.web.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class PayOSCreatePaymentResponse {
    
    @JsonProperty("code")
    String code;
    
    @JsonProperty("desc")
    String desc;
    
    @JsonProperty("data")
    PayOSPaymentData data;
    
    @Value
    public static class PayOSPaymentData {
        @JsonProperty("bin")
        String bin;
        
        @JsonProperty("accountNumber")
        String accountNumber;
        
        @JsonProperty("accountName")
        String accountName;
        
        @JsonProperty("amount")
        Long amount;
        
        @JsonProperty("description")
        String description;
        
        @JsonProperty("orderCode")
        Long orderCode;
        
        @JsonProperty("currency")
        String currency;
        
        @JsonProperty("paymentLinkId")
        String paymentLinkId;
        
        @JsonProperty("status")
        String status;
        
        @JsonProperty("checkoutUrl")
        String checkoutUrl;
        
        @JsonProperty("qrCode")
        String qrCode;
        
        @JsonProperty("expiredAt")
        Long expiredAt;
    }
}


package com.hokori.web.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PayOSCreatePaymentRequest {
    
    @JsonProperty("orderCode")
    Long orderCode;
    
    @JsonProperty("amount")
    Long amount; // Số tiền (đơn vị: VND)
    
    @JsonProperty("description")
    String description;
    
    @JsonProperty("items")
    List<PayOSItem> items;
    
    @JsonProperty("cancelUrl")
    String cancelUrl;
    
    @JsonProperty("returnUrl")
    String returnUrl;
    
    @JsonProperty("expiredAt")
    Long expiredAt; // Unix timestamp (seconds)
    
    @Value
    @Builder
    public static class PayOSItem {
        @JsonProperty("name")
        String name;
        
        @JsonProperty("quantity")
        Integer quantity;
        
        @JsonProperty("price")
        Long price; // Đơn vị: VND
    }
}


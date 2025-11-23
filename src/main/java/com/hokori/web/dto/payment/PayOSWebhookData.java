package com.hokori.web.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class PayOSWebhookData {
    
    @JsonProperty("code")
    String code;
    
    @JsonProperty("desc")
    String desc;
    
    @JsonProperty("data")
    PayOSWebhookPaymentData data;
    
    @JsonProperty("signature")
    String signature;
    
    @Value
    public static class PayOSWebhookPaymentData {
        @JsonProperty("orderCode")
        Long orderCode;
        
        @JsonProperty("amount")
        Long amount;
        
        @JsonProperty("description")
        String description;
        
        @JsonProperty("accountNumber")
        String accountNumber;
        
        @JsonProperty("reference")
        String reference;
        
        @JsonProperty("transactionDateTime")
        String transactionDateTime;
        
        @JsonProperty("currency")
        String currency;
        
        @JsonProperty("paymentLinkId")
        String paymentLinkId;
        
        @JsonProperty("code")
        String code;
        
        @JsonProperty("desc")
        String desc;
        
        @JsonProperty("counterAccountBankId")
        String counterAccountBankId;
        
        @JsonProperty("counterAccountBankName")
        String counterAccountBankName;
        
        @JsonProperty("counterAccountName")
        String counterAccountName;
        
        @JsonProperty("counterAccountNumber")
        String counterAccountNumber;
        
        @JsonProperty("virtualAccountName")
        String virtualAccountName;
        
        @JsonProperty("virtualAccountNumber")
        String virtualAccountNumber;
    }
}


package com.hokori.web.dto.wallet;

import com.hokori.web.Enum.WalletTransactionSource;
import com.hokori.web.Enum.WalletTransactionStatus;
import com.hokori.web.entity.WalletTransaction;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class WalletTransactionResponse {

    Long id;
    WalletTransactionStatus status;
    Long amountCents;
    Long balanceAfterCents;
    WalletTransactionSource source;
    Long courseId;
    String courseTitle;
    String description;
    Instant createdAt;

    public static WalletTransactionResponse fromEntity(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .status(tx.getStatus())
                .amountCents(tx.getAmountCents())
                .balanceAfterCents(tx.getBalanceAfterCents())
                .source(tx.getSource())
                .courseId(tx.getCourse() != null ? tx.getCourse().getId() : null)
                .courseTitle(tx.getCourse() != null ? tx.getCourse().getTitle() : null)
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

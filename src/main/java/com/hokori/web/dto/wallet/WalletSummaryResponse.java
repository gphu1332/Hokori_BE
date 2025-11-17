package com.hokori.web.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor  // <- cần cái này cho JPQL constructor expression
public class WalletSummaryResponse {
    private Long userId;
    private Long walletBalance;
    private LocalDate lastPayoutDate;
}

package com.hokori.web.dto.wallet;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class WalletSummaryResponse {

    Long userId;
    Long walletBalance;
    LocalDate lastPayoutDate;
}

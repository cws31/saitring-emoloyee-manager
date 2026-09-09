package com.sonuSaitring.sonuSaitringManagement.dashboard.dto;

import java.math.BigDecimal;

public record FinancialSummary(
        BigDecimal totalEarnings,
        BigDecimal totalAdvances,
        BigDecimal previousBalance,
        BigDecimal netPayable,
        BigDecimal amountPaid,
        BigDecimal remainingBalance) {
}
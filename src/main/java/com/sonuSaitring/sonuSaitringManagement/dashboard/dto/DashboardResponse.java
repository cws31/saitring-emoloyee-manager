package com.sonuSaitring.sonuSaitringManagement.dashboard.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long totalActiveEmployees,
        BigDecimal totalPayableAmount,
        BigDecimal totalAdvanceAmount,
        BigDecimal totalOverAdvanceAmount) {
}
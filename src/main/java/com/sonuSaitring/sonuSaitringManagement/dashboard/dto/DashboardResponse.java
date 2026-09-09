package com.sonuSaitring.sonuSaitringManagement.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalEmployees,
        BigDecimal totalPayable,
        BigDecimal totalAdvance,
        BigDecimal totalOverAdvance,
        List<EmployeeFinancialStatus> employees) {
}
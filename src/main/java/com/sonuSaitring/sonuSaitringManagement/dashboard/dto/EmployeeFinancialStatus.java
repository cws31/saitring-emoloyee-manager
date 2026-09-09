package com.sonuSaitring.sonuSaitringManagement.dashboard.dto;

import java.math.BigDecimal;

public record EmployeeFinancialStatus(
        Long employeeId,
        String employeeName,
        BigDecimal payableAmount,
        BigDecimal advanceAmount,
        BigDecimal amountPaid,
        BigDecimal dueAmount) {
}
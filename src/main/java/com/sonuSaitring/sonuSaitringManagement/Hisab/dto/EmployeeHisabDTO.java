package com.sonuSaitring.sonuSaitringManagement.Hisab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class EmployeeHisabDTO {
    private Long detailId;
    private Long employeeId;
    private String employeeName;
    private Integer presentDays;
    private Integer halfDays;
    private BigDecimal attendanceUnits;
    private BigDecimal dailyRate;
    private BigDecimal totalEarning;
    private BigDecimal openingBalance;
    private BigDecimal totalAdvance;
    private BigDecimal totalSettlement;
    private BigDecimal closingBalance;
    private String balanceStatus;
}

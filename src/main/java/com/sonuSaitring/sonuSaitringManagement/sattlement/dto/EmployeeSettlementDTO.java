package com.sonuSaitring.sonuSaitringManagement.sattlement.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeSettlementDTO {
    private Long employeeId;
    private BigDecimal amountPaid;
    private LocalDate settlementDate;
    private String note;
}
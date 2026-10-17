package com.sonuSaitring.sonuSaitringManagement.sattlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SettlementResponseDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long monthClosingDetailId;
    private Integer year;
    private Integer month;
    private BigDecimal amountPaid;
    private LocalDate settlementDate;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

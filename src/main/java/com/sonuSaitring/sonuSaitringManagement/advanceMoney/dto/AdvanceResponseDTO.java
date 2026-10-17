package com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdvanceResponseDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String note;
    private BigDecimal runningBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

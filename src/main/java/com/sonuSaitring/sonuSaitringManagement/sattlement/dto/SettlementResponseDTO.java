package com.sonuSaitring.sonuSaitringManagement.sattlement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter 
@Getter 
@AllArgsConstructor @NoArgsConstructor 
public class SettlementResponseDTO {

    private Long id;

    private Long employeeId;
    private String employeeName;
    private String employeeMobile;

    private BigDecimal amountPaid;
    private LocalDate settlementDate;
    private String note;
    private LocalDateTime createdAt;
}
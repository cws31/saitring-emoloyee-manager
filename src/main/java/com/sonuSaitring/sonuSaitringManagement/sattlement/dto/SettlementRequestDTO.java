package com.sonuSaitring.sonuSaitringManagement.sattlement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SettlementRequestDTO {
    @NotNull
    private Long employeeId;
    @NotNull
    private Integer year;
    @NotNull
    private Integer month;
    @NotNull
    @DecimalMin(value = "0.01", message = "Settlement amount must be greater than zero")
    private BigDecimal amountPaid;
    @NotNull
    private LocalDate settlementDate;
    @Size(max = 255)
    private String note;
    @Size(max = 500)
    private String reason;
}

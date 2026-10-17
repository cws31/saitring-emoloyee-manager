package com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdvanceRequestDTO {
    @NotNull
    private Long employeeId;
    @NotNull
    @DecimalMin(value = "0.01", message = "Advance amount must be greater than zero")
    private BigDecimal amount;
    @NotNull
    private LocalDate paymentDate;
    @Size(max = 255)
    private String note;
    @Size(max = 500)
    private String reason;
}

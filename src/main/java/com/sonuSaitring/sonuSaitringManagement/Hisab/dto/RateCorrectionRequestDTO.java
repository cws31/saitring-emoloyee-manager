package com.sonuSaitring.sonuSaitringManagement.Hisab.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RateCorrectionRequestDTO {
    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal dailyRate;
    @NotNull
    @Size(min = 3, max = 500)
    private String reason;
}

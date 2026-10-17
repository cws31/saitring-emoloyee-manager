package com.sonuSaitring.sonuSaitringManagement.Hisab.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OpeningBalanceRequestDTO {
    @NotNull
    private BigDecimal openingBalance;
    @NotNull
    @Size(min = 3, max = 500)
    private String reason;
}

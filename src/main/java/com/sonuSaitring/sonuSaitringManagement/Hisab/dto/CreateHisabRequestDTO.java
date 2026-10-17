package com.sonuSaitring.sonuSaitringManagement.Hisab.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateHisabRequestDTO {
    @NotNull
    @Min(2000)
    @Max(2200)
    private Integer year;
    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;
}

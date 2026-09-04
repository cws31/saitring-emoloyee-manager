package com.sonuSaitring.sonuSaitringManagement.Hisab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HisabRequestDTO {
    private int year;
    private int month;
    private Map<Long, BigDecimal> employeeExtraMoneyMap;
}
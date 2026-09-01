package com.sonuSaitring.sonuSaitringManagement.monthClosing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthClosingRequestDTO {
    private int year;
    private int month;

    private Map<Long, BigDecimal> employeeExtraMoneyMap;
}
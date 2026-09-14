package com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor 
@NoArgsConstructor 
public class AdvanceRequestDTO {
    private Long employeeId;
    private Double amount;
    private LocalDate paymentDate;
    private String note;

}
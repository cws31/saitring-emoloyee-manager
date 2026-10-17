package com.sonuSaitring.sonuSaitringManagement.Hisab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class HisabResponseDTO {
    private Long id;
    private Integer year;
    private Integer month;
    private String status;
    private LocalDateTime closedAt;
    private Long totalEmployees;
    private BigDecimal totalPayable;
    private BigDecimal totalOverAdvance;
    private List<EmployeeHisabDTO> employees;
}

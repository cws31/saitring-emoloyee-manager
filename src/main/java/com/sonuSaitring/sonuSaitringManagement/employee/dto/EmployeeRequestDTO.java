package com.sonuSaitring.sonuSaitringManagement.employee.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmployeeRequestDTO {

    @NotBlank(message = "Employee name is required")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid mobile number format")
    private String mobile;

    @PositiveOrZero(message = "Initial rate must be zero or positive")
    private BigDecimal initialRate;
}
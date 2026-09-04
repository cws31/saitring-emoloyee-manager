package com.sonuSaitring.sonuSaitringManagement.sattlement.entity;

import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_settlements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private BigDecimal amountPaid;

    private LocalDate settlementDate;

    private String note; 

    private LocalDateTime createdAt;
}
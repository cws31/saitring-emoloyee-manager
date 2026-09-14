package com.sonuSaitring.sonuSaitringManagement.sattlement.entity;

import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_settlements", indexes = {
        @Index(name = "idx_settlement_owner_id", columnList = "owner_id"),
        @Index(name = "idx_settlement_owner_date", columnList = "owner_id, settlement_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    private BigDecimal amountPaid;

    private LocalDate settlementDate;

    private String note;

    private LocalDateTime createdAt;
}
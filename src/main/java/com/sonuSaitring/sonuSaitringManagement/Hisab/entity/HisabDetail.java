package com.sonuSaitring.sonuSaitringManagement.Hisab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "month_closing_detail")
@Data
public class HisabDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "month_closing_id")
    @JsonIgnore
    private Hisab monthClosing;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private String employeeName;
    private BigDecimal totalPresences;
    private BigDecimal rate;
    private BigDecimal totalEarning;
    private BigDecimal totalAdvance;
    private BigDecimal previousBalance;
    private BigDecimal extraMoney;
    private BigDecimal netPayable;
    private BigDecimal amountPaid;
    private BigDecimal remainingBalance;

    @Column(nullable = false)
    private boolean hisabCompleted = false;

    @Transient
    private List<EmployeeSettlement> settlements;
}
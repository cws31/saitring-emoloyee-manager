package com.sonuSaitring.sonuSaitringManagement.monthClosing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "month_closing_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthClosingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "month_closing_id")
    private MonthClosing monthClosing;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private String employeeName;

    private BigDecimal totalPresences;

    private BigDecimal rate;

    private BigDecimal totalEarning;

    private BigDecimal totalAdvance;

    private BigDecimal extraMoney;

    private BigDecimal netPayable;
}
package com.sonuSaitring.sonuSaitringManagement.Hisab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "month_closing_detail", indexes = @Index(name = "idx_month_detail_employee", columnList = "employee_id,month_closing_id"))
@Data
@NoArgsConstructor
public class HisabDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "month_closing_id")
    @JsonIgnore
    private Hisab hisab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @Column(name = "employee_name", length = 255)
    private String employeeName;

    @Column(name = "present_days", nullable = false)
    private Integer presentDays = 0;

    @Column(name = "half_days", nullable = false)
    private Integer halfDays = 0;

    @Column(name = "total_presences", precision = 38, scale = 2)
    private BigDecimal attendanceUnits = BigDecimal.ZERO;

    @Column(name = "rate", precision = 38, scale = 2)
    private BigDecimal rateSnapshot = BigDecimal.ZERO;

    @Column(name = "total_earning", precision = 38, scale = 2)
    private BigDecimal totalEarning = BigDecimal.ZERO;

    @Column(name = "total_advance", precision = 38, scale = 2)
    private BigDecimal totalAdvance = BigDecimal.ZERO;

    @Column(name = "previous_balance", precision = 38, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 38, scale = 2)
    private BigDecimal totalSettlement = BigDecimal.ZERO;

    @Column(name = "extra_money", precision = 38, scale = 2)
    private BigDecimal extraMoney = BigDecimal.ZERO;

    @Column(name = "net_payable", precision = 38, scale = 2)
    private BigDecimal netPayable = BigDecimal.ZERO;

    @Column(name = "remaining_balance", precision = 38, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(name = "hisab_completed", nullable = false)
    private boolean hisabCompleted;

    @Column(name = "manual_opening_balance", nullable = false)
    private boolean manualOpeningBalance;

    @UpdateTimestamp
    @Column(name = "recalculated_at")
    private LocalDateTime recalculatedAt;
}

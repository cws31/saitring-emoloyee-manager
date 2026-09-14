package com.sonuSaitring.sonuSaitringManagement.Hisab.entity;

import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "month_closings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_month_closing_owner_year_month", columnNames = { "owner_id", "year", "month" })
}, indexes = {
        @Index(name = "idx_month_closing_owner_id", columnList = "owner_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hisab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int year;

    private int month;

    private LocalDateTime closedAt;

    @Column(name = "total_employees", nullable = false)
    private Long totalEmployees = 0L;

    @Column(name = "total_payable", precision = 38, scale = 2, nullable = false)
    private BigDecimal totalPayable = BigDecimal.ZERO;

    @Column(name = "total_over_advance", precision = 38, scale = 2, nullable = false)
    private BigDecimal totalOverAdvance = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @OneToMany(mappedBy = "monthClosing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HisabDetail> details = new ArrayList<>();

    public void addDetail(HisabDetail detail) {
        details.add(detail);
        detail.setMonthClosing(this);
    }

    public void clearDetails() {
        for (HisabDetail detail : details) {
            detail.setMonthClosing(null);
        }
        details.clear();
    }
}
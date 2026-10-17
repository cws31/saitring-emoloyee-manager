package com.sonuSaitring.sonuSaitringManagement.Hisab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "month_closings", uniqueConstraints = @UniqueConstraint(name = "uk_month_closing_owner_year_month", columnNames = {
        "owner_id", "year", "month" }))
@Data
@NoArgsConstructor
public class Hisab {
    public enum Status {
        OPEN, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "total_employees", nullable = false)
    private Long totalEmployees = 0L;

    @Column(name = "total_payable", precision = 38, scale = 2, nullable = false)
    private BigDecimal totalPayable = BigDecimal.ZERO;

    @Column(name = "total_over_advance", precision = 38, scale = 2, nullable = false)
    private BigDecimal totalOverAdvance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.OPEN;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private Owner owner;

    @OneToMany(mappedBy = "hisab", fetch = FetchType.LAZY)
    private List<HisabDetail> details = new ArrayList<>();
}

package com.sonuSaitring.sonuSaitringManagement.employee.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_owner_mobile", columnNames = { "owner_id", "mobile" })
}, indexes = {
        @Index(name = "idx_employee_owner_id", columnList = "owner_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(name = "initial_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal initialRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean blocked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private Owner owner;
}
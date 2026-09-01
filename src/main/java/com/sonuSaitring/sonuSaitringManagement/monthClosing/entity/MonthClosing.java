package com.sonuSaitring.sonuSaitringManagement.monthClosing.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "month_closings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int year;
    private int month;

    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "monthClosing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonthClosingDetail> details;
}
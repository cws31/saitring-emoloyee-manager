package com.sonuSaitring.sonuSaitringManagement.Hisab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList; 
import java.util.List;

@Entity
@Table(name = "month_closings")
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
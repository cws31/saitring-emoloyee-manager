package com.sonuSaitring.sonuSaitringManagement.monthClosing.repository;

import com.sonuSaitring.sonuSaitringManagement.monthClosing.entity.MonthClosing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthClosingRepository extends JpaRepository<MonthClosing, Long> {
    Optional<MonthClosing> findByYearAndMonth(int year, int month);

    boolean existsByYearAndMonth(int year, int month);
}
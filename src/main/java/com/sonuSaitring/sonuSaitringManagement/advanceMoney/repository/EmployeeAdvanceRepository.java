package com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeAdvanceRepository extends JpaRepository<EmployeeAdvance, Long> {
    List<EmployeeAdvance> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

    List<EmployeeAdvance> findByEmployeeIdAndPaymentDateBetween(Long employeeId, LocalDate startDate,
            LocalDate endDate);
}
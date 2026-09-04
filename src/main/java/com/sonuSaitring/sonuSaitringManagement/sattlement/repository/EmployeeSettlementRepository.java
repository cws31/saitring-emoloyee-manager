

package com.sonuSaitring.sonuSaitringManagement.sattlement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeSettlementRepository extends JpaRepository<EmployeeSettlement, Long> {
    List<EmployeeSettlement> findByEmployeeId(Long employeeId);
    List<EmployeeSettlement> findBySettlementDateBetween(LocalDate startDate, LocalDate endDate);
}
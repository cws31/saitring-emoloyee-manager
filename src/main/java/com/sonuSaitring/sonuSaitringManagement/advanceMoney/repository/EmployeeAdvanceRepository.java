package com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAdvanceRepository
        extends JpaRepository<EmployeeAdvance, Long> {

    List<EmployeeAdvance> findByOwnerIdAndPaymentDateBetween(
            Long ownerId,
            LocalDate startDate,
            LocalDate endDate);

    List<EmployeeAdvance> findByOwnerIdAndEmployeeIdAndPaymentDateBetween(
            Long ownerId,
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate);

    Optional<EmployeeAdvance> findByIdAndOwnerId(
            Long id,
            Long ownerId);

    boolean existsByIdAndOwnerId(
            Long id,
            Long ownerId);

    void deleteByIdAndOwnerId(
            Long id,
            Long ownerId);
}
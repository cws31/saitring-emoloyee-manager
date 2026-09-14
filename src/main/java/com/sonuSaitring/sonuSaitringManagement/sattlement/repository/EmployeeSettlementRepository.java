package com.sonuSaitring.sonuSaitringManagement.sattlement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSettlementRepository
        extends JpaRepository<EmployeeSettlement, Long> {

    List<EmployeeSettlement> findByOwnerId(Long ownerId);

    List<EmployeeSettlement> findByOwnerIdAndEmployeeId(
            Long ownerId,
            Long employeeId);

    List<EmployeeSettlement> findByOwnerIdAndSettlementDateBetween(
            Long ownerId,
            LocalDate startDate,
            LocalDate endDate);

    Optional<EmployeeSettlement> findByIdAndOwnerId(
            Long id,
            Long ownerId);

    boolean existsByIdAndOwnerId(
            Long id,
            Long ownerId);

    void deleteByIdAndOwnerId(
            Long id,
            Long ownerId);
}
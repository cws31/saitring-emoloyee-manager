package com.sonuSaitring.sonuSaitringManagement.sattlement.repository;

import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeSettlementRepository extends JpaRepository<EmployeeSettlement, Long> {
        Optional<EmployeeSettlement> findByIdAndOwnerId(Long id, Long ownerId);

        List<EmployeeSettlement> findByOwnerIdAndSettlementDateBetweenOrderBySettlementDateAscIdAsc(Long ownerId,
                        LocalDate from, LocalDate to);

        List<EmployeeSettlement> findByOwnerIdAndEmployeeIdAndSettlementDateBetween(Long ownerId, Long employeeId,
                        LocalDate from, LocalDate to);

        List<EmployeeSettlement> findByOwnerIdAndEmployeeId(Long ownerId, Long employeeId);

        List<EmployeeSettlement> findByHisabDetailId(Long hisabDetailId);
}

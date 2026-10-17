package com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeAdvanceRepository extends JpaRepository<EmployeeAdvance, Long> {
        Optional<EmployeeAdvance> findByIdAndOwnerId(Long id, Long ownerId);

        List<EmployeeAdvance> findByOwnerIdAndPaymentDateBetweenOrderByPaymentDateAscIdAsc(Long ownerId, LocalDate from,
                        LocalDate to);

        List<EmployeeAdvance> findByOwnerIdAndEmployeeIdAndPaymentDateBetween(Long ownerId, Long employeeId,
                        LocalDate from, LocalDate to);

        List<EmployeeAdvance> findByOwnerIdAndEmployeeId(Long ownerId, Long employeeId);
}

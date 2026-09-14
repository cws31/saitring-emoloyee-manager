package com.sonuSaitring.sonuSaitringManagement.advanceMoney.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmployeeAdvanceServiceImpl
                implements EmployeeAdvanceService {

        private final EmployeeAdvanceRepository advanceRepository;
        private final EmployeeRepository employeeRepository;
        private final CurrentOwnerService currentOwnerService;

        private final Counter advanceCreated;
        private final Counter advanceUpdated;
        private final Counter advanceDeleted;
        private final Counter advanceRecordFailure;
        private final Counter advanceUpdateFailure;
        private final Counter advanceDeleteFailure;
        private final Counter monthlyAdvanceQuery;

        private final Timer advanceCreateTimer;
        private final Timer advanceUpdateTimer;
        private final Timer advanceDeleteTimer;
        private final Timer monthlyAdvanceQueryTimer;

        public EmployeeAdvanceServiceImpl(
                        EmployeeAdvanceRepository advanceRepository,
                        EmployeeRepository employeeRepository,
                        CurrentOwnerService currentOwnerService,
                        MeterRegistry meterRegistry) {

                this.advanceRepository = advanceRepository;
                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;

                this.advanceCreated = Counter.builder("advance.created")
                                .description("Number of employee advances created")
                                .register(meterRegistry);

                this.advanceUpdated = Counter.builder("advance.updated")
                                .description("Number of employee advances updated")
                                .register(meterRegistry);

                this.advanceDeleted = Counter.builder("advance.deleted")
                                .description("Number of employee advances deleted")
                                .register(meterRegistry);

                this.advanceRecordFailure = Counter.builder("advance.record.failure")
                                .description("Number of failed employee advance creation operations")
                                .register(meterRegistry);

                this.advanceUpdateFailure = Counter.builder("advance.update.failure")
                                .description("Number of failed employee advance update operations")
                                .register(meterRegistry);

                this.advanceDeleteFailure = Counter.builder("advance.delete.failure")
                                .description("Number of failed employee advance deletion operations")
                                .register(meterRegistry);

                this.monthlyAdvanceQuery = Counter.builder("advance.monthly.query")
                                .description("Number of monthly employee advance queries")
                                .register(meterRegistry);

                this.advanceCreateTimer = Timer.builder("advance.create.duration")
                                .description("Time taken to create an employee advance")
                                .register(meterRegistry);

                this.advanceUpdateTimer = Timer.builder("advance.update.duration")
                                .description("Time taken to update an employee advance")
                                .register(meterRegistry);

                this.advanceDeleteTimer = Timer.builder("advance.delete.duration")
                                .description("Time taken to delete an employee advance")
                                .register(meterRegistry);

                this.monthlyAdvanceQueryTimer = Timer.builder("advance.monthly.query.duration")
                                .description("Time taken to fetch monthly employee advances")
                                .register(meterRegistry);
        }

        @Override
        @Transactional
        public AdvanceResponseDTO recordAdvance(
                        AdvanceRequestDTO requestDTO) {

                return advanceCreateTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        log.info(
                                        "Recording advance: employeeId={}, ownerId={}, amount={}",
                                        requestDTO.getEmployeeId(),
                                        ownerId,
                                        requestDTO.getAmount());

                        try {

                                Employee employee = employeeRepository
                                                .findByIdAndOwnerId(
                                                                requestDTO.getEmployeeId(),
                                                                ownerId)
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Employee not found"));

                                EmployeeAdvance advance = new EmployeeAdvance();

                                advance.setEmployee(employee);
                                advance.setOwner(
                                                currentOwnerService.getCurrentOwner());

                                advance.setAmount(requestDTO.getAmount());

                                advance.setPaymentDate(
                                                requestDTO.getPaymentDate() != null
                                                                ? requestDTO.getPaymentDate()
                                                                : LocalDate.now());

                                advance.setNote(requestDTO.getNote());

                                EmployeeAdvance savedAdvance = advanceRepository.save(advance);

                                advanceCreated.increment();

                                log.info(
                                                "Advance created successfully: advanceId={}, employeeId={}, ownerId={}",
                                                savedAdvance.getId(),
                                                requestDTO.getEmployeeId(),
                                                ownerId);

                                return mapToResponseDTO(savedAdvance);

                        } catch (RuntimeException ex) {

                                advanceRecordFailure.increment();

                                log.error(
                                                "Failed to record advance: employeeId={}, ownerId={}, exception={}",
                                                requestDTO.getEmployeeId(),
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        @Transactional(readOnly = true)
        public List<AdvanceResponseDTO> getMonthlyAdvances(
                        int year,
                        int month) {

                return monthlyAdvanceQueryTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        monthlyAdvanceQuery.increment();

                        log.info(
                                        "Fetching monthly advances: year={}, month={}, ownerId={}",
                                        year,
                                        month,
                                        ownerId);

                        LocalDate startDate = LocalDate.of(year, month, 1);

                        LocalDate endDate = startDate.withDayOfMonth(
                                        startDate.lengthOfMonth());

                        List<EmployeeAdvance> advances = advanceRepository
                                        .findByOwnerIdAndPaymentDateBetween(
                                                        ownerId,
                                                        startDate,
                                                        endDate);

                        log.info(
                                        "Monthly advances fetched: year={}, month={}, ownerId={}, count={}",
                                        year,
                                        month,
                                        ownerId,
                                        advances.size());

                        return advances.stream()
                                        .map(this::mapToResponseDTO)
                                        .toList();
                });
        }

        @Override
        @Transactional
        public AdvanceResponseDTO updateAdvance(
                        Long id,
                        AdvanceRequestDTO requestDTO) {

                return advanceUpdateTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        log.info(
                                        "Updating advance: id={}, ownerId={}, employeeId={}",
                                        id,
                                        ownerId,
                                        requestDTO.getEmployeeId());

                        try {

                                EmployeeAdvance advance = advanceRepository
                                                .findByIdAndOwnerId(id, ownerId)
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Advance not found"));

                                Employee employee = employeeRepository
                                                .findByIdAndOwnerId(
                                                                requestDTO.getEmployeeId(),
                                                                ownerId)
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Employee not found"));

                                advance.setEmployee(employee);

                                advance.setOwner(
                                                currentOwnerService.getCurrentOwner());

                                advance.setAmount(
                                                requestDTO.getAmount());

                                if (requestDTO.getPaymentDate() != null) {
                                        advance.setPaymentDate(
                                                        requestDTO.getPaymentDate());
                                }

                                advance.setNote(
                                                requestDTO.getNote());

                                EmployeeAdvance updatedAdvance = advanceRepository.save(advance);

                                advanceUpdated.increment();

                                log.info(
                                                "Advance updated successfully: id={}, ownerId={}",
                                                id,
                                                ownerId);

                                return mapToResponseDTO(updatedAdvance);

                        } catch (RuntimeException ex) {

                                advanceUpdateFailure.increment();

                                log.error(
                                                "Failed to update advance: id={}, ownerId={}, exception={}",
                                                id,
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        @Transactional
        public void deleteAdvance(Long id) {

                advanceDeleteTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        log.info(
                                        "Deleting advance: id={}, ownerId={}",
                                        id,
                                        ownerId);

                        try {

                                EmployeeAdvance advance = advanceRepository
                                                .findByIdAndOwnerId(id, ownerId)
                                                .orElseThrow(() -> new RuntimeException(
                                                                "Advance not found"));

                                advanceRepository.delete(advance);

                                advanceDeleted.increment();

                                log.info(
                                                "Advance deleted successfully: id={}, ownerId={}",
                                                id,
                                                ownerId);

                        } catch (RuntimeException ex) {

                                advanceDeleteFailure.increment();

                                log.error(
                                                "Failed to delete advance: id={}, ownerId={}, exception={}",
                                                id,
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        private AdvanceResponseDTO mapToResponseDTO(
                        EmployeeAdvance advance) {

                Employee employee = advance.getEmployee();

                return new AdvanceResponseDTO(
                                advance.getId(),
                                employee.getId(),
                                employee.getName(),
                                employee.getMobile(),
                                advance.getAmount(),
                                advance.getPaymentDate(),
                                advance.getNote());
        }
}
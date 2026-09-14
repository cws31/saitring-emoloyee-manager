package com.sonuSaitring.sonuSaitringManagement.sattlement.service;

import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.EmployeeSettlementDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.SettlementResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeSettlementServiceImpl
                implements EmployeeSettlementService {

        private static final Logger logger = LoggerFactory.getLogger(EmployeeSettlementServiceImpl.class);

        @Autowired
        private EmployeeSettlementRepository settlementRepository;

        @Autowired
        private EmployeeRepository employeeRepository;

        @Autowired
        private CurrentOwnerService currentOwnerService;

        private final Counter settlementCreated;
        private final Counter settlementUpdated;
        private final Counter settlementDeleted;
        private final Counter settlementNotFound;

        private final Timer settlementCreateTimer;
        private final Timer settlementUpdateTimer;
        private final Timer settlementDeleteTimer;
        private final Timer settlementListTimer;
        private final Timer employeeSettlementListTimer;

        public EmployeeSettlementServiceImpl(
                        MeterRegistry meterRegistry) {

                this.settlementCreated = Counter.builder("settlement.created")
                                .description("Number of employee settlements created")
                                .register(meterRegistry);

                this.settlementUpdated = Counter.builder("settlement.updated")
                                .description("Number of employee settlements updated")
                                .register(meterRegistry);

                this.settlementDeleted = Counter.builder("settlement.deleted")
                                .description("Number of employee settlements deleted")
                                .register(meterRegistry);

                this.settlementNotFound = Counter.builder("settlement.not_found")
                                .description("Number of settlement or employee lookup failures")
                                .register(meterRegistry);

                this.settlementCreateTimer = Timer.builder("settlement.create.duration")
                                .description("Time taken to create a settlement")
                                .register(meterRegistry);

                this.settlementUpdateTimer = Timer.builder("settlement.update.duration")
                                .description("Time taken to update a settlement")
                                .register(meterRegistry);

                this.settlementDeleteTimer = Timer.builder("settlement.delete.duration")
                                .description("Time taken to delete a settlement")
                                .register(meterRegistry);

                this.settlementListTimer = Timer.builder("settlement.list.duration")
                                .description("Time taken to fetch all settlements")
                                .register(meterRegistry);

                this.employeeSettlementListTimer = Timer.builder("settlement.employee.list.duration")
                                .description("Time taken to fetch settlements for an employee")
                                .register(meterRegistry);
        }

        @Override
        @Transactional
        public SettlementResponseDTO saveSettlement(
                        EmployeeSettlementDTO dto) {

                return settlementCreateTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        Owner owner = currentOwnerService.getCurrentOwner();

                        logger.info(
                                        "Recording payment settlement: employeeId={}, ownerId={}, amount={}",
                                        dto.getEmployeeId(),
                                        ownerId,
                                        dto.getAmountPaid());

                        try {

                                Employee employee = employeeRepository
                                                .findByIdAndOwnerId(
                                                                dto.getEmployeeId(),
                                                                ownerId)
                                                .orElseThrow(() -> {

                                                        settlementNotFound.increment();

                                                        logger.warn(
                                                                        "Failed to save settlement: employee not found with id={}, ownerId={}",
                                                                        dto.getEmployeeId(),
                                                                        ownerId);

                                                        return new ResourceNotFoundException(
                                                                        "Employee not found with id: "
                                                                                        + dto.getEmployeeId());
                                                });

                                EmployeeSettlement settlement = new EmployeeSettlement();

                                settlement.setEmployee(employee);
                                settlement.setOwner(owner);

                                settlement.setAmountPaid(
                                                dto.getAmountPaid());

                                settlement.setSettlementDate(
                                                dto.getSettlementDate() != null
                                                                ? dto.getSettlementDate()
                                                                : LocalDate.now());

                                settlement.setNote(
                                                dto.getNote());

                                settlement.setCreatedAt(
                                                LocalDateTime.now());

                                EmployeeSettlement savedSettlement = settlementRepository.save(settlement);

                                settlementCreated.increment();

                                logger.info(
                                                "Settlement created successfully: settlementId={}, ownerId={}",
                                                savedSettlement.getId(),
                                                ownerId);

                                return mapToResponse(savedSettlement);

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Settlement creation failed: employeeId={}, ownerId={}, exception={}",
                                                dto.getEmployeeId(),
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        @Transactional
        public SettlementResponseDTO updateSettlement(
                        Long id,
                        EmployeeSettlementDTO dto) {

                return settlementUpdateTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Updating settlement: settlementId={}, ownerId={}",
                                        id,
                                        ownerId);

                        try {

                                EmployeeSettlement settlement = settlementRepository
                                                .findByIdAndOwnerId(
                                                                id,
                                                                ownerId)
                                                .orElseThrow(() -> {

                                                        settlementNotFound.increment();

                                                        logger.warn(
                                                                        "Settlement record not found: settlementId={}, ownerId={}",
                                                                        id,
                                                                        ownerId);

                                                        return new ResourceNotFoundException(
                                                                        "Settlement record not found with id: "
                                                                                        + id);
                                                });

                                if (dto.getEmployeeId() != null) {

                                        Employee employee = employeeRepository
                                                        .findByIdAndOwnerId(
                                                                        dto.getEmployeeId(),
                                                                        ownerId)
                                                        .orElseThrow(() -> {

                                                                settlementNotFound.increment();

                                                                logger.warn(
                                                                                "Employee not found while updating settlement: employeeId={}, ownerId={}",
                                                                                dto.getEmployeeId(),
                                                                                ownerId);

                                                                return new ResourceNotFoundException(
                                                                                "Employee not found with id: "
                                                                                                + dto.getEmployeeId());
                                                        });

                                        settlement.setEmployee(employee);
                                }

                                if (dto.getAmountPaid() != null) {

                                        settlement.setAmountPaid(
                                                        dto.getAmountPaid());
                                }

                                if (dto.getSettlementDate() != null) {

                                        settlement.setSettlementDate(
                                                        dto.getSettlementDate());
                                }

                                if (dto.getNote() != null) {

                                        settlement.setNote(
                                                        dto.getNote());
                                }

                                EmployeeSettlement updatedSettlement = settlementRepository.save(settlement);

                                settlementUpdated.increment();

                                logger.info(
                                                "Settlement updated successfully: settlementId={}, ownerId={}",
                                                id,
                                                ownerId);

                                return mapToResponse(updatedSettlement);

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Settlement update failed: settlementId={}, ownerId={}, exception={}",
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
        public void deleteSettlement(Long id) {

                settlementDeleteTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Deleting settlement: settlementId={}, ownerId={}",
                                        id,
                                        ownerId);

                        try {

                                if (!settlementRepository.existsByIdAndOwnerId(
                                                id,
                                                ownerId)) {

                                        settlementNotFound.increment();

                                        logger.warn(
                                                        "Settlement record not found: settlementId={}, ownerId={}",
                                                        id,
                                                        ownerId);

                                        throw new ResourceNotFoundException(
                                                        "Settlement record not found with id: "
                                                                        + id);
                                }

                                settlementRepository.deleteByIdAndOwnerId(
                                                id,
                                                ownerId);

                                settlementDeleted.increment();

                                logger.info(
                                                "Settlement deleted successfully: settlementId={}, ownerId={}",
                                                id,
                                                ownerId);

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Settlement deletion failed: settlementId={}, ownerId={}, exception={}",
                                                id,
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        @Transactional(readOnly = true)
        public List<SettlementResponseDTO> getAllSettlements() {

                return settlementListTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Fetching all employee settlement records: ownerId={}",
                                        ownerId);

                        List<SettlementResponseDTO> settlements = settlementRepository
                                        .findByOwnerId(ownerId)
                                        .stream()
                                        .map(this::mapToResponse)
                                        .toList();

                        logger.info(
                                        "Settlement records fetched: ownerId={}, count={}",
                                        ownerId,
                                        settlements.size());

                        return settlements;
                });
        }

        @Override
        @Transactional(readOnly = true)
        public List<SettlementResponseDTO> getSettlementsByEmployee(
                        Long employeeId) {

                return employeeSettlementListTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Fetching settlements for employee: employeeId={}, ownerId={}",
                                        employeeId,
                                        ownerId);

                        employeeRepository
                                        .findByIdAndOwnerId(
                                                        employeeId,
                                                        ownerId)
                                        .orElseThrow(() -> {

                                                settlementNotFound.increment();

                                                logger.warn(
                                                                "Employee not found while fetching settlements: employeeId={}, ownerId={}",
                                                                employeeId,
                                                                ownerId);

                                                return new ResourceNotFoundException(
                                                                "Employee not found with id: "
                                                                                + employeeId);
                                        });

                        List<SettlementResponseDTO> settlements = settlementRepository
                                        .findByOwnerIdAndEmployeeId(
                                                        ownerId,
                                                        employeeId)
                                        .stream()
                                        .map(this::mapToResponse)
                                        .toList();

                        logger.info(
                                        "Employee settlements fetched: employeeId={}, ownerId={}, count={}",
                                        employeeId,
                                        ownerId,
                                        settlements.size());

                        return settlements;
                });
        }

        private SettlementResponseDTO mapToResponse(
                        EmployeeSettlement settlement) {

                Employee employee = settlement.getEmployee();

                return new SettlementResponseDTO(
                                settlement.getId(),
                                employee != null
                                                ? employee.getId()
                                                : null,
                                employee != null
                                                ? employee.getName()
                                                : null,
                                employee != null
                                                ? employee.getMobile()
                                                : null,
                                settlement.getAmountPaid(),
                                settlement.getSettlementDate(),
                                settlement.getNote(),
                                settlement.getCreatedAt());
        }
}
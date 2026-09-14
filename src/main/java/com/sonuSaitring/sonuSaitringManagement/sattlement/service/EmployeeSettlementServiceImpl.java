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

        @Override
        @Transactional
        public SettlementResponseDTO saveSettlement(
                        EmployeeSettlementDTO dto) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                Owner owner = currentOwnerService.getCurrentOwner();

                logger.info(
                                "Recording payment settlement of amount {} for employeeId: {} and ownerId: {}",
                                dto.getAmountPaid(),
                                dto.getEmployeeId(),
                                ownerId);

                Employee employee = employeeRepository
                                .findByIdAndOwnerId(
                                                dto.getEmployeeId(),
                                                ownerId)
                                .orElseThrow(() -> {

                                        logger.warn(
                                                        "Failed to save settlement: Employee not found with id: {} for ownerId: {}",
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

                logger.info(
                                "Successfully saved settlement record ID: {} for ownerId: {}",
                                savedSettlement.getId(),
                                ownerId);

                return mapToResponse(savedSettlement);
        }

        @Override
        @Transactional
        public SettlementResponseDTO updateSettlement(
                        Long id,
                        EmployeeSettlementDTO dto) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Updating settlement record ID: {} for ownerId: {}",
                                id,
                                ownerId);

                EmployeeSettlement settlement = settlementRepository
                                .findByIdAndOwnerId(
                                                id,
                                                ownerId)
                                .orElseThrow(() -> {

                                        logger.warn(
                                                        "Settlement record not found with id: {} for ownerId: {}",
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
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Employee not found with id: "
                                                                        + dto.getEmployeeId()));

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

                logger.info(
                                "Successfully updated settlement record ID: {} for ownerId: {}",
                                id,
                                ownerId);

                return mapToResponse(updatedSettlement);
        }

        @Override
        @Transactional
        public void deleteSettlement(Long id) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Deleting settlement record ID: {} for ownerId: {}",
                                id,
                                ownerId);

                if (!settlementRepository.existsByIdAndOwnerId(
                                id,
                                ownerId)) {

                        logger.warn(
                                        "Settlement record not found with id: {} for ownerId: {}",
                                        id,
                                        ownerId);

                        throw new ResourceNotFoundException(
                                        "Settlement record not found with id: "
                                                        + id);
                }

                settlementRepository.deleteByIdAndOwnerId(
                                id,
                                ownerId);

                logger.info(
                                "Successfully deleted settlement record ID: {} for ownerId: {}",
                                id,
                                ownerId);
        }

        @Override
        @Transactional(readOnly = true)
        public List<SettlementResponseDTO> getAllSettlements() {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Fetching all employee settlement records for ownerId: {}",
                                ownerId);

                return settlementRepository
                                .findByOwnerId(ownerId)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<SettlementResponseDTO> getSettlementsByEmployee(
                        Long employeeId) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Fetching settlements for employeeId: {} and ownerId: {}",
                                employeeId,
                                ownerId);

                employeeRepository
                                .findByIdAndOwnerId(
                                                employeeId,
                                                ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Employee not found with id: "
                                                                + employeeId));

                return settlementRepository
                                .findByOwnerIdAndEmployeeId(
                                                ownerId,
                                                employeeId)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
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
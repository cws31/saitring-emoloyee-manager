package com.sonuSaitring.sonuSaitringManagement.sattlement.service;

import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.EmployeeSettlementDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeSettlementServiceImpl implements EmployeeSettlementService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeSettlementServiceImpl.class);

    @Autowired
    private EmployeeSettlementRepository settlementRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public EmployeeSettlement saveSettlement(EmployeeSettlementDTO dto) {
        logger.info("Recording payment settlement of amount {} for employeeId: {}", dto.getAmountPaid(),
                dto.getEmployeeId());
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> {
                    logger.error("Failed to save settlement: Employee not found with id: {}", dto.getEmployeeId());
                    return new RuntimeException("Employee not found");
                });

        EmployeeSettlement settlement = new EmployeeSettlement();
        settlement.setEmployee(employee);
        settlement.setAmountPaid(dto.getAmountPaid());
        settlement.setSettlementDate(
                dto.getSettlementDate() != null ? dto.getSettlementDate() : java.time.LocalDate.now());
        settlement.setNote(dto.getNote());
        settlement.setCreatedAt(LocalDateTime.now());

        EmployeeSettlement savedSettlement = settlementRepository.save(settlement);
        logger.info("Successfully saved settlement record ID: {}", savedSettlement.getId());
        return savedSettlement;
    }

    public EmployeeSettlement updateSettlement(Long id, EmployeeSettlementDTO dto) {
        logger.info("Updating settlement record ID: {}", id);
        EmployeeSettlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Settlement record not found with id: {}", id);
                    return new RuntimeException("Settlement record not found");
                });

        if (dto.getAmountPaid() != null)
            settlement.setAmountPaid(dto.getAmountPaid());
        if (dto.getSettlementDate() != null)
            settlement.setSettlementDate(dto.getSettlementDate());
        if (dto.getNote() != null)
            settlement.setNote(dto.getNote());

        EmployeeSettlement updatedSettlement = settlementRepository.save(settlement);
        logger.info("Successfully updated settlement record ID: {}", id);
        return updatedSettlement;
    }

    public void deleteSettlement(Long id) {
        logger.info("Deleting settlement record ID: {}", id);
        settlementRepository.deleteById(id);
        logger.info("Successfully deleted settlement record ID: {}", id);
    }

    public List<EmployeeSettlement> getAllSettlements() {
        logger.info("Fetching all employee settlement records.");
        return settlementRepository.findAll();
    }

    public List<EmployeeSettlement> getSettlementsByEmployee(Long employeeId) {
        logger.info("Fetching settlements for employeeId: {}", employeeId);
        return settlementRepository.findByEmployeeId(employeeId);
    }
}
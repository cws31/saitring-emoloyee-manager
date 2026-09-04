package com.sonuSaitring.sonuSaitringManagement.sattlement.service;

import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.EmployeeSettlementDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeSettlementServiceImpl {

    @Autowired
    private EmployeeSettlementRepository settlementRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public EmployeeSettlement saveSettlement(EmployeeSettlementDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        EmployeeSettlement settlement = new EmployeeSettlement();
        settlement.setEmployee(employee);
        settlement.setAmountPaid(dto.getAmountPaid());
        settlement.setSettlementDate(
                dto.getSettlementDate() != null ? dto.getSettlementDate() : java.time.LocalDate.now());
        settlement.setNote(dto.getNote());
        settlement.setCreatedAt(LocalDateTime.now());

        return settlementRepository.save(settlement);
    }

    public EmployeeSettlement updateSettlement(Long id, EmployeeSettlementDTO dto) {
        EmployeeSettlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement record not found"));

        if (dto.getAmountPaid() != null)
            settlement.setAmountPaid(dto.getAmountPaid());
        if (dto.getSettlementDate() != null)
            settlement.setSettlementDate(dto.getSettlementDate());
        if (dto.getNote() != null)
            settlement.setNote(dto.getNote());

        return settlementRepository.save(settlement);
    }

    public void deleteSettlement(Long id) {
        settlementRepository.deleteById(id);
    }

    public List<EmployeeSettlement> getAllSettlements() {
        return settlementRepository.findAll();
    }

    public List<EmployeeSettlement> getSettlementsByEmployee(Long employeeId) {
        return settlementRepository.findByEmployeeId(employeeId);
    }
}
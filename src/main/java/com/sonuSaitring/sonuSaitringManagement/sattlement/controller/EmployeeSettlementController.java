package com.sonuSaitring.sonuSaitringManagement.sattlement.controller;

import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.EmployeeSettlementDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.SettlementResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.service.EmployeeSettlementServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
public class EmployeeSettlementController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeSettlementController.class);

    @Autowired
    private EmployeeSettlementServiceImpl settlementService;

    @PostMapping
    public ResponseEntity<SettlementResponseDTO> createSettlement(
            @RequestBody EmployeeSettlementDTO dto) {

        logger.info(
                "Settlement creation request received: employeeId={}, amount={}",
                dto.getEmployeeId(),
                dto.getAmountPaid());

        ResponseEntity<SettlementResponseDTO> response = ResponseEntity.ok(
                settlementService.saveSettlement(dto));

        logger.info(
                "Settlement creation request completed: employeeId={}",
                dto.getEmployeeId());

        return response;
    }

    @PutMapping("/{id}")
    public ResponseEntity<SettlementResponseDTO> updateSettlement(
            @PathVariable Long id,
            @RequestBody EmployeeSettlementDTO dto) {

        logger.info(
                "Settlement update request received: settlementId={}, employeeId={}",
                id,
                dto.getEmployeeId());

        ResponseEntity<SettlementResponseDTO> response = ResponseEntity.ok(
                settlementService.updateSettlement(id, dto));

        logger.info(
                "Settlement update request completed: settlementId={}",
                id);

        return response;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSettlement(
            @PathVariable Long id) {

        logger.info(
                "Settlement deletion request received: settlementId={}",
                id);

        settlementService.deleteSettlement(id);

        logger.info(
                "Settlement deletion request completed: settlementId={}",
                id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SettlementResponseDTO>> getAllSettlements() {

        logger.info("All settlements request received");

        ResponseEntity<List<SettlementResponseDTO>> response = ResponseEntity.ok(
                settlementService.getAllSettlements());

        logger.info("All settlements request completed");

        return response;
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<SettlementResponseDTO>> getSettlementsByEmployee(
            @PathVariable Long employeeId) {

        logger.info(
                "Employee settlements request received: employeeId={}",
                employeeId);

        ResponseEntity<List<SettlementResponseDTO>> response = ResponseEntity.ok(
                settlementService.getSettlementsByEmployee(employeeId));

        logger.info(
                "Employee settlements request completed: employeeId={}",
                employeeId);

        return response;
    }
}
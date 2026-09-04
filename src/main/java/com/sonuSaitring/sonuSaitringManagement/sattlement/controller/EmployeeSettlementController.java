package com.sonuSaitring.sonuSaitringManagement.sattlement.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.EmployeeSettlementDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.service.EmployeeSettlementServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/settlements")
public class EmployeeSettlementController {

    @Autowired
    private EmployeeSettlementServiceImpl settlementService;

    @PostMapping
    public ResponseEntity<EmployeeSettlement> createSettlement(@RequestBody EmployeeSettlementDTO dto) {
        return ResponseEntity.ok(settlementService.saveSettlement(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeSettlement> updateSettlement(@PathVariable Long id,
            @RequestBody EmployeeSettlementDTO dto) {
        return ResponseEntity.ok(settlementService.updateSettlement(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSettlement(@PathVariable Long id) {
        settlementService.deleteSettlement(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<EmployeeSettlement>> getAllSettlements() {
        return ResponseEntity.ok(settlementService.getAllSettlements());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeSettlement>> getSettlementsByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(settlementService.getSettlementsByEmployee(employeeId));
    }
}
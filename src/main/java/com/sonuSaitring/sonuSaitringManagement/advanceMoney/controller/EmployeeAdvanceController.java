package com.sonuSaitring.sonuSaitringManagement.advanceMoney.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.service.EmployeeAdvanceService;

@RestController
@RequestMapping("/api/advances")
public class EmployeeAdvanceController {

    private final EmployeeAdvanceService advanceService;

    public EmployeeAdvanceController(
            EmployeeAdvanceService advanceService) {

        this.advanceService = advanceService;
    }

    @PostMapping
    public ResponseEntity<AdvanceResponseDTO> recordAdvance(
            @RequestBody AdvanceRequestDTO requestDTO) {

        return ResponseEntity.ok(
                advanceService.recordAdvance(requestDTO));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<AdvanceResponseDTO>> getMonthlyAdvances(
            @RequestParam int year,
            @RequestParam int month) {

        return ResponseEntity.ok(
                advanceService.getMonthlyAdvances(
                        year,
                        month));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdvanceResponseDTO> updateAdvance(
            @PathVariable Long id,
            @RequestBody AdvanceRequestDTO requestDTO) {

        return ResponseEntity.ok(
                advanceService.updateAdvance(
                        id,
                        requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdvance(
            @PathVariable Long id) {

        advanceService.deleteAdvance(id);

        return ResponseEntity.ok().build();
    }
}
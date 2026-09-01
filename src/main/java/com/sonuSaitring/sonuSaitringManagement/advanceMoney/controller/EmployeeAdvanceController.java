package com.sonuSaitring.sonuSaitringManagement.advanceMoney.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.service.EmployeeAdvanceService;

import java.util.List;

@RestController
@RequestMapping("/api/advances")

public class EmployeeAdvanceController {

    @Autowired
    private EmployeeAdvanceService advanceService;

    @PostMapping
    public ResponseEntity<EmployeeAdvance> recordAdvance(@RequestBody AdvanceRequestDTO requestDTO) {
        return ResponseEntity.ok(advanceService.recordAdvance(requestDTO));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<EmployeeAdvance>> getMonthlyAdvances(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(advanceService.getMonthlyAdvances(year, month));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdvance(@PathVariable Long id) {
        advanceService.deleteAdvance(id);
        return ResponseEntity.ok().build();
    }
    @PutMapping("/{id}")
public ResponseEntity<EmployeeAdvance> updateAdvance(
        @PathVariable Long id, @RequestBody AdvanceRequestDTO requestDTO) {
    return ResponseEntity.ok(advanceService.updateAdvance(id, requestDTO));
}
}
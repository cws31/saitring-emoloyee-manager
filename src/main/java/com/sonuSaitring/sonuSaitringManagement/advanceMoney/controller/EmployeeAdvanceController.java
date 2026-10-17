package com.sonuSaitring.sonuSaitringManagement.advanceMoney.controller;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.service.EmployeeAdvanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/advances")
public class EmployeeAdvanceController {
        private final EmployeeAdvanceService service;

        public EmployeeAdvanceController(EmployeeAdvanceService service) {
                this.service = service;
        }

        @PostMapping
        public ResponseEntity<AdvanceResponseDTO> create(@Valid @RequestBody AdvanceRequestDTO request) {
                return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
        }

        @GetMapping("/monthly")
        public List<AdvanceResponseDTO> list(
                        @RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                        @RequestParam(required = false) Long employeeId,
                        @RequestParam(required = false) LocalDate fromDate,
                        @RequestParam(required = false) LocalDate toDate) {
                return service.list(year, month, employeeId, fromDate, toDate);
        }

        @GetMapping("/{id}")
        public AdvanceResponseDTO get(@PathVariable Long id) {
                return service.get(id);
        }

        @PutMapping("/{id}")
        public AdvanceResponseDTO update(@PathVariable Long id, @Valid @RequestBody AdvanceRequestDTO request) {
                return service.update(id, request);
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(required = false) String reason) {
                service.delete(id, reason);
                return ResponseEntity.noContent().build();
        }
}

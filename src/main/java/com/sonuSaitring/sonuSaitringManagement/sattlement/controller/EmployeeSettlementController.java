package com.sonuSaitring.sonuSaitringManagement.sattlement.controller;

import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.SettlementRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.SettlementResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.service.EmployeeSettlementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/settlements")
public class EmployeeSettlementController {
        private final EmployeeSettlementService service;

        public EmployeeSettlementController(EmployeeSettlementService service) {
                this.service = service;
        }

        @PostMapping
        public ResponseEntity<SettlementResponseDTO> create(@Valid @RequestBody SettlementRequestDTO request) {
                return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
        }

        @GetMapping
        public List<SettlementResponseDTO> list(@RequestParam(required = false) Integer year,
                        @RequestParam(required = false) Integer month, @RequestParam(required = false) Long employeeId,
                        @RequestParam(required = false) LocalDate fromDate,
                        @RequestParam(required = false) LocalDate toDate) {
                return service.list(year, month, employeeId, fromDate, toDate);
        }

        @GetMapping("/employee/{employeeId}")
        public List<SettlementResponseDTO> byEmployee(@PathVariable Long employeeId) {
                return service.list(null, null, employeeId, null, null);
        }

        @GetMapping("/{id}")
        public SettlementResponseDTO get(@PathVariable Long id) {
                return service.get(id);
        }

        @PutMapping("/{id}")
        public SettlementResponseDTO update(@PathVariable Long id, @Valid @RequestBody SettlementRequestDTO request) {
                return service.update(id, request);
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(required = false) String reason) {
                service.delete(id, reason);
                return ResponseEntity.noContent().build();
        }
}

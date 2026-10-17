package com.sonuSaitring.sonuSaitringManagement.Hisab.controller;

import com.sonuSaitring.sonuSaitringManagement.Hisab.dto.CreateHisabRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Hisab.dto.HisabResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Hisab.dto.OpeningBalanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Hisab.dto.RateCorrectionRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Hisab.service.HisabService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/month-closings")
public class HisabController {
        private final HisabService service;

        public HisabController(HisabService service) {
                this.service = service;
        }

        @PostMapping
        public org.springframework.http.ResponseEntity<HisabResponseDTO> createOrGet(
                        @Valid @RequestBody CreateHisabRequestDTO request) {
                return org.springframework.http.ResponseEntity.status(HttpStatus.OK)
                                .body(service.getMonthlyHisab(request.getYear(), request.getMonth()));
        }

        @GetMapping
        public HisabResponseDTO getMonthly(@RequestParam int year, @RequestParam int month) {
                return service.getMonthlyHisab(year, month);
        }

        @GetMapping("/{id}")
        public HisabResponseDTO get(@PathVariable Long id) {
                return service.getHisabById(id);
        }

        @PostMapping("/{id}/close")
        public HisabResponseDTO close(@PathVariable Long id) {
                return service.closeMonth(id);
        }

        @GetMapping("/{id}/employees/{employeeId}")
        public HisabResponseDTO getEmployee(@PathVariable Long id, @PathVariable Long employeeId) {
                HisabResponseDTO hisab = service.getHisabById(id);
                return new HisabResponseDTO(hisab.getId(), hisab.getYear(), hisab.getMonth(), hisab.getStatus(),
                                hisab.getClosedAt(),
                                hisab.getTotalEmployees(), hisab.getTotalPayable(), hisab.getTotalOverAdvance(),
                                hisab.getEmployees().stream().filter(e -> e.getEmployeeId().equals(employeeId))
                                                .toList());
        }

        @GetMapping("/{id}/employees/{employeeId}/transactions")
        public Map<String, Object> transactions(@PathVariable Long id, @PathVariable Long employeeId) {
                return service.getEmployeeTransactions(id, employeeId);
        }

        @PutMapping("/{id}/employees/{employeeId}/opening-balance")
        public HisabResponseDTO openingBalance(@PathVariable Long id, @PathVariable Long employeeId,
                        @Valid @RequestBody OpeningBalanceRequestDTO request) {
                HisabResponseDTO hisab = service.getHisabById(id);
                service.setOpeningBalance(employeeId, hisab.getYear(), hisab.getMonth(), request.getOpeningBalance(),
                                request.getReason());
                return service.getHisabById(id);
        }

        @PutMapping("/{id}/employees/{employeeId}/rate")
        public HisabResponseDTO correctRate(@PathVariable Long id, @PathVariable Long employeeId,
                        @Valid @RequestBody RateCorrectionRequestDTO request) {
                HisabResponseDTO hisab = service.getHisabById(id);
                service.correctRate(employeeId, hisab.getYear(), hisab.getMonth(), request.getDailyRate(),
                                request.getReason());
                return service.getHisabById(id);
        }
}

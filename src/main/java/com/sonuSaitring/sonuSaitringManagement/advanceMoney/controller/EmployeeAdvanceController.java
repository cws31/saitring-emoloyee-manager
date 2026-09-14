package com.sonuSaitring.sonuSaitringManagement.advanceMoney.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.service.EmployeeAdvanceService;

@RestController
@RequestMapping("/api/advances")
public class EmployeeAdvanceController {

        private static final Logger logger = LoggerFactory.getLogger(EmployeeAdvanceController.class);

        private final EmployeeAdvanceService advanceService;

        public EmployeeAdvanceController(
                        EmployeeAdvanceService advanceService) {

                this.advanceService = advanceService;
        }

        @PostMapping
        public ResponseEntity<AdvanceResponseDTO> recordAdvance(
                        @RequestBody AdvanceRequestDTO requestDTO) {

                logger.info(
                                "Advance record request received: employeeId={}, amount={}",
                                requestDTO.getEmployeeId(),
                                requestDTO.getAmount());

                ResponseEntity<AdvanceResponseDTO> response = ResponseEntity.ok(
                                advanceService.recordAdvance(requestDTO));

                logger.info(
                                "Advance record request completed: employeeId={}",
                                requestDTO.getEmployeeId());

                return response;
        }

        @GetMapping("/monthly")
        public ResponseEntity<List<AdvanceResponseDTO>> getMonthlyAdvances(
                        @RequestParam int year,
                        @RequestParam int month) {

                logger.info(
                                "Monthly advances request received: year={}, month={}",
                                year,
                                month);

                ResponseEntity<List<AdvanceResponseDTO>> response = ResponseEntity.ok(
                                advanceService.getMonthlyAdvances(
                                                year,
                                                month));

                logger.info(
                                "Monthly advances request completed: year={}, month={}",
                                year,
                                month);

                return response;
        }

        @PutMapping("/{id}")
        public ResponseEntity<AdvanceResponseDTO> updateAdvance(
                        @PathVariable Long id,
                        @RequestBody AdvanceRequestDTO requestDTO) {

                logger.info(
                                "Advance update request received: id={}, employeeId={}",
                                id,
                                requestDTO.getEmployeeId());

                ResponseEntity<AdvanceResponseDTO> response = ResponseEntity.ok(
                                advanceService.updateAdvance(
                                                id,
                                                requestDTO));

                logger.info(
                                "Advance update request completed: id={}",
                                id);

                return response;
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteAdvance(
                        @PathVariable Long id) {

                logger.info(
                                "Advance delete request received: id={}",
                                id);

                advanceService.deleteAdvance(id);

                logger.info(
                                "Advance delete request completed: id={}",
                                id);

                return ResponseEntity.ok().build();
        }
}
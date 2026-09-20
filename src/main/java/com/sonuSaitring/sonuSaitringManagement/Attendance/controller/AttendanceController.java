package com.sonuSaitring.sonuSaitringManagement.Attendance.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceSummaryDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.BulkAttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.service.AttendanceService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    public ResponseEntity<AttendanceResponseDTO> markAttendance(
            @Valid @RequestBody AttendanceRequestDTO requestDTO) {

        logger.info(
                "Attendance request received: employeeId={}, date={}, status={}",
                requestDTO.getEmployeeId(),
                requestDTO.getAttendanceDate(),
                requestDTO.getStatus());

        AttendanceResponseDTO saved = attendanceService.markAttendance(requestDTO);

        logger.info(
                "Attendance request completed: employeeId={}, date={}",
                requestDTO.getEmployeeId(),
                requestDTO.getAttendanceDate());

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<AttendanceResponseDTO>> markBulkAttendance(
            @Valid @RequestBody BulkAttendanceRequestDTO requestDTO) {

        logger.info(
                "Bulk attendance request received: date={}, status={}",
                requestDTO.getAttendanceDate(),
                requestDTO.getStatus());

        List<AttendanceResponseDTO> savedList = attendanceService.markBulkAttendance(requestDTO);

        logger.info(
                "Bulk attendance request completed: date={}, records={}",
                requestDTO.getAttendanceDate(),
                savedList.size());

        return ResponseEntity.ok(savedList);
    }

    @GetMapping("/month")
    public ResponseEntity<List<AttendanceResponseDTO>> getMonthlyAttendance(
            @RequestParam int year,
            @RequestParam int month) {

        logger.info(
                "Monthly attendance request received: year={}, month={}",
                year,
                month);

        List<AttendanceResponseDTO> list = attendanceService.getMonthlyAttendance(year, month);

        logger.info(
                "Monthly attendance request completed: year={}, month={}, records={}",
                year,
                month,
                list.size());

        return ResponseEntity.ok(list);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceResponseDTO>> getEmployeeMonthlyAttendance(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month) {

        logger.info(
                "Employee attendance request received: employeeId={}, year={}, month={}",
                employeeId,
                year,
                month);

        List<AttendanceResponseDTO> list = attendanceService.getEmployeeMonthlyAttendance(
                employeeId,
                year,
                month);

        logger.info(
                "Employee attendance request completed: employeeId={}, year={}, month={}, records={}",
                employeeId,
                year,
                month,
                list.size());

        return ResponseEntity.ok(list);
    }

    @GetMapping("/summary/day")
    public ResponseEntity<AttendanceSummaryDTO> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        logger.info("Daily attendance summary request received: date={}", date);

        AttendanceSummaryDTO summary = attendanceService.getDailyAttendanceSummary(date);

        logger.info("Daily attendance summary request completed: date={}", date);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/summary/month")
    public ResponseEntity<List<AttendanceSummaryDTO>> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {

        logger.info("Monthly attendance summary request received: year={}, month={}", year, month);

        List<AttendanceSummaryDTO> summaries = attendanceService.getMonthlyAttendanceSummary(year, month);

        logger.info("Monthly attendance summary request completed: year={}, month={}, records={}", year, month, summaries.size());

        return ResponseEntity.ok(summaries);
    }
}
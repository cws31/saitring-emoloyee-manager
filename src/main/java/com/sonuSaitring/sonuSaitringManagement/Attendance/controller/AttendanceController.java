package com.sonuSaitring.sonuSaitringManagement.Attendance.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.service.AttendanceService;

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
}
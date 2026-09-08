package com.sonuSaitring.sonuSaitringManagement.Attendance.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.service.AttendanceService;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<AttendanceResponseDTO> markAttendance(@Valid @RequestBody AttendanceRequestDTO requestDTO) {
        AttendanceResponseDTO saved = attendanceService.markAttendance(requestDTO);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/month")
    public ResponseEntity<List<AttendanceResponseDTO>> getMonthlyAttendance(
            @RequestParam int year, @RequestParam int month) {
        List<AttendanceResponseDTO> list = attendanceService.getMonthlyAttendance(year, month);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceResponseDTO>> getEmployeeMonthlyAttendance(
            @PathVariable Long employeeId, @RequestParam int year, @RequestParam int month) {
        List<AttendanceResponseDTO> list = attendanceService.getEmployeeMonthlyAttendance(employeeId, year, month);
        return ResponseEntity.ok(list);
    }
}
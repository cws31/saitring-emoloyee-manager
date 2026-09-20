package com.sonuSaitring.sonuSaitringManagement.Attendance.service;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceSummaryDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.BulkAttendanceRequestDTO;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceResponseDTO markAttendance(AttendanceRequestDTO requestDTO);

    List<AttendanceResponseDTO> getMonthlyAttendance(int year, int month);

    List<AttendanceResponseDTO> getEmployeeMonthlyAttendance(Long employeeId, int year, int month);

    List<AttendanceResponseDTO> markBulkAttendance(BulkAttendanceRequestDTO requestDTO);

    AttendanceSummaryDTO getDailyAttendanceSummary(LocalDate date);

    List<AttendanceSummaryDTO> getMonthlyAttendanceSummary(int year, int month);
}
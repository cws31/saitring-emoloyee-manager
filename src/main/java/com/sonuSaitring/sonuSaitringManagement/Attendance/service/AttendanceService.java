package com.sonuSaitring.sonuSaitringManagement.Attendance.service;



import java.time.LocalDate;
import java.util.List;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;

public interface AttendanceService {
    Attendance markAttendance(AttendanceRequestDTO requestDTO);
    List<Attendance> getMonthlyAttendance(int year, int month);
    List<Attendance> getEmployeeMonthlyAttendance(Long employeeId, int year, int month);
}
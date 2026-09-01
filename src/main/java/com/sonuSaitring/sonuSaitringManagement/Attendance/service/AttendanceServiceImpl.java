package com.sonuSaitring.sonuSaitringManagement.Attendance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Attendance markAttendance(AttendanceRequestDTO requestDTO) {
        Employee employee = employeeRepository.findById(requestDTO.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + requestDTO.getEmployeeId()));

        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndAttendanceDate(
                requestDTO.getEmployeeId(), requestDTO.getAttendanceDate());

        Attendance attendance;
        if (existing.isPresent()) {
            attendance = existing.get();
            attendance.setStatus(requestDTO.getStatus());
            attendance.setReason(requestDTO.getReason());
        } else {
            attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setAttendanceDate(requestDTO.getAttendanceDate());
            attendance.setStatus(requestDTO.getStatus());
            attendance.setReason(requestDTO.getReason());
        }

        return attendanceRepository.save(attendance);
    }

    @Override
    public List<Attendance> getMonthlyAttendance(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return attendanceRepository.findAllByMonth(startDate, endDate);
    }

    @Override
    public List<Attendance> getEmployeeMonthlyAttendance(Long employeeId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return attendanceRepository.findByEmployeeIdAndMonth(employeeId, startDate, endDate);
    }
}
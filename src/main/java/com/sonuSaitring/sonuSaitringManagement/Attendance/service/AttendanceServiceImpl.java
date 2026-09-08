package com.sonuSaitring.sonuSaitringManagement.Attendance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public AttendanceResponseDTO markAttendance(AttendanceRequestDTO requestDTO) {
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

        Attendance savedAttendance = attendanceRepository.save(attendance);
        return mapToResponseDTO(savedAttendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getMonthlyAttendance(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return attendanceRepository.findAllByMonth(startDate, endDate).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getEmployeeMonthlyAttendance(Long employeeId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return attendanceRepository.findByEmployeeIdAndMonth(employeeId, startDate, endDate).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private AttendanceResponseDTO mapToResponseDTO(Attendance attendance) {
        Employee emp = attendance.getEmployee();
        return AttendanceResponseDTO.builder()
                .id(attendance.getId())
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .reason(attendance.getReason())
                .createdAt(attendance.getCreatedAt())
                .employeeId(emp != null ? emp.getId() : null)
                .employeeName(emp != null ? emp.getName() : null)
                .employeeMobile(emp != null ? emp.getMobile() : null)
                .build();
    }
}
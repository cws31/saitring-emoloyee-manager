package com.sonuSaitring.sonuSaitringManagement.Attendance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(AttendanceServiceImpl.class);

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CurrentOwnerService currentOwnerService;

    @Override
    public AttendanceResponseDTO markAttendance(AttendanceRequestDTO requestDTO) {

        Long ownerId = currentOwnerService.getCurrentOwnerId();

        logger.info(
                "Marking attendance for ownerId: {}, employeeId: {}, date: {}",
                ownerId,
                requestDTO.getEmployeeId(),
                requestDTO.getAttendanceDate());

        Employee employee = employeeRepository
                .findByIdAndOwnerId(requestDTO.getEmployeeId(), ownerId)
                .orElseThrow(() -> {
                    logger.error(
                            "Employee {} does not belong to owner {}",
                            requestDTO.getEmployeeId(),
                            ownerId);

                    return new RuntimeException(
                            "Employee not found.");
                });

        Optional<Attendance> existing = attendanceRepository
                .findByOwnerIdAndEmployeeIdAndAttendanceDate(
                        ownerId,
                        requestDTO.getEmployeeId(),
                        requestDTO.getAttendanceDate());

        Attendance attendance;

        if (existing.isPresent()) {

            attendance = existing.get();

            attendance.setStatus(requestDTO.getStatus());
            attendance.setReason(requestDTO.getReason());

        } else {

            attendance = new Attendance();

            attendance.setEmployee(employee);
            attendance.setOwner(currentOwnerService.getCurrentOwner());
            attendance.setAttendanceDate(requestDTO.getAttendanceDate());
            attendance.setStatus(requestDTO.getStatus());
            attendance.setReason(requestDTO.getReason());
        }

        Attendance savedAttendance = attendanceRepository.save(attendance);

        return mapToResponseDTO(savedAttendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getMonthlyAttendance(
            int year,
            int month) {

        Long ownerId = currentOwnerService.getCurrentOwnerId();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return attendanceRepository
                .findAllByOwnerIdAndMonth(
                        ownerId,
                        startDate,
                        endDate)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponseDTO> getEmployeeMonthlyAttendance(
            Long employeeId,
            int year,
            int month) {

        Long ownerId = currentOwnerService.getCurrentOwnerId();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        employeeRepository
                .findByIdAndOwnerId(employeeId, ownerId)
                .orElseThrow(() -> new RuntimeException("Employee not found."));

        return attendanceRepository
                .findByOwnerIdAndEmployeeIdAndMonth(
                        ownerId,
                        employeeId,
                        startDate,
                        endDate)
                .stream()
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
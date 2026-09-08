package com.sonuSaitring.sonuSaitringManagement.Attendance.dto;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDTO {
    private Long id;
    private LocalDate attendanceDate;
    private Attendance.AttendanceStatus status;
    private String reason;
    private LocalDateTime createdAt;
    private Long employeeId;
    private String employeeName;
    private String employeeMobile;
}
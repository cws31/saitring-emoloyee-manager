package com.sonuSaitring.sonuSaitringManagement.Attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;

@Data
public class AttendanceRequestDTO {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotNull(message = "Attendance status is required")
    private Attendance.AttendanceStatus status;

    private String reason;
}
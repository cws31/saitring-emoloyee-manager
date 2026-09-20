package com.sonuSaitring.sonuSaitringManagement.Attendance.dto;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BulkAttendanceRequestDTO {

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotNull(message = "Attendance status is required")
    private Attendance.AttendanceStatus status;

    private String reason;
}
package com.sonuSaitring.sonuSaitringManagement.Attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryDTO {
    private LocalDate attendanceDate;
    private long present;
    private long absent;
    private long halfPresent;
}
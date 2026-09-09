package com.sonuSaitring.sonuSaitringManagement.dashboard.dto;

import java.math.BigDecimal;

public record AttendanceSummary(
        long present,
        long halfDay,
        long absent,
        long totalMarked,
        BigDecimal attendanceRate) {
}
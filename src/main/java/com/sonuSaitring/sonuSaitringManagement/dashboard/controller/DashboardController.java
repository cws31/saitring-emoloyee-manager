package com.sonuSaitring.sonuSaitringManagement.dashboard.controller;

import com.sonuSaitring.sonuSaitringManagement.dashboard.dto.DashboardResponse;
import com.sonuSaitring.sonuSaitringManagement.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        LocalDate now = LocalDate.now();

        int requestedYear = year != null ? year : now.getYear();

        int requestedMonth = month != null ? month : now.getMonthValue();

        return ResponseEntity.ok(
                dashboardService.getDashboard(
                        requestedYear,
                        requestedMonth));
    }
}   
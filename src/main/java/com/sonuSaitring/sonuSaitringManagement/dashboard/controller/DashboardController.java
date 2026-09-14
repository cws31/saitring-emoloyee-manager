package com.sonuSaitring.sonuSaitringManagement.dashboard.controller;

import com.sonuSaitring.sonuSaitringManagement.dashboard.dto.DashboardResponse;
import com.sonuSaitring.sonuSaitringManagement.dashboard.service.DashboardService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService) {

        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam int year,
            @RequestParam int month) {

        return ResponseEntity.ok(
                dashboardService.getDashboard(
                        year,
                        month));
    }
}
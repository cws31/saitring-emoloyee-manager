package com.sonuSaitring.sonuSaitringManagement.dashboard.controller;

import com.sonuSaitring.sonuSaitringManagement.dashboard.dto.DashboardResponse;
import com.sonuSaitring.sonuSaitringManagement.dashboard.service.DashboardService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService) {

        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam int year,
            @RequestParam int month) {

        logger.info(
                "Dashboard request received: year={}, month={}",
                year,
                month);

        ResponseEntity<DashboardResponse> response = ResponseEntity.ok(
                dashboardService.getDashboard(
                        year,
                        month));

        logger.info(
                "Dashboard request completed: year={}, month={}",
                year,
                month);

        return response;
    }
}
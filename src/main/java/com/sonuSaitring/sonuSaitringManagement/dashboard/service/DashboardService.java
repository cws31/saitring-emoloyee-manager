package com.sonuSaitring.sonuSaitringManagement.dashboard.service;

import com.sonuSaitring.sonuSaitringManagement.dashboard.dto.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboard(int year, int month);
}
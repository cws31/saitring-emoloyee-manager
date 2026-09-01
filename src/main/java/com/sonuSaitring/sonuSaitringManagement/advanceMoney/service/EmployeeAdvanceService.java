package com.sonuSaitring.sonuSaitringManagement.advanceMoney.service;

import java.util.List;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;

public interface EmployeeAdvanceService {
    EmployeeAdvance recordAdvance(AdvanceRequestDTO requestDTO);

    EmployeeAdvance updateAdvance(Long id, AdvanceRequestDTO requestDTO);

    List<EmployeeAdvance> getMonthlyAdvances(int year, int month);

    void deleteAdvance(Long id);
}
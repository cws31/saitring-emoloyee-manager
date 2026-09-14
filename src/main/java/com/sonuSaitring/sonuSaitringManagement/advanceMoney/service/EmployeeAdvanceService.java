package com.sonuSaitring.sonuSaitringManagement.advanceMoney.service;

import java.util.List;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceResponseDTO;

public interface EmployeeAdvanceService {

    AdvanceResponseDTO recordAdvance(
            AdvanceRequestDTO requestDTO);

    List<AdvanceResponseDTO> getMonthlyAdvances(
            int year,
            int month);

    AdvanceResponseDTO updateAdvance(
            Long id,
            AdvanceRequestDTO requestDTO);

    void deleteAdvance(Long id);
}
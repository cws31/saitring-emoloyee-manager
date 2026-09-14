package com.sonuSaitring.sonuSaitringManagement.sattlement.service;

import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.EmployeeSettlementDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.SettlementResponseDTO;

import java.util.List;

public interface EmployeeSettlementService {

        SettlementResponseDTO saveSettlement(
                        EmployeeSettlementDTO dto);

        SettlementResponseDTO updateSettlement(
                        Long id,
                        EmployeeSettlementDTO dto);

        void deleteSettlement(Long id);

        List<SettlementResponseDTO> getAllSettlements();

        List<SettlementResponseDTO> getSettlementsByEmployee(
                        Long employeeId);
}
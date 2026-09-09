package com.sonuSaitring.sonuSaitringManagement.sattlement.service;

import java.util.List;

import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.EmployeeSettlementDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;

public interface EmployeeSettlementService {

    EmployeeSettlement saveSettlement(EmployeeSettlementDTO dto);

    EmployeeSettlement updateSettlement(
            Long id,
            EmployeeSettlementDTO dto);

    void deleteSettlement(Long id);

    List<EmployeeSettlement> getAllSettlements();

    List<EmployeeSettlement> getSettlementsByEmployee(
            Long employeeId);
}
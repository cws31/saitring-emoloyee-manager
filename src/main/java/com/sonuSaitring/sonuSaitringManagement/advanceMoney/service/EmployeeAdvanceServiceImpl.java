package com.sonuSaitring.sonuSaitringManagement.advanceMoney.service;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeAdvanceServiceImpl implements EmployeeAdvanceService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeAdvanceServiceImpl.class);

    @Autowired
    private EmployeeAdvanceRepository advanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public EmployeeAdvance recordAdvance(
            AdvanceRequestDTO requestDTO) {

        logger.info(
                "Recording advance amount {} for employeeId: {}",
                requestDTO.getAmount(),
                requestDTO.getEmployeeId());

        Employee employee = employeeRepository.findById(
                requestDTO.getEmployeeId()).orElseThrow(() -> {

                    logger.warn(
                            "Employee not found with id: {} while recording advance",
                            requestDTO.getEmployeeId());

                    return new ResourceNotFoundException(
                            "Employee not found with id: "
                                    + requestDTO.getEmployeeId());
                });

        EmployeeAdvance advance = new EmployeeAdvance();

        advance.setEmployee(employee);
        advance.setAmount(requestDTO.getAmount());

        advance.setPaymentDate(
                requestDTO.getPaymentDate() != null
                        ? requestDTO.getPaymentDate()
                        : LocalDate.now());

        advance.setNote(requestDTO.getNote());

        return advanceRepository.save(advance);
    }

    @Override
    public EmployeeAdvance updateAdvance(
            Long id,
            AdvanceRequestDTO requestDTO) {

        logger.info(
                "Updating advance record ID: {}",
                id);

        EmployeeAdvance existingAdvance = advanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Advance record not found with id: "
                                + id));

        Employee employee = employeeRepository.findById(
                requestDTO.getEmployeeId()).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Employee not found with id: "
                                        + requestDTO.getEmployeeId()));

        existingAdvance.setEmployee(employee);
        existingAdvance.setAmount(requestDTO.getAmount());

        if (requestDTO.getPaymentDate() != null) {
            existingAdvance.setPaymentDate(
                    requestDTO.getPaymentDate());
        }

        existingAdvance.setNote(requestDTO.getNote());

        return advanceRepository.save(existingAdvance);
    }

    @Override
    public List<EmployeeAdvance> getMonthlyAdvances(
            int year,
            int month) {

        logger.info(
                "Fetching monthly advances for Year: {}, Month: {}",
                year,
                month);

        LocalDate startDate = LocalDate.of(year, month, 1);

        LocalDate endDate = startDate.withDayOfMonth(
                startDate.lengthOfMonth());

        return advanceRepository.findByPaymentDateBetween(
                startDate,
                endDate);
    }

    @Override
    public void deleteAdvance(Long id) {

        logger.info(
                "Deleting advance record ID: {}",
                id);

        if (!advanceRepository.existsById(id)) {

            throw new ResourceNotFoundException(
                    "Advance record not found with id: " + id);
        }

        advanceRepository.deleteById(id);
    }
}
package com.sonuSaitring.sonuSaitringManagement.advanceMoney.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmployeeAdvanceServiceImpl
                implements EmployeeAdvanceService {

        private final EmployeeAdvanceRepository advanceRepository;
        private final EmployeeRepository employeeRepository;
        private final CurrentOwnerService currentOwnerService;

        public EmployeeAdvanceServiceImpl(
                        EmployeeAdvanceRepository advanceRepository,
                        EmployeeRepository employeeRepository,
                        CurrentOwnerService currentOwnerService) {

                this.advanceRepository = advanceRepository;
                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;
        }


        @Override
        @Transactional
        public AdvanceResponseDTO recordAdvance(
                        AdvanceRequestDTO requestDTO) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                log.info(
                                "Recording advance amount {} for employeeId: {} and ownerId: {}",
                                requestDTO.getAmount(),
                                requestDTO.getEmployeeId(),
                                ownerId);

                Employee employee = employeeRepository
                                .findByIdAndOwnerId(
                                                requestDTO.getEmployeeId(),
                                                ownerId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Employee not found"));

                EmployeeAdvance advance = new EmployeeAdvance();

                advance.setEmployee(employee);
                advance.setOwner(
                                currentOwnerService.getCurrentOwner());

                advance.setAmount(requestDTO.getAmount());

                advance.setPaymentDate(
                                requestDTO.getPaymentDate() != null
                                                ? requestDTO.getPaymentDate()
                                                : LocalDate.now());

                advance.setNote(requestDTO.getNote());

                EmployeeAdvance savedAdvance = advanceRepository.save(advance);

                return mapToResponseDTO(savedAdvance);
        }


        @Override
        @Transactional(readOnly = true)
        public List<AdvanceResponseDTO> getMonthlyAdvances(
                        int year,
                        int month) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                log.info(
                                "Fetching monthly advances for Year: {}, Month: {}, ownerId: {}",
                                year,
                                month,
                                ownerId);

                LocalDate startDate = LocalDate.of(year, month, 1);

                LocalDate endDate = startDate.withDayOfMonth(
                                startDate.lengthOfMonth());

                List<EmployeeAdvance> advances = advanceRepository
                                .findByOwnerIdAndPaymentDateBetween(
                                                ownerId,
                                                startDate,
                                                endDate);

                return advances.stream()
                                .map(this::mapToResponseDTO)
                                .toList();
        }


        @Override
        @Transactional
        public AdvanceResponseDTO updateAdvance(
                        Long id,
                        AdvanceRequestDTO requestDTO) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                log.info(
                                "Updating advance id: {} for ownerId: {}",
                                id,
                                ownerId);

                EmployeeAdvance advance = advanceRepository
                                .findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Advance not found"));

                Employee employee = employeeRepository
                                .findByIdAndOwnerId(
                                                requestDTO.getEmployeeId(),
                                                ownerId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Employee not found"));

                advance.setEmployee(employee);

                advance.setOwner(
                                currentOwnerService.getCurrentOwner());

                advance.setAmount(
                                requestDTO.getAmount());

                if (requestDTO.getPaymentDate() != null) {
                        advance.setPaymentDate(
                                        requestDTO.getPaymentDate());
                }

                advance.setNote(
                                requestDTO.getNote());

                EmployeeAdvance updatedAdvance = advanceRepository.save(advance);

                return mapToResponseDTO(updatedAdvance);
        }

   

        @Override
        @Transactional
        public void deleteAdvance(Long id) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                log.info(
                                "Deleting advance id: {} for ownerId: {}",
                                id,
                                ownerId);

                EmployeeAdvance advance = advanceRepository
                                .findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Advance not found"));

                advanceRepository.delete(advance);
        }

      

        private AdvanceResponseDTO mapToResponseDTO(
                        EmployeeAdvance advance) {

                Employee employee = advance.getEmployee();

                return new AdvanceResponseDTO(
                                advance.getId(),
                                employee.getId(),
                                employee.getName(),
                                employee.getMobile(),
                                advance.getAmount(),
                                advance.getPaymentDate(),
                                advance.getNote());
        }
}
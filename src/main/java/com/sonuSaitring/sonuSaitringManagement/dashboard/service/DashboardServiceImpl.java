package com.sonuSaitring.sonuSaitringManagement.dashboard.service;

import com.sonuSaitring.sonuSaitringManagement.Hisab.dto.EmployeeHisabDTO;
import com.sonuSaitring.sonuSaitringManagement.Hisab.dto.HisabResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Hisab.service.HisabService;
import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.dashboard.dto.DashboardResponse;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class DashboardServiceImpl implements DashboardService {
        private final EmployeeRepository employeeRepository;
        private final CurrentOwnerService currentOwnerService;
        private final HisabService hisabService;

        public DashboardServiceImpl(EmployeeRepository employeeRepository, CurrentOwnerService currentOwnerService,
                        HisabService hisabService) {
                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;
                this.hisabService = hisabService;
        }

        @Override
        public DashboardResponse getDashboard(int year, int month) {
                if (year < 2000 || year > 2200 || month < 1 || month > 12)
                        throw new BadRequestException("Invalid year or month.");
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                Set<Long> activeEmployeeIds = employeeRepository.findAllByOwnerId(ownerId).stream()
                                .filter(employee -> !employee.isBlocked()).map(Employee::getId)
                                .collect(Collectors.toSet());
                HisabResponseDTO hisab = hisabService.getMonthlyHisab(year, month);
                BigDecimal totalPayable = BigDecimal.ZERO;
                BigDecimal totalAdvance = BigDecimal.ZERO;
                BigDecimal totalOverAdvance = BigDecimal.ZERO;
                for (EmployeeHisabDTO employee : hisab.getEmployees()) {
                        if (!activeEmployeeIds.contains(employee.getEmployeeId()))
                                continue;
                        totalAdvance = totalAdvance.add(employee.getTotalAdvance());
                        int sign = employee.getClosingBalance().compareTo(BigDecimal.ZERO);
                        if (sign > 0)
                                totalPayable = totalPayable.add(employee.getClosingBalance());
                        if (sign < 0)
                                totalOverAdvance = totalOverAdvance.add(employee.getClosingBalance().abs());
                }
                return new DashboardResponse(activeEmployeeIds.size(), totalPayable, totalAdvance, totalOverAdvance);
        }
}

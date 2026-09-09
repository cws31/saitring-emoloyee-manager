package com.sonuSaitring.sonuSaitringManagement.dashboard.service;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import com.sonuSaitring.sonuSaitringManagement.Hisab.repository.HisabRepository;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.dashboard.dto.DashboardResponse;
import com.sonuSaitring.sonuSaitringManagement.dashboard.dto.EmployeeFinancialStatus;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

        private static final BigDecimal ZERO = BigDecimal.ZERO;
        private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

        private final EmployeeRepository employeeRepository;
        private final AttendanceRepository attendanceRepository;
        private final EmployeeAdvanceRepository advanceRepository;
        private final EmployeeSettlementRepository settlementRepository;
        private final HisabRepository hisabRepository;

        public DashboardServiceImpl(
                        EmployeeRepository employeeRepository,
                        AttendanceRepository attendanceRepository,
                        EmployeeAdvanceRepository advanceRepository,
                        EmployeeSettlementRepository settlementRepository,
                        HisabRepository hisabRepository) {

                this.employeeRepository = employeeRepository;
                this.attendanceRepository = attendanceRepository;
                this.advanceRepository = advanceRepository;
                this.settlementRepository = settlementRepository;
                this.hisabRepository = hisabRepository;
        }

        @Override
        public DashboardResponse getDashboard(int year, int month) {

                validatePeriod(year, month);

                LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);

                LocalDate endDate = firstDayOfMonth.withDayOfMonth(
                                firstDayOfMonth.lengthOfMonth());

                LocalDate today = LocalDate.now();

                if (year == today.getYear()
                                && month == today.getMonthValue()) {

                        endDate = today;
                }

                List<Employee> employees = employeeRepository.findAll();

                List<Employee> activeEmployees = employees.stream()
                                .filter(employee -> !employee.isBlocked())
                                .toList();

                long totalEmployees = activeEmployees.size();

                List<EmployeeFinancialStatus> employeeStatus = buildEmployeeFinancialStatus(
                                activeEmployees,
                                year,
                                month,
                                endDate);

                BigDecimal totalPayable = ZERO;
                BigDecimal totalAdvance = ZERO;
                BigDecimal totalOverAdvance = ZERO;

                for (EmployeeFinancialStatus employee : employeeStatus) {

                        BigDecimal due = safe(employee.dueAmount());
                        if (due.compareTo(ZERO) > 0) {

                                totalPayable = totalPayable.add(due);
                        }

                        if (due.compareTo(ZERO) < 0) {

                                totalOverAdvance = totalOverAdvance.add(due);
                        }

                        
                        totalAdvance = totalAdvance.add(
                                        safe(employee.advanceAmount()));
                }

                return new DashboardResponse(
                                totalEmployees,
                                money(totalPayable),
                                money(totalAdvance),
                                money(totalOverAdvance),
                                employeeStatus);
        }

       

        private List<EmployeeFinancialStatus> buildEmployeeFinancialStatus(
                        List<Employee> employees,
                        int year,
                        int month,
                        LocalDate endDate) {

                Map<Long, BigDecimal> earningsMap = new HashMap<>();

                Map<Long, BigDecimal> advanceMap = new HashMap<>();

                Map<Long, BigDecimal> paidMap = new HashMap<>();

                LocalDate startDate = LocalDate.of(year, month, 1);

               

                List<Attendance> attendances = attendanceRepository.findAllByMonth(
                                startDate,
                                endDate);

                for (Attendance attendance : attendances) {

                        Employee employee = attendance.getEmployee();

                        if (employee == null
                                        || employee.isBlocked()) {

                                continue;
                        }

                        BigDecimal presenceValue = ZERO;

                        if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {

                                presenceValue = BigDecimal.ONE;

                        } else if (attendance.getStatus() == Attendance.AttendanceStatus.HALF_DAY) {

                                presenceValue = HALF_DAY;
                        }

                        BigDecimal rate = safe(employee.getInitialRate());

                        BigDecimal earning = presenceValue.multiply(rate);

                        earningsMap.merge(
                                        employee.getId(),
                                        earning,
                                        BigDecimal::add);
                }

               
                List<EmployeeAdvance> advances = advanceRepository.findByPaymentDateBetween(
                                startDate,
                                endDate);

                for (EmployeeAdvance advance : advances) {

                        Employee employee = advance.getEmployee();

                        if (employee == null
                                        || employee.isBlocked()
                                        || advance.getAmount() == null) {

                                continue;
                        }

                       
                        String note = advance.getNote() == null
                                        ? ""
                                        : advance.getNote().toLowerCase();

                        if (note.contains("carry-forward")
                                        || note.startsWith(
                                                        "previous month balance carry-forward")) {

                                continue;
                        }

                        BigDecimal amount = BigDecimal.valueOf(
                                        advance.getAmount());

                        advanceMap.merge(
                                        employee.getId(),
                                        amount,
                                        BigDecimal::add);
                }

             
                List<EmployeeSettlement> settlements = settlementRepository
                                .findBySettlementDateBetween(
                                                startDate,
                                                endDate);

                for (EmployeeSettlement settlement : settlements) {

                        Employee employee = settlement.getEmployee();

                        if (employee == null
                                        || employee.isBlocked()
                                        || settlement.getAmountPaid() == null) {

                                continue;
                        }

                        paidMap.merge(
                                        employee.getId(),
                                        settlement.getAmountPaid(),
                                        BigDecimal::add);
                }

               

                Map<Long, BigDecimal> previousBalances = getPreviousBalances(
                                year,
                                month);

               
                List<EmployeeFinancialStatus> result = new ArrayList<>();

                for (Employee employee : employees) {

                        Long employeeId = employee.getId();

                        BigDecimal earnings = earningsMap.getOrDefault(
                                        employeeId,
                                        ZERO);

                        BigDecimal advance = advanceMap.getOrDefault(
                                        employeeId,
                                        ZERO);

                        BigDecimal previousBalance = previousBalances.getOrDefault(
                                        employeeId,
                                        ZERO);

                        BigDecimal amountPaid = paidMap.getOrDefault(
                                        employeeId,
                                        ZERO);

                     
                        BigDecimal payable = earnings
                                        .subtract(advance)
                                        .add(previousBalance);

                        
                        BigDecimal due = payable.subtract(amountPaid);

                        result.add(
                                        new EmployeeFinancialStatus(
                                                        employeeId,
                                                        employee.getName(),
                                                        money(payable),
                                                        money(advance),
                                                        money(amountPaid),
                                                        money(due)));
                }

                result.sort(
                                (a, b) -> a.employeeName()
                                                .compareToIgnoreCase(
                                                                b.employeeName()));

                return result;
        }

      
        private Map<Long, BigDecimal> getPreviousBalances(
                        int year,
                        int month) {

                LocalDate current = LocalDate.of(year, month, 1);

                LocalDate previous = current.minusMonths(1);

                Hisab previousHisab = hisabRepository
                                .findByYearAndMonth(
                                                previous.getYear(),
                                                previous.getMonthValue())
                                .orElse(null);

                if (previousHisab == null
                                || previousHisab.getDetails() == null) {

                        return Map.of();
                }

                Map<Long, BigDecimal> balances = new HashMap<>();

                for (HisabDetail detail : previousHisab.getDetails()) {

                        Employee employee = detail.getEmployee();

                        if (employee == null
                                        || employee.isBlocked()
                                        || detail.getRemainingBalance() == null) {

                                continue;
                        }

                        balances.put(
                                        employee.getId(),
                                        detail.getRemainingBalance());
                }

                return balances;
        }


        private static BigDecimal safe(
                        BigDecimal value) {

                return value == null
                                ? ZERO
                                : value;
        }

        private static BigDecimal money(
                        BigDecimal value) {

                return safe(value)
                                .setScale(
                                                2,
                                                RoundingMode.HALF_UP);
        }

        private void validatePeriod(
                        int year,
                        int month) {

                if (year < 2000 || year > 2100) {

                        throw new IllegalArgumentException(
                                        "Year must be between 2000 and 2100");
                }

                if (month < 1 || month > 12) {

                        throw new IllegalArgumentException(
                                        "Month must be between 1 and 12");
                }
        }
}
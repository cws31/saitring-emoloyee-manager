package com.sonuSaitring.sonuSaitringManagement.dashboard.service;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import com.sonuSaitring.sonuSaitringManagement.Hisab.repository.HisabRepository;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.dashboard.dto.DashboardResponse;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

        private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

        private static final BigDecimal ZERO = BigDecimal.ZERO;
        private static final BigDecimal HALF_DAY = new BigDecimal("0.5");

        private final EmployeeRepository employeeRepository;
        private final AttendanceRepository attendanceRepository;
        private final EmployeeAdvanceRepository advanceRepository;
        private final EmployeeSettlementRepository settlementRepository;
        private final HisabRepository hisabRepository;
        private final CurrentOwnerService currentOwnerService;

        private final Counter dashboardSuccess;
        private final Counter dashboardFailure;

        private final Timer dashboardTimer;
        private final Timer earningsTimer;
        private final Timer advancesTimer;
        private final Timer settlementsTimer;
        private final Timer previousBalancesTimer;

        public DashboardServiceImpl(
                        EmployeeRepository employeeRepository,
                        AttendanceRepository attendanceRepository,
                        EmployeeAdvanceRepository advanceRepository,
                        EmployeeSettlementRepository settlementRepository,
                        HisabRepository hisabRepository,
                        CurrentOwnerService currentOwnerService,
                        MeterRegistry meterRegistry) {

                this.employeeRepository = employeeRepository;
                this.attendanceRepository = attendanceRepository;
                this.advanceRepository = advanceRepository;
                this.settlementRepository = settlementRepository;
                this.hisabRepository = hisabRepository;
                this.currentOwnerService = currentOwnerService;

                this.dashboardSuccess = Counter.builder("dashboard.request")
                                .tag("result", "success")
                                .description("Number of successful dashboard requests")
                                .register(meterRegistry);

                this.dashboardFailure = Counter.builder("dashboard.request")
                                .tag("result", "failure")
                                .description("Number of failed dashboard requests")
                                .register(meterRegistry);

                this.dashboardTimer = Timer.builder("dashboard.request.duration")
                                .description("Time taken to generate dashboard data")
                                .register(meterRegistry);

                this.earningsTimer = Timer.builder("dashboard.earnings.duration")
                                .description("Time taken to calculate dashboard earnings")
                                .register(meterRegistry);

                this.advancesTimer = Timer.builder("dashboard.advances.duration")
                                .description("Time taken to calculate dashboard advances")
                                .register(meterRegistry);

                this.settlementsTimer = Timer.builder("dashboard.settlements.duration")
                                .description("Time taken to calculate dashboard settlements")
                                .register(meterRegistry);

                this.previousBalancesTimer = Timer.builder("dashboard.previous_balances.duration")
                                .description("Time taken to retrieve previous dashboard balances")
                                .register(meterRegistry);
        }

        @Override
        public DashboardResponse getDashboard(int year, int month) {

                return dashboardTimer.record(() -> {

                        validatePeriod(year, month);

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Generating dashboard: ownerId={}, year={}, month={}",
                                        ownerId,
                                        year,
                                        month);

                        try {

                                LocalDate startDate = LocalDate.of(year, month, 1);

                                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                                LocalDate today = LocalDate.now();

                                if (year == today.getYear()
                                                && month == today.getMonthValue()) {

                                        endDate = today;
                                }

                                List<Employee> employees = employeeRepository.findAllByOwnerId(ownerId);

                                long totalActiveEmployees = employees.stream()
                                                .filter(employee -> !employee.isBlocked())
                                                .count();

                                logger.debug(
                                                "Dashboard employees loaded: ownerId={}, total={}, active={}",
                                                ownerId,
                                                employees.size(),
                                                totalActiveEmployees);

                                Map<Long, BigDecimal> earningsMap = calculateEarnings(
                                                ownerId,
                                                employees,
                                                startDate,
                                                endDate);

                                Map<Long, BigDecimal> advanceMap = calculateAdvances(
                                                ownerId,
                                                employees,
                                                startDate,
                                                endDate);

                                Map<Long, BigDecimal> paidMap = calculateSettlements(
                                                ownerId,
                                                employees,
                                                startDate,
                                                endDate);

                                Map<Long, BigDecimal> previousBalanceMap = getPreviousBalances(
                                                ownerId,
                                                year,
                                                month);

                                BigDecimal totalPayable = ZERO;
                                BigDecimal totalAdvance = ZERO;
                                BigDecimal totalOverAdvance = ZERO;

                                for (Employee employee : employees) {

                                        if (employee.isBlocked()) {
                                                continue;
                                        }

                                        Long employeeId = employee.getId();

                                        BigDecimal earnings = earningsMap.getOrDefault(
                                                        employeeId,
                                                        ZERO);

                                        BigDecimal advance = advanceMap.getOrDefault(
                                                        employeeId,
                                                        ZERO);

                                        BigDecimal previousBalance = previousBalanceMap.getOrDefault(
                                                        employeeId,
                                                        ZERO);

                                        BigDecimal amountPaid = paidMap.getOrDefault(
                                                        employeeId,
                                                        ZERO);

                                        BigDecimal payable = earnings
                                                        .subtract(advance)
                                                        .add(previousBalance);

                                        BigDecimal due = payable.subtract(amountPaid);

                                        if (due.compareTo(ZERO) > 0) {

                                                totalPayable = totalPayable.add(due);
                                        }

                                        if (due.compareTo(ZERO) < 0) {

                                                totalOverAdvance = totalOverAdvance.add(due.abs());
                                        }

                                        totalAdvance = totalAdvance.add(advance);
                                }

                                DashboardResponse response = new DashboardResponse(
                                                totalActiveEmployees,
                                                money(totalPayable),
                                                money(totalAdvance),
                                                money(totalOverAdvance));

                                dashboardSuccess.increment();

                                logger.info(
                                                "Dashboard generated successfully: ownerId={}, year={}, month={}, activeEmployees={}",
                                                ownerId,
                                                year,
                                                month,
                                                totalActiveEmployees);

                                logger.debug(
                                                "Dashboard financial summary: ownerId={}, totalPayable={}, totalAdvance={}, totalOverAdvance={}",
                                                ownerId,
                                                totalPayable,
                                                totalAdvance,
                                                totalOverAdvance);

                                return response;

                        } catch (RuntimeException ex) {

                                dashboardFailure.increment();

                                logger.error(
                                                "Dashboard generation failed: ownerId={}, year={}, month={}, exception={}",
                                                ownerId,
                                                year,
                                                month,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        private Map<Long, BigDecimal> calculateEarnings(
                        Long ownerId,
                        List<Employee> employees,
                        LocalDate startDate,
                        LocalDate endDate) {

                return earningsTimer.record(() -> {

                        Map<Long, BigDecimal> earningsMap = new HashMap<>();

                        List<Attendance> attendances = attendanceRepository.findAllByOwnerIdAndMonth(
                                        ownerId,
                                        startDate,
                                        endDate);

                        logger.debug(
                                        "Dashboard attendance data loaded: ownerId={}, count={}",
                                        ownerId,
                                        attendances.size());

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

                        return earningsMap;
                });
        }

        private Map<Long, BigDecimal> calculateAdvances(
                        Long ownerId,
                        List<Employee> employees,
                        LocalDate startDate,
                        LocalDate endDate) {

                return advancesTimer.record(() -> {

                        Map<Long, BigDecimal> advanceMap = new HashMap<>();

                        List<EmployeeAdvance> advances = advanceRepository
                                        .findByOwnerIdAndPaymentDateBetween(
                                                        ownerId,
                                                        startDate,
                                                        endDate);

                        logger.debug(
                                        "Dashboard advance data loaded: ownerId={}, count={}",
                                        ownerId,
                                        advances.size());

                        for (EmployeeAdvance advance : advances) {

                                Employee employee = advance.getEmployee();

                                if (employee == null
                                                || employee.isBlocked()
                                                || advance.getAmount() == null) {

                                        continue;
                                }

                                String note = advance.getNote() == null
                                                ? ""
                                                : advance.getNote()
                                                                .toLowerCase();

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

                        return advanceMap;
                });
        }

        private Map<Long, BigDecimal> calculateSettlements(
                        Long ownerId,
                        List<Employee> employees,
                        LocalDate startDate,
                        LocalDate endDate) {

                return settlementsTimer.record(() -> {

                        Map<Long, BigDecimal> paidMap = new HashMap<>();

                        List<EmployeeSettlement> settlements = settlementRepository
                                        .findByOwnerIdAndSettlementDateBetween(
                                                        ownerId,
                                                        startDate,
                                                        endDate);

                        logger.debug(
                                        "Dashboard settlement data loaded: ownerId={}, count={}",
                                        ownerId,
                                        settlements.size());

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

                        return paidMap;
                });
        }

        private Map<Long, BigDecimal> getPreviousBalances(
                        Long ownerId,
                        int year,
                        int month) {

                return previousBalancesTimer.record(() -> {

                        LocalDate current = LocalDate.of(year, month, 1);

                        LocalDate previous = current.minusMonths(1);

                        Hisab previousHisab = hisabRepository
                                        .findByOwnerIdAndYearAndMonth(
                                                        ownerId,
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
                });
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

package com.sonuSaitring.sonuSaitringManagement.Hisab.service;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.dto.EmployeeHisabDTO;
import com.sonuSaitring.sonuSaitringManagement.Hisab.dto.HisabResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import com.sonuSaitring.sonuSaitringManagement.Hisab.repository.HisabDetailRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.repository.HisabRepository;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import com.sonuSaitring.sonuSaitringManagement.owner.repository.OwnerRepository;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

@Service
@Transactional
public class HisabService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HALF = new BigDecimal("0.50");

    private final HisabRepository hisabRepository;
    private final HisabDetailRepository detailRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeAdvanceRepository advanceRepository;
    private final EmployeeSettlementRepository settlementRepository;
    private final OwnerRepository ownerRepository;
    private final CurrentOwnerService currentOwnerService;

    public HisabService(
            HisabRepository hisabRepository,
            HisabDetailRepository detailRepository,
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            EmployeeAdvanceRepository advanceRepository,
            EmployeeSettlementRepository settlementRepository,
            OwnerRepository ownerRepository,
            CurrentOwnerService currentOwnerService) {

        this.hisabRepository = hisabRepository;
        this.detailRepository = detailRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.advanceRepository = advanceRepository;
        this.settlementRepository = settlementRepository;
        this.ownerRepository = ownerRepository;
        this.currentOwnerService = currentOwnerService;
    }

    // =========================================================
    // MONTHLY HISAB
    // =========================================================

    public HisabResponseDTO getMonthlyHisab(int year, int month) {
        validatePeriod(year, month);

        Long ownerId = currentOwnerService.getCurrentOwnerId();

        Hisab hisab = getOrCreateMonth(ownerId, year, month);
        LocalDate monthEnd = YearMonth.of(year, month).atEndOfMonth();

        List<Employee> employees = employeeRepository.findAllByOwnerId(ownerId)
                .stream()
                .filter(employee -> employee.getCreatedAt() == null
                        || !employee.getCreatedAt().toLocalDate().isAfter(monthEnd))
                .toList();

        for (Employee employee : employees) {
            recalculateEmployeeFrom(
                    ownerId,
                    employee.getId(),
                    YearMonth.of(year, month).atDay(1),
                    "Monthly Hisab refreshed");
        }

        return buildResponse(ownerId, hisab.getId());
    }

    public HisabResponseDTO getHisabById(Long id) {
        Long ownerId = currentOwnerService.getCurrentOwnerId();

        Hisab hisab = hisabRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly Hisab not found."));

        return getMonthlyHisab(hisab.getYear(), hisab.getMonth());
    }

    public HisabResponseDTO closeMonth(Long id) {
        Long ownerId = currentOwnerService.getCurrentOwnerId();

        Hisab hisab = hisabRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly Hisab not found."));

        getMonthlyHisab(hisab.getYear(), hisab.getMonth());

        hisab = hisabRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly Hisab not found."));

        if (hisab.getStatus() != Hisab.Status.CLOSED) {
            hisab.setStatus(Hisab.Status.CLOSED);
            hisab.setClosedAt(LocalDateTime.now());
            hisabRepository.save(hisab);
        }

        return buildResponse(ownerId, hisab.getId());
    }

    // =========================================================
    // CREATE OR RETRIEVE EMPLOYEE MONTHLY DETAIL
    // =========================================================

    public HisabDetail getOrCreateDetail(
            Long ownerId,
            Long employeeId,
            int year,
            int month) {

        validatePeriod(year, month);

        Employee employee = employeeRepository.findByIdAndOwnerId(employeeId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

        LocalDate monthEnd = YearMonth.of(year, month).atEndOfMonth();

        if (employee.getCreatedAt() != null
                && employee.getCreatedAt().toLocalDate().isAfter(monthEnd)) {
            throw new BadRequestException(
                    "Employee did not exist during the selected month.");
        }

        Hisab hisab = getOrCreateMonth(ownerId, year, month);

        Optional<HisabDetail> existing = detailRepository.findFirstByHisabIdAndEmployeeIdOrderByIdAsc(
                hisab.getId(), employeeId);

        if (existing.isPresent()) {
            HisabDetail detail = existing.get();

            /*
             * Repair a missing or zero snapshot when the employee has a
             * positive configured rate. Existing nonzero historical rates
             * remain unchanged.
             */
            if ((detail.getRateSnapshot() == null
                    || detail.getRateSnapshot().signum() == 0)
                    && employee.getInitialRate() != null
                    && employee.getInitialRate().signum() > 0) {

                detail.setRateSnapshot(money(employee.getInitialRate()));
                detailRepository.save(detail);
            }

            return detail;
        }

        HisabDetail detail = new HisabDetail();
        detail.setHisab(hisab);
        detail.setEmployee(employee);
        detail.setEmployeeName(employee.getName());
        detail.setPresentDays(0);
        detail.setHalfDays(0);
        detail.setAttendanceUnits(ZERO);
        detail.setRateSnapshot(money(employee.getInitialRate()));
        detail.setTotalEarning(ZERO);
        detail.setTotalAdvance(ZERO);
        detail.setTotalSettlement(ZERO);

        BigDecimal openingBalance = findPreviousClosing(ownerId, employeeId, year, month);

        detail.setOpeningBalance(openingBalance);
        detail.setClosingBalance(openingBalance);
        detail.setExtraMoney(openingBalance.max(ZERO));
        detail.setNetPayable(openingBalance);

        return detailRepository.save(detail);
    }

    // =========================================================
    // RECALCULATE EMPLOYEE MONTHS
    // =========================================================

    public void recalculateEmployeeFrom(
            Long employeeId,
            LocalDate affectedDate,
            String reason) {

        Long ownerId = currentOwnerService.getCurrentOwnerId();

        recalculateEmployeeFrom(ownerId, employeeId, affectedDate, reason);
    }

    public void recalculateEmployeeFrom(
            Long ownerId,
            Long employeeId,
            LocalDate affectedDate,
            String reason) {

        Employee employee = employeeRepository.findByIdAndOwnerId(employeeId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

        List<Attendance> attendance = attendanceRepository.findAllForEmployeeAndOwner(ownerId, employeeId);

        List<EmployeeAdvance> advances = advanceRepository.findByOwnerIdAndEmployeeId(ownerId, employeeId);

        List<EmployeeSettlement> settlements = settlementRepository.findByOwnerIdAndEmployeeId(ownerId, employeeId);

        List<HisabDetail> existingDetails = detailRepository.findAllForEmployeeAndOwner(employeeId, ownerId);

        List<Hisab> existingMonths = hisabRepository.findByOwnerIdOrderByYearAscMonthAsc(ownerId);

        TreeSet<YearMonth> periods = new TreeSet<>();

        attendance.stream()
                .map(Attendance::getAttendanceDate)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .forEach(periods::add);

        advances.stream()
                .map(EmployeeAdvance::getPaymentDate)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .forEach(periods::add);

        settlements.stream()
                .map(EmployeeSettlement::getSettlementDate)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .forEach(periods::add);

        existingMonths.forEach(h -> periods.add(YearMonth.of(h.getYear(), h.getMonth())));

        existingDetails.forEach(d -> periods.add(YearMonth.of(
                d.getHisab().getYear(),
                d.getHisab().getMonth())));

        periods.add(YearMonth.from(affectedDate));

        YearMonth period = periods.first();
        YearMonth lastPeriod = periods.last();

        while (!period.isAfter(lastPeriod)) {
            YearMonth currentPeriod = period;
            LocalDate start = currentPeriod.atDay(1);
            LocalDate end = currentPeriod.atEndOfMonth();

            boolean hasAttendance = attendance.stream().anyMatch(a -> !a.getAttendanceDate().isBefore(start)
                    && !a.getAttendanceDate().isAfter(end));

            boolean hasAdvance = advances.stream().anyMatch(a -> !a.getPaymentDate().isBefore(start)
                    && !a.getPaymentDate().isAfter(end));

            boolean hasSettlement = settlements.stream()
                    .map(EmployeeSettlement::getSettlementDate)
                    .filter(Objects::nonNull)
                    .anyMatch(date -> !date.isBefore(start) && !date.isAfter(end));

            boolean hasMonth = existingMonths.stream().anyMatch(h -> h.getYear().equals(currentPeriod.getYear())
                    && h.getMonth().equals(currentPeriod.getMonthValue()));

            boolean hasDetail = existingDetails.stream()
                    .anyMatch(d -> d.getHisab().getYear().equals(currentPeriod.getYear())
                            && d.getHisab().getMonth().equals(currentPeriod.getMonthValue()));

            boolean isAffectedMonth = currentPeriod.equals(YearMonth.from(affectedDate));

            if (hasAttendance || hasAdvance || hasSettlement
                    || hasMonth || hasDetail || isAffectedMonth) {

                if (employee.getCreatedAt() == null
                        || !employee.getCreatedAt().toLocalDate().isAfter(end)) {

                    recalculateOneMonth(
                            ownerId,
                            employee,
                            currentPeriod,
                            attendance,
                            advances,
                            settlements);
                }
            }

            period = period.plusMonths(1);
        }
    }

    private void recalculateOneMonth(
            Long ownerId,
            Employee employee,
            YearMonth period,
            List<Attendance> allAttendance,
            List<EmployeeAdvance> allAdvances,
            List<EmployeeSettlement> allSettlements) {

        int year = period.getYear();
        int month = period.getMonthValue();

        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        HisabDetail detail = getOrCreateDetail(ownerId, employee.getId(), year, month);

        BigDecimal previousClosing = findPreviousClosing(ownerId, employee.getId(), year, month);

        if (!detail.isManualOpeningBalance()
                || hasEarlierDetail(ownerId, employee.getId(), period)) {
            detail.setOpeningBalance(previousClosing);
        }

        List<Attendance> monthAttendance = allAttendance.stream()
                .filter(a -> !a.getAttendanceDate().isBefore(start)
                        && !a.getAttendanceDate().isAfter(end))
                .toList();

        int presentDays = (int) monthAttendance.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
                .count();

        int halfDays = (int) monthAttendance.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.HALF_DAY)
                .count();

        BigDecimal attendanceUnits = calculateAttendanceUnits(presentDays, halfDays);

        BigDecimal dailyRate = money(detail.getRateSnapshot());

        BigDecimal earnings = calculateEarnings(attendanceUnits, dailyRate);

        BigDecimal monthlyAdvances = money(allAdvances.stream()
                .filter(a -> !a.getPaymentDate().isBefore(start)
                        && !a.getPaymentDate().isAfter(end))
                .map(EmployeeAdvance::getAmount)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add));

        BigDecimal monthlySettlements = money(allSettlements.stream()
                .filter(s -> s.getSettlementDate() != null
                        && !s.getSettlementDate().isBefore(start)
                        && !s.getSettlementDate().isAfter(end))
                .map(EmployeeSettlement::getAmountPaid)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add));

        BigDecimal finalBalance = calculateFinalBalance(
                money(detail.getOpeningBalance()),
                earnings,
                monthlyAdvances,
                monthlySettlements);

        detail.setEmployeeName(employee.getName());
        detail.setPresentDays(presentDays);
        detail.setHalfDays(halfDays);
        detail.setAttendanceUnits(attendanceUnits);
        detail.setTotalEarning(earnings);
        detail.setTotalAdvance(monthlyAdvances);
        detail.setTotalSettlement(monthlySettlements);
        detail.setClosingBalance(finalBalance);
        detail.setExtraMoney(finalBalance.max(ZERO));
        detail.setNetPayable(finalBalance);

        detailRepository.save(detail);

        refreshHeader(detail.getHisab());
    }

    // =========================================================
    // ALL HISAB CALCULATIONS LIVE HERE
    // =========================================================

    private BigDecimal calculateAttendanceUnits(
            int presentDays,
            int halfDays) {

        return BigDecimal.valueOf(presentDays)
                .add(HALF.multiply(BigDecimal.valueOf(halfDays)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEarnings(
            BigDecimal attendanceUnits,
            BigDecimal dailyRate) {

        return money(money(attendanceUnits).multiply(money(dailyRate)));
    }

    private BigDecimal calculateFinalBalance(
            BigDecimal openingBalance,
            BigDecimal earnings,
            BigDecimal advances,
            BigDecimal settlements) {

        return money(
                money(openingBalance)
                        .add(money(earnings))
                        .subtract(money(advances))
                        .subtract(money(settlements)));
    }

    // =========================================================
    // ATTENDANCE INTEGRATION
    // =========================================================

    public void validateAttendanceMutation(
            Long ownerId,
            Long employeeId,
            LocalDate date,
            String reason) {
        // Keep this method for existing Attendance service integrations.
    }

    public void recordAttendanceChange(
            Long ownerId,
            Long employeeId,
            LocalDate date,
            String action,
            String reason,
            Object oldValues,
            Object newValues) {

        recalculateEmployeeFrom(ownerId, employeeId, date, reason);
    }

    public boolean isClosed(Long ownerId, LocalDate date) {
        return hisabRepository.findByOwnerIdAndYearAndMonth(
                ownerId, date.getYear(), date.getMonthValue())
                .map(h -> h.getStatus() == Hisab.Status.CLOSED)
                .orElse(false);
    }

    // =========================================================
    // OPENING BALANCE
    // =========================================================

    public void setOpeningBalance(
            Long employeeId,
            int year,
            int month,
            BigDecimal openingBalance,
            String reason) {

        Long ownerId = currentOwnerService.getCurrentOwnerId();

        HisabDetail detail = getOrCreateDetail(ownerId, employeeId, year, month);

        if (hasEarlierDetail(ownerId, employeeId, YearMonth.of(year, month))) {
            throw new BadRequestException(
                    "Opening balance is calculated from the previous month's final balance.");
        }

        detail.setOpeningBalance(money(openingBalance));
        detail.setManualOpeningBalance(true);

        detailRepository.save(detail);

        recalculateEmployeeFrom(
                ownerId,
                employeeId,
                YearMonth.of(year, month).atDay(1),
                reason);
    }

    // =========================================================
    // MONTHLY RATE CORRECTION
    // =========================================================

    public void correctRate(
            Long employeeId,
            int year,
            int month,
            BigDecimal rate,
            String reason) {

        if (rate == null || rate.signum() < 0) {
            throw new BadRequestException("Daily rate cannot be negative.");
        }

        Long ownerId = currentOwnerService.getCurrentOwnerId();

        HisabDetail detail = getOrCreateDetail(ownerId, employeeId, year, month);

        detail.setRateSnapshot(money(rate));

        detailRepository.save(detail);

        recalculateEmployeeFrom(
                ownerId,
                employeeId,
                YearMonth.of(year, month).atDay(1),
                reason);
    }

    // =========================================================
    // EMPLOYEE TRANSACTIONS
    // =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getEmployeeTransactions(
            Long hisabId,
            Long employeeId) {

        Long ownerId = currentOwnerService.getCurrentOwnerId();

        Hisab hisab = hisabRepository.findByIdAndOwnerId(hisabId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly Hisab not found."));

        detailRepository.findByHisabIdAndEmployeeIdAndOwnerId(
                hisabId, employeeId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee Hisab detail not found."));

        LocalDate start = YearMonth.of(
                hisab.getYear(), hisab.getMonth()).atDay(1);

        LocalDate end = YearMonth.of(
                hisab.getYear(), hisab.getMonth()).atEndOfMonth();

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("employeeId", employeeId);
        result.put("year", hisab.getYear());
        result.put("month", hisab.getMonth());

        result.put("attendance",
                attendanceRepository.findByOwnerIdAndEmployeeIdAndMonth(
                        ownerId, employeeId, start, end)
                        .stream()
                        .map(a -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("date", a.getAttendanceDate());
                            row.put("status", a.getStatus().name());
                            row.put("reason", a.getReason());
                            return row;
                        }).toList());

        result.put("advances",
                advanceRepository.findByOwnerIdAndEmployeeIdAndPaymentDateBetween(
                        ownerId, employeeId, start, end)
                        .stream()
                        .map(a -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("id", a.getId());
                            row.put("date", a.getPaymentDate());
                            row.put("amount", a.getAmount());
                            row.put("note", a.getNote());
                            return row;
                        }).toList());

        result.put("settlements",
                settlementRepository.findByOwnerIdAndEmployeeIdAndSettlementDateBetween(
                        ownerId, employeeId, start, end)
                        .stream()
                        .map(s -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("id", s.getId());
                            row.put("date", s.getSettlementDate());
                            row.put("amountPaid", s.getAmountPaid());
                            row.put("note", s.getNote());
                            return row;
                        }).toList());

        return result;
    }

    // =========================================================
    // RESPONSE BUILDING
    // =========================================================

    private HisabResponseDTO buildResponse(Long ownerId, Long hisabId) {
        Hisab hisab = hisabRepository.findByIdAndOwnerId(hisabId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly Hisab not found."));

        List<HisabDetail> details = detailRepository.findByHisabIdOrderByEmployeeNameAsc(hisab.getId());

        refreshHeader(hisab);

        return new HisabResponseDTO(
                hisab.getId(),
                hisab.getYear(),
                hisab.getMonth(),
                hisab.getStatus().name(),
                hisab.getClosedAt(),
                hisab.getTotalEmployees(),
                money(hisab.getTotalPayable()),
                money(hisab.getTotalOverAdvance()),
                details.stream().map(this::toDTO).toList());
    }

    private Hisab getOrCreateMonth(Long ownerId, int year, int month) {
        validatePeriod(year, month);

        return hisabRepository.findByOwnerIdAndYearAndMonth(ownerId, year, month)
                .orElseGet(() -> {
                    Owner owner = ownerRepository.findById(ownerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Owner not found."));

                    Hisab hisab = new Hisab();
                    hisab.setOwner(owner);
                    hisab.setYear(year);
                    hisab.setMonth(month);
                    hisab.setStatus(Hisab.Status.OPEN);
                    hisab.setTotalEmployees(0L);
                    hisab.setTotalPayable(ZERO);
                    hisab.setTotalOverAdvance(ZERO);

                    return hisabRepository.save(hisab);
                });
    }

    private BigDecimal findPreviousClosing(
            Long ownerId,
            Long employeeId,
            int year,
            int month) {

        return detailRepository.findPreviousDetails(
                employeeId, ownerId, year, month)
                .stream()
                .findFirst()
                .map(d -> money(d.getClosingBalance()))
                .orElse(ZERO);
    }

    private boolean hasEarlierDetail(
            Long ownerId,
            Long employeeId,
            YearMonth period) {

        return !detailRepository.findPreviousDetails(
                employeeId,
                ownerId,
                period.getYear(),
                period.getMonthValue()).isEmpty();
    }

    private void refreshHeader(Hisab hisab) {
        List<HisabDetail> rows = detailRepository.findByHisabIdOrderByEmployeeNameAsc(hisab.getId());

        BigDecimal totalPayable = ZERO;
        BigDecimal totalOverAdvance = ZERO;

        for (HisabDetail row : rows) {
            BigDecimal balance = money(row.getClosingBalance());

            if (balance.signum() > 0) {
                totalPayable = totalPayable.add(balance);
            } else if (balance.signum() < 0) {
                totalOverAdvance = totalOverAdvance.add(balance.abs());
            }
        }

        hisab.setTotalEmployees((long) rows.size());
        hisab.setTotalPayable(money(totalPayable));
        hisab.setTotalOverAdvance(money(totalOverAdvance));

        hisabRepository.save(hisab);
    }

    private EmployeeHisabDTO toDTO(HisabDetail detail) {
        BigDecimal balance = money(detail.getClosingBalance());

        String status = balance.signum() > 0
                ? "EXTRA"
                : balance.signum() < 0
                        ? "DUE"
                        : "SETTLED";

        return new EmployeeHisabDTO(
                detail.getId(),
                detail.getEmployee() == null ? null : detail.getEmployee().getId(),
                detail.getEmployeeName(),
                detail.getPresentDays(),
                detail.getHalfDays(),
                money(detail.getAttendanceUnits()),
                money(detail.getRateSnapshot()),
                money(detail.getTotalEarning()),
                money(detail.getOpeningBalance()),
                money(detail.getTotalAdvance()),
                money(detail.getTotalSettlement()),
                balance,
                status);
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? ZERO : amount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validatePeriod(int year, int month) {
        if (year < 2000 || year > 2200 || month < 1 || month > 12) {
            throw new BadRequestException("Invalid year or month.");
        }
    }
}

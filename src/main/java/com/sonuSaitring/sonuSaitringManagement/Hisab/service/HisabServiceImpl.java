package com.sonuSaitring.sonuSaitringManagement.Hisab.service;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import com.sonuSaitring.sonuSaitringManagement.Hisab.repository.HisabDetailRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.repository.HisabRepository;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HisabServiceImpl implements HisabService {

    private static final Logger logger = LoggerFactory.getLogger(HisabServiceImpl.class);

    @Autowired
    private HisabRepository hisabRepository;

    @Autowired
    private HisabDetailRepository hisabDetailRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeAdvanceRepository advanceRepository;

    @Autowired
    private EmployeeSettlementRepository settlementRepository;

    @Transactional
    public Hisab generateMonthReport(int year, int month) {
        logger.info("=== Starting Month Report Generation for Year: {}, Month: {} ===", year, month);
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDate settlementCutoffDate = endDate.plusDays(10);
        LocalDate nextMonthFirstDay = startDate.plusMonths(1);

        LocalDate prevMonthDate = startDate.minusMonths(1);
        Hisab prevHisab = hisabRepository.findByYearAndMonth(prevMonthDate.getYear(), prevMonthDate.getMonthValue())
                .orElse(null);
        Map<Long, BigDecimal> prevRemainingBalanceMap = new java.util.HashMap<>();
        if (prevHisab != null && prevHisab.getDetails() != null) {
            for (HisabDetail pd : prevHisab.getDetails()) {
                if (pd.getEmployee() != null && pd.getRemainingBalance() != null) {
                    prevRemainingBalanceMap.put(pd.getEmployee().getId(), pd.getRemainingBalance());
                }
            }
        }

        List<Employee> activeEmployees = employeeRepository.findAll();
        logger.info("Found {} active employees for month closing processing.", activeEmployees.size());

        List<EmployeeSettlement> monthSettlements = settlementRepository.findBySettlementDateBetween(startDate,
                settlementCutoffDate);

        String currentMonthKey = month + "/" + year;

        List<EmployeeSettlement> filteredMonthSettlements = monthSettlements.stream()
                .filter(s -> s.getEmployee() != null && s.getAmountPaid() != null)
                .filter(s -> {
                    String note = s.getNote() != null ? s.getNote().toLowerCase() : "";
                    if (note.contains("closing") || note.contains("month closing")) {
                        return note.contains(currentMonthKey);
                    }
                    return true;
                })
                .collect(Collectors.toList());

        Map<Long, BigDecimal> paidAmountMap = filteredMonthSettlements.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getEmployee().getId(),
                        Collectors.mapping(EmployeeSettlement::getAmountPaid,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        Hisab hisabRecord = hisabRepository
                .findByYearAndMonth(year, month)
                .orElseGet(() -> {
                    logger.info("Creating new Hisab record container for Year: {}, Month: {}", year, month);
                    Hisab hr = new Hisab();
                    hr.setYear(year);
                    hr.setMonth(month);
                    hr.setClosedAt(LocalDateTime.now());
                    return hr;
                });

        Map<Long, HisabDetail> existingDetailsMap = new java.util.HashMap<>();
        if (hisabRecord.getDetails() != null) {
            existingDetailsMap = hisabRecord.getDetails().stream()
                    .filter(d -> d.getEmployee() != null)
                    .collect(Collectors.toMap(d -> d.getEmployee().getId(), d -> d));
        }

        List<HisabDetail> newDetailsList = new ArrayList<>();

        for (Employee employee : activeEmployees) {
            List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndMonth(employee.getId(), startDate,
                    endDate);

            BigDecimal totalPresences = BigDecimal.ZERO;
            if (attendances != null) {
                for (Attendance attendance : attendances) {
                    String statusStr = attendance.getStatus() != null ? attendance.getStatus().name() : "";
                    if ("PRESENT".equalsIgnoreCase(statusStr)) {
                        totalPresences = totalPresences.add(BigDecimal.ONE);
                    } else if ("HALF_DAY".equalsIgnoreCase(statusStr)) {
                        totalPresences = totalPresences.add(new BigDecimal("0.5"));
                    }
                }
            }

            List<EmployeeAdvance> advances = advanceRepository.findByEmployeeIdAndPaymentDateBetween(employee.getId(),
                    startDate, endDate);

            BigDecimal currentMonthAdvance = BigDecimal.ZERO;
            if (advances != null) {
                for (EmployeeAdvance a : advances) {
                    String note = a.getNote() != null ? a.getNote().toLowerCase() : "";
                    boolean isCarryForward = note.contains("carry-forward")
                            || note.startsWith("previous month balance carry-forward");

                    if (!isCarryForward && a.getAmount() != null) {
                        currentMonthAdvance = currentMonthAdvance.add(BigDecimal.valueOf(a.getAmount()));
                    }
                }
            }

            BigDecimal rate = employee.getInitialRate() != null ? employee.getInitialRate() : BigDecimal.ZERO;
            BigDecimal totalEarning = totalPresences.multiply(rate);
            BigDecimal previousMonthBalance = prevRemainingBalanceMap.getOrDefault(employee.getId(), BigDecimal.ZERO);
            BigDecimal safeCurrentMonthAdvance = currentMonthAdvance != null ? currentMonthAdvance : BigDecimal.ZERO;

            BigDecimal netPayable = totalEarning.subtract(safeCurrentMonthAdvance).add(previousMonthBalance);
            BigDecimal amountPaid = paidAmountMap.getOrDefault(employee.getId(), BigDecimal.ZERO);
            BigDecimal remainingBalance = netPayable.subtract(amountPaid);

            HisabDetail detail = existingDetailsMap.getOrDefault(employee.getId(), new HisabDetail());
            detail.setMonthClosing(hisabRecord);
            detail.setEmployee(employee);
            detail.setEmployeeName(employee.getName());
            detail.setTotalPresences(totalPresences);
            detail.setRate(rate);
            detail.setTotalEarning(totalEarning);
            detail.setTotalAdvance(safeCurrentMonthAdvance);
            detail.setPreviousBalance(previousMonthBalance);
            detail.setExtraMoney(BigDecimal.ZERO);
            detail.setNetPayable(netPayable);
            detail.setAmountPaid(amountPaid);
            detail.setRemainingBalance(remainingBalance);

            List<EmployeeSettlement> empSpecificSettlements = filteredMonthSettlements.stream()
                    .filter(s -> s.getEmployee().getId().equals(employee.getId()))
                    .collect(Collectors.toList());
            detail.setSettlements(empSpecificSettlements);

            String carryForwardNote = "Previous month balance carry-forward from " + month + "/" + year;
            List<EmployeeAdvance> existingNextMonthAdvances = advanceRepository.findByEmployeeIdAndPaymentDateBetween(
                    employee.getId(), nextMonthFirstDay, nextMonthFirstDay);

            EmployeeAdvance existingCarryForward = existingNextMonthAdvances.stream()
                    .filter(a -> a.getNote() != null && a.getNote().startsWith("Previous month balance carry-forward"))
                    .findFirst()
                    .orElse(null);

            if (remainingBalance.compareTo(BigDecimal.ZERO) != 0) {
                if (existingCarryForward == null) {
                    EmployeeAdvance carryForwardAdvance = new EmployeeAdvance();
                    carryForwardAdvance.setEmployee(employee);
                    carryForwardAdvance.setAmount(remainingBalance.doubleValue());
                    carryForwardAdvance.setPaymentDate(nextMonthFirstDay);
                    carryForwardAdvance.setNote(carryForwardNote
                            + (remainingBalance.compareTo(BigDecimal.ZERO) < 0 ? " (Due)" : " (Extra Credit)"));
                    advanceRepository.save(carryForwardAdvance);
                } else {
                    existingCarryForward.setAmount(remainingBalance.doubleValue());
                    existingCarryForward.setNote(carryForwardNote
                            + (remainingBalance.compareTo(BigDecimal.ZERO) < 0 ? " (Due)" : " (Extra Credit)"));
                    advanceRepository.save(existingCarryForward);
                }
            } else {
                if (existingCarryForward != null) {
                    advanceRepository.delete(existingCarryForward);
                }
            }

            newDetailsList.add(detail);
        }

        if (hisabRecord.getDetails() == null) {
            hisabRecord.setDetails(new ArrayList<>());
        } else {
            hisabRecord.getDetails().clear();
        }

        for (HisabDetail detail : newDetailsList) {
            hisabRecord.getDetails().add(detail);
            detail.setMonthClosing(hisabRecord);
        }

        Hisab savedHisab = hisabRepository.save(hisabRecord);
        logger.info("=== Successfully completed Month Report Generation for Year: {}, Month: {} ===", year, month);
        return savedHisab;
    }

    @Override
    @Transactional
    public HisabDetail markEmployeeHisabCompleted(Long detailId, boolean completed) {
        logger.info("Toggling hisab completion status for detailId: {} to {}", detailId, completed);
        HisabDetail detail = hisabDetailRepository.findById(detailId)
                .orElseThrow(() -> {
                    logger.error("Hisab detail not found with id: {}", detailId);
                    return new RuntimeException("Hisab detail not found with id: " + detailId);
                });

        detail.setHisabCompleted(completed);
        return hisabDetailRepository.save(detail);
    }

    @Override
    public List<Hisab> getAllMonthClosings() {
        logger.info("Fetching all month closing records.");
        return hisabRepository.findAll();
    }

    @Override
    public Hisab getMonthClosingById(Long id) {
        logger.info("Fetching month closing by ID: {}", id);
        return hisabRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hisab record not found with id: " + id));
    }

    @Override
    @Transactional
    public Hisab getMonthClosingByYearAndMonth(int year, int month) {
        return generateMonthReport(year, month);
    }
}
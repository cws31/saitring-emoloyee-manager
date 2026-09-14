package com.sonuSaitring.sonuSaitringManagement.Hisab.service;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab;
import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import com.sonuSaitring.sonuSaitringManagement.Hisab.repository.HisabDetailRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.repository.HisabRepository;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        @Autowired
        private CurrentOwnerService currentOwnerService;

        private final Counter reportGenerationSuccess;
        private final Counter reportGenerationFailure;
        private final Counter completionUpdate;
        private final Counter completionNotFound;
        private final Counter lookupSuccess;
        private final Counter lookupNotFound;
        private final Counter carryForwardCreated;
        private final Counter carryForwardUpdated;
        private final Counter carryForwardDeleted;

        private final Timer reportGenerationTimer;
        private final Timer completionUpdateTimer;
        private final Timer lookupTimer;
        private final Timer listTimer;

        public HisabServiceImpl(MeterRegistry meterRegistry) {

                this.reportGenerationSuccess = Counter.builder("hisab.report.generation")
                                .tag("result", "success")
                                .description("Number of successfully generated Hisab reports")
                                .register(meterRegistry);

                this.reportGenerationFailure = Counter.builder("hisab.report.generation")
                                .tag("result", "failure")
                                .description("Number of failed Hisab report generations")
                                .register(meterRegistry);

                this.completionUpdate = Counter.builder("hisab.completion.update")
                                .description("Number of Hisab completion status updates")
                                .register(meterRegistry);

                this.completionNotFound = Counter.builder("hisab.completion.not_found")
                                .description("Number of Hisab completion updates where detail was not found")
                                .register(meterRegistry);

                this.lookupSuccess = Counter.builder("hisab.lookup")
                                .tag("result", "success")
                                .description("Number of successful Hisab lookups")
                                .register(meterRegistry);

                this.lookupNotFound = Counter.builder("hisab.lookup")
                                .tag("result", "not_found")
                                .description("Number of unsuccessful Hisab lookups")
                                .register(meterRegistry);

                this.carryForwardCreated = Counter.builder("hisab.carry_forward")
                                .tag("operation", "created")
                                .description("Number of carry-forward advances created")
                                .register(meterRegistry);

                this.carryForwardUpdated = Counter.builder("hisab.carry_forward")
                                .tag("operation", "updated")
                                .description("Number of carry-forward advances updated")
                                .register(meterRegistry);

                this.carryForwardDeleted = Counter.builder("hisab.carry_forward")
                                .tag("operation", "deleted")
                                .description("Number of carry-forward advances deleted")
                                .register(meterRegistry);

                this.reportGenerationTimer = Timer.builder("hisab.report.generation.duration")
                                .description("Time taken to generate a Hisab report")
                                .register(meterRegistry);

                this.completionUpdateTimer = Timer.builder("hisab.completion.update.duration")
                                .description("Time taken to update Hisab completion status")
                                .register(meterRegistry);

                this.lookupTimer = Timer.builder("hisab.lookup.duration")
                                .description("Time taken for Hisab lookup")
                                .register(meterRegistry);

                this.listTimer = Timer.builder("hisab.list.duration")
                                .description("Time taken to fetch Hisab month closings")
                                .register(meterRegistry);
        }

        @Transactional
        public Hisab generateMonthReport(int year, int month) {

                return reportGenerationTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();
                        Owner owner = currentOwnerService.getCurrentOwner();

                        logger.info(
                                        "Starting month report generation: ownerId={}, year={}, month={}",
                                        ownerId,
                                        year,
                                        month);

                        try {

                                LocalDate startDate = LocalDate.of(year, month, 1);

                                LocalDate endDate = startDate.withDayOfMonth(
                                                startDate.lengthOfMonth());

                                LocalDate settlementCutoffDate = endDate.plusDays(10);

                                LocalDate nextMonthFirstDay = startDate.plusMonths(1);

                                LocalDate prevMonthDate = startDate.minusMonths(1);

                                Hisab prevHisab = hisabRepository
                                                .findByOwnerIdAndYearAndMonth(
                                                                ownerId,
                                                                prevMonthDate.getYear(),
                                                                prevMonthDate.getMonthValue())
                                                .orElse(null);

                                Map<Long, BigDecimal> prevRemainingBalanceMap = new java.util.HashMap<>();

                                if (prevHisab != null
                                                && prevHisab.getDetails() != null) {

                                        for (HisabDetail pd : prevHisab.getDetails()) {

                                                if (pd.getEmployee() != null
                                                                && pd.getRemainingBalance() != null) {

                                                        prevRemainingBalanceMap.put(
                                                                        pd.getEmployee().getId(),
                                                                        pd.getRemainingBalance());
                                                }
                                        }
                                }

                                List<Employee> activeEmployees = employeeRepository.findAllByOwnerId(ownerId)
                                                .stream()
                                                .filter(employee -> !employee.isBlocked())
                                                .collect(Collectors.toList());

                                logger.info(
                                                "Active employees found: ownerId={}, count={}",
                                                ownerId,
                                                activeEmployees.size());

                                List<EmployeeSettlement> monthSettlements = settlementRepository
                                                .findByOwnerIdAndSettlementDateBetween(
                                                                ownerId,
                                                                startDate,
                                                                settlementCutoffDate);

                                String currentMonthKey = month + "/" + year;

                                List<EmployeeSettlement> filteredMonthSettlements = monthSettlements.stream()
                                                .filter(s -> s.getEmployee() != null
                                                                && s.getAmountPaid() != null)
                                                .filter(s -> {

                                                        String note = s.getNote() != null
                                                                        ? s.getNote().toLowerCase()
                                                                        : "";

                                                        if (note.contains("closing")
                                                                        || note.contains("month closing")) {

                                                                return note.contains(currentMonthKey);
                                                        }

                                                        return true;
                                                })
                                                .collect(Collectors.toList());

                                logger.debug(
                                                "Settlement data loaded: ownerId={}, total={}, filtered={}",
                                                ownerId,
                                                monthSettlements.size(),
                                                filteredMonthSettlements.size());

                                Map<Long, BigDecimal> paidAmountMap = filteredMonthSettlements.stream()
                                                .collect(Collectors.groupingBy(
                                                                s -> s.getEmployee().getId(),
                                                                Collectors.mapping(
                                                                                EmployeeSettlement::getAmountPaid,
                                                                                Collectors.reducing(
                                                                                                BigDecimal.ZERO,
                                                                                                BigDecimal::add))));

                                Hisab hisabRecord = hisabRepository
                                                .findByOwnerIdAndYearAndMonth(
                                                                ownerId,
                                                                year,
                                                                month)
                                                .orElseGet(() -> {

                                                        logger.info(
                                                                        "Creating new Hisab record: ownerId={}, year={}, month={}",
                                                                        ownerId,
                                                                        year,
                                                                        month);

                                                        Hisab hr = new Hisab();

                                                        hr.setYear(year);
                                                        hr.setMonth(month);
                                                        hr.setClosedAt(LocalDateTime.now());
                                                        hr.setOwner(owner);

                                                        return hr;
                                                });

                                if (hisabRecord.getOwner() == null) {
                                        hisabRecord.setOwner(owner);
                                }

                                Map<Long, HisabDetail> existingDetailsMap = new java.util.HashMap<>();

                                if (hisabRecord.getDetails() != null) {

                                        existingDetailsMap = hisabRecord.getDetails()
                                                        .stream()
                                                        .filter(d -> d.getEmployee() != null)
                                                        .collect(Collectors.toMap(
                                                                        d -> d.getEmployee().getId(),
                                                                        d -> d));
                                }

                                BigDecimal totalPayable = BigDecimal.ZERO;

                                BigDecimal totalOverAdvance = BigDecimal.ZERO;

                                List<HisabDetail> newDetailsList = new ArrayList<>();

                                int carryForwardCreatedCount = 0;
                                int carryForwardUpdatedCount = 0;
                                int carryForwardDeletedCount = 0;

                                for (Employee employee : activeEmployees) {

                                        List<Attendance> attendances = attendanceRepository
                                                        .findByOwnerIdAndEmployeeIdAndMonth(
                                                                        ownerId,
                                                                        employee.getId(),
                                                                        startDate,
                                                                        endDate);

                                        BigDecimal totalPresences = BigDecimal.ZERO;

                                        if (attendances != null) {

                                                for (Attendance attendance : attendances) {

                                                        String statusStr = attendance.getStatus() != null
                                                                        ? attendance.getStatus().name()
                                                                        : "";

                                                        if ("PRESENT".equalsIgnoreCase(statusStr)) {

                                                                totalPresences = totalPresences.add(BigDecimal.ONE);

                                                        } else if ("HALF_DAY".equalsIgnoreCase(statusStr)) {

                                                                totalPresences = totalPresences.add(
                                                                                new BigDecimal("0.5"));
                                                        }
                                                }
                                        }

                                        List<EmployeeAdvance> advances = advanceRepository
                                                        .findByOwnerIdAndEmployeeIdAndPaymentDateBetween(
                                                                        ownerId,
                                                                        employee.getId(),
                                                                        startDate,
                                                                        endDate);

                                        BigDecimal currentMonthAdvance = BigDecimal.ZERO;

                                        if (advances != null) {

                                                for (EmployeeAdvance a : advances) {

                                                        String note = a.getNote() != null
                                                                        ? a.getNote().toLowerCase()
                                                                        : "";

                                                        boolean isCarryForward = note.contains("carry-forward")
                                                                        || note.startsWith(
                                                                                        "previous month balance carry-forward");

                                                        if (!isCarryForward
                                                                        && a.getAmount() != null) {

                                                                currentMonthAdvance = currentMonthAdvance.add(
                                                                                BigDecimal.valueOf(
                                                                                                a.getAmount()));
                                                        }
                                                }
                                        }

                                        BigDecimal rate = employee.getInitialRate() != null
                                                        ? employee.getInitialRate()
                                                        : BigDecimal.ZERO;

                                        BigDecimal totalEarning = totalPresences.multiply(rate);

                                        BigDecimal previousMonthBalance = prevRemainingBalanceMap.getOrDefault(
                                                        employee.getId(),
                                                        BigDecimal.ZERO);

                                        BigDecimal safeCurrentMonthAdvance = currentMonthAdvance != null
                                                        ? currentMonthAdvance
                                                        : BigDecimal.ZERO;

                                        BigDecimal netPayable = totalEarning
                                                        .subtract(safeCurrentMonthAdvance)
                                                        .add(previousMonthBalance);

                                        BigDecimal amountPaid = paidAmountMap.getOrDefault(
                                                        employee.getId(),
                                                        BigDecimal.ZERO);

                                        BigDecimal remainingBalance = netPayable.subtract(amountPaid);

                                        if (remainingBalance.compareTo(BigDecimal.ZERO) > 0) {

                                                totalPayable = totalPayable.add(remainingBalance);

                                        } else if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {

                                                totalOverAdvance = totalOverAdvance.add(
                                                                remainingBalance.abs());
                                        }

                                        HisabDetail detail = existingDetailsMap.getOrDefault(
                                                        employee.getId(),
                                                        new HisabDetail());

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

                                        List<EmployeeSettlement> empSpecificSettlements = filteredMonthSettlements
                                                        .stream()
                                                        .filter(s -> s.getEmployee()
                                                                        .getId()
                                                                        .equals(employee.getId()))
                                                        .collect(Collectors.toList());

                                        detail.setSettlements(empSpecificSettlements);

                                        String carryForwardNote = "Previous month balance carry-forward from "
                                                        + month
                                                        + "/"
                                                        + year;

                                        List<EmployeeAdvance> existingNextMonthAdvances = advanceRepository
                                                        .findByOwnerIdAndEmployeeIdAndPaymentDateBetween(
                                                                        ownerId,
                                                                        employee.getId(),
                                                                        nextMonthFirstDay,
                                                                        nextMonthFirstDay);

                                        EmployeeAdvance existingCarryForward = existingNextMonthAdvances.stream()
                                                        .filter(a -> a.getNote() != null
                                                                        && a.getNote().startsWith(
                                                                                        "Previous month balance carry-forward"))
                                                        .findFirst()
                                                        .orElse(null);

                                        if (remainingBalance.compareTo(
                                                        BigDecimal.ZERO) != 0) {

                                                if (existingCarryForward == null) {

                                                        EmployeeAdvance carryForwardAdvance = new EmployeeAdvance();

                                                        carryForwardAdvance.setEmployee(employee);

                                                        carryForwardAdvance.setOwner(owner);

                                                        carryForwardAdvance.setAmount(
                                                                        remainingBalance.doubleValue());

                                                        carryForwardAdvance.setPaymentDate(
                                                                        nextMonthFirstDay);

                                                        carryForwardAdvance.setNote(
                                                                        carryForwardNote
                                                                                        + (remainingBalance.compareTo(
                                                                                                        BigDecimal.ZERO) < 0
                                                                                                                        ? " (Due)"
                                                                                                                        : " (Extra Credit)"));

                                                        advanceRepository.save(
                                                                        carryForwardAdvance);

                                                        carryForwardCreatedCount++;
                                                        carryForwardCreated.increment();

                                                } else {

                                                        existingCarryForward.setOwner(owner);

                                                        existingCarryForward.setAmount(
                                                                        remainingBalance.doubleValue());

                                                        existingCarryForward.setNote(
                                                                        carryForwardNote
                                                                                        + (remainingBalance.compareTo(
                                                                                                        BigDecimal.ZERO) < 0
                                                                                                                        ? " (Due)"
                                                                                                                        : " (Extra Credit)"));

                                                        advanceRepository.save(
                                                                        existingCarryForward);

                                                        carryForwardUpdatedCount++;
                                                        carryForwardUpdated.increment();
                                                }

                                        } else {

                                                if (existingCarryForward != null) {

                                                        advanceRepository.deleteByIdAndOwnerId(
                                                                        existingCarryForward.getId(),
                                                                        ownerId);

                                                        carryForwardDeletedCount++;
                                                        carryForwardDeleted.increment();
                                                }
                                        }

                                        newDetailsList.add(detail);
                                }

                                hisabRecord.setTotalEmployees(
                                                (long) activeEmployees.size());

                                hisabRecord.setTotalPayable(
                                                totalPayable.setScale(
                                                                2,
                                                                RoundingMode.HALF_UP));

                                hisabRecord.setTotalOverAdvance(
                                                totalOverAdvance.setScale(
                                                                2,
                                                                RoundingMode.HALF_UP));

                                if (hisabRecord.getDetails() == null) {

                                        hisabRecord.setDetails(
                                                        new ArrayList<>());

                                } else {

                                        hisabRecord.getDetails().clear();
                                }

                                for (HisabDetail detail : newDetailsList) {

                                        hisabRecord.getDetails().add(detail);

                                        detail.setMonthClosing(hisabRecord);
                                }

                                Hisab savedHisab = hisabRepository.save(hisabRecord);

                                reportGenerationSuccess.increment();

                                logger.info(
                                                "Month closing completed: ownerId={}, year={}, month={}, employees={}, carryForwardCreated={}, carryForwardUpdated={}, carryForwardDeleted={}",
                                                ownerId,
                                                year,
                                                month,
                                                activeEmployees.size(),
                                                carryForwardCreatedCount,
                                                carryForwardUpdatedCount,
                                                carryForwardDeletedCount);

                                logger.debug(
                                                "Month closing financial summary: ownerId={}, totalPayable={}, totalOverAdvance={}",
                                                ownerId,
                                                totalPayable,
                                                totalOverAdvance);

                                return savedHisab;

                        } catch (RuntimeException ex) {

                                reportGenerationFailure.increment();

                                logger.error(
                                                "Month report generation failed: ownerId={}, year={}, month={}, exception={}",
                                                ownerId,
                                                year,
                                                month,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        @Transactional
        public HisabDetail markEmployeeHisabCompleted(
                        Long detailId,
                        boolean completed) {

                return completionUpdateTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Toggling hisab completion status: ownerId={}, detailId={}, completed={}",
                                        ownerId,
                                        detailId,
                                        completed);

                        try {

                                HisabDetail detail = hisabDetailRepository
                                                .findByIdAndOwnerId(
                                                                detailId,
                                                                ownerId)
                                                .orElseThrow(() -> {

                                                        completionNotFound.increment();

                                                        logger.warn(
                                                                        "Hisab detail not found: ownerId={}, detailId={}",
                                                                        ownerId,
                                                                        detailId);

                                                        return new ResourceNotFoundException(
                                                                        "Hisab detail not found with id: "
                                                                                        + detailId);
                                                });

                                detail.setHisabCompleted(completed);

                                HisabDetail savedDetail = hisabDetailRepository.save(detail);

                                completionUpdate.increment();

                                logger.info(
                                                "Hisab completion status updated: ownerId={}, detailId={}, completed={}",
                                                ownerId,
                                                detailId,
                                                completed);

                                return savedDetail;

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Failed to update Hisab completion status: ownerId={}, detailId={}, exception={}",
                                                ownerId,
                                                detailId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        public List<Hisab> getAllMonthClosings() {

                return listTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Fetching month closing records: ownerId={}",
                                        ownerId);

                        List<Hisab> monthClosings = hisabRepository.findAllByOwnerId(ownerId);

                        logger.info(
                                        "Month closing records fetched: ownerId={}, count={}",
                                        ownerId,
                                        monthClosings.size());

                        return monthClosings;
                });
        }

        @Override
        public Hisab getMonthClosingById(Long id) {

                return lookupTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Fetching month closing: ownerId={}, id={}",
                                        ownerId,
                                        id);

                        try {

                                Hisab hisab = hisabRepository
                                                .findByIdAndOwnerId(id, ownerId)
                                                .orElseThrow(() -> {

                                                        lookupNotFound.increment();

                                                        logger.warn(
                                                                        "Hisab record not found: ownerId={}, id={}",
                                                                        ownerId,
                                                                        id);

                                                        return new ResourceNotFoundException(
                                                                        "Hisab record not found with id: "
                                                                                        + id);
                                                });

                                lookupSuccess.increment();

                                logger.info(
                                                "Hisab record fetched successfully: ownerId={}, id={}",
                                                ownerId,
                                                id);

                                return hisab;

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Failed to fetch Hisab record: ownerId={}, id={}, exception={}",
                                                ownerId,
                                                id,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        @Transactional
        public Hisab getMonthClosingByYearAndMonth(
                        int year,
                        int month) {

                logger.info(
                                "Fetching month closing by period: year={}, month={}",
                                year,
                                month);

                return generateMonthReport(
                                year,
                                month);
        }
}
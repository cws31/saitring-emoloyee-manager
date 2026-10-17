package com.sonuSaitring.sonuSaitringManagement.Attendance.service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceSummaryDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.BulkAttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.Hisab.service.HisabService;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

        private static final Logger logger = LoggerFactory.getLogger(AttendanceServiceImpl.class);

        private final AttendanceRepository attendanceRepository;
        private final EmployeeRepository employeeRepository;
        private final CurrentOwnerService currentOwnerService;
        private final HisabService hisabService;

        private final Counter attendanceMarked;
        private final Counter attendanceCreated;
        private final Counter attendanceUpdated;
        private final Counter employeeNotFound;
        private final Counter monthlyQueries;
        private final Counter employeeMonthlyQueries;
        private final Counter bulkAttendanceMarked;

        private final Timer markAttendanceTimer;
        private final Timer monthlyAttendanceTimer;
        private final Timer employeeMonthlyAttendanceTimer;
        private final Timer markBulkAttendanceTimer;

        public AttendanceServiceImpl(
                        AttendanceRepository attendanceRepository,
                        EmployeeRepository employeeRepository,
                        CurrentOwnerService currentOwnerService,
                        HisabService hisabService,
                        MeterRegistry meterRegistry) {

                this.attendanceRepository = attendanceRepository;
                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;
                this.hisabService = hisabService;

                this.attendanceMarked = Counter.builder("attendance.marked")
                                .description("Number of successfully marked attendance records")
                                .register(meterRegistry);

                this.attendanceCreated = Counter.builder("attendance.created")
                                .description("Number of newly created attendance records")
                                .register(meterRegistry);

                this.attendanceUpdated = Counter.builder("attendance.updated")
                                .description("Number of existing attendance records updated")
                                .register(meterRegistry);

                this.employeeNotFound = Counter.builder("attendance.employee_not_found")
                                .description("Number of attendance requests for unavailable employees")
                                .register(meterRegistry);

                this.monthlyQueries = Counter.builder("attendance.monthly_query")
                                .description("Number of monthly attendance queries")
                                .register(meterRegistry);

                this.employeeMonthlyQueries = Counter.builder("attendance.employee_monthly_query")
                                .description("Number of employee monthly attendance queries")
                                .register(meterRegistry);

                this.bulkAttendanceMarked = Counter.builder("attendance.bulk_marked")
                                .description("Number of successfully executed bulk attendance requests")
                                .register(meterRegistry);

                this.markAttendanceTimer = Timer.builder("attendance.mark.duration")
                                .description("Time taken to mark attendance")
                                .register(meterRegistry);

                this.monthlyAttendanceTimer = Timer.builder("attendance.monthly_query.duration")
                                .description("Time taken to retrieve monthly attendance")
                                .register(meterRegistry);

                this.employeeMonthlyAttendanceTimer = Timer.builder("attendance.employee_monthly_query.duration")
                                .description("Time taken to retrieve employee monthly attendance")
                                .register(meterRegistry);

                this.markBulkAttendanceTimer = Timer.builder("attendance.mark_bulk.duration")
                                .description("Time taken to execute bulk attendance marking")
                                .register(meterRegistry);
        }

        @Override
        public AttendanceResponseDTO markAttendance(
                        AttendanceRequestDTO requestDTO) {

                return markAttendanceTimer.record(() -> {
                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Marking attendance: ownerId={}, employeeId={}, date={}, status={}",
                                        ownerId,
                                        requestDTO.getEmployeeId(),
                                        requestDTO.getAttendanceDate(),
                                        requestDTO.getStatus());

                        Employee employee = employeeRepository
                                        .findByIdAndOwnerId(requestDTO.getEmployeeId(), ownerId)
                                        .orElseThrow(() -> {
                                                employeeNotFound.increment();

                                                return new ResourceNotFoundException("Employee not found.");
                                        });

                        validateEmployeeJoiningDate(
                                        employee,
                                        requestDTO.getAttendanceDate());

                        Optional<Attendance> existing = attendanceRepository
                                        .findByOwnerIdAndEmployeeIdAndAttendanceDate(
                                                        ownerId,
                                                        requestDTO.getEmployeeId(),
                                                        requestDTO.getAttendanceDate());

                        hisabService.validateAttendanceMutation(
                                        ownerId,
                                        requestDTO.getEmployeeId(),
                                        requestDTO.getAttendanceDate(),
                                        requestDTO.getReason());

                        String oldStatus = existing
                                        .map(a -> a.getStatus().name())
                                        .orElse(null);

                        Attendance attendance;

                        if (existing.isPresent()) {
                                attendance = existing.get();
                                attendance.setStatus(requestDTO.getStatus());
                                attendance.setReason(requestDTO.getReason());
                                attendanceUpdated.increment();
                        } else {
                                attendance = new Attendance();
                                attendance.setEmployee(employee);
                                attendance.setOwner(currentOwnerService.getCurrentOwner());
                                attendance.setAttendanceDate(requestDTO.getAttendanceDate());
                                attendance.setStatus(requestDTO.getStatus());
                                attendance.setReason(requestDTO.getReason());
                                attendanceCreated.increment();
                        }

                        Attendance savedAttendance = attendanceRepository.save(attendance);

                        Map<String, Object> oldValues = new LinkedHashMap<>();
                        oldValues.put("status", oldStatus);

                        Map<String, Object> newValues = new LinkedHashMap<>();
                        newValues.put("status", savedAttendance.getStatus().name());
                        newValues.put("attendanceDate", savedAttendance.getAttendanceDate());

                        hisabService.recordAttendanceChange(
                                        ownerId,
                                        savedAttendance.getEmployee().getId(),
                                        savedAttendance.getAttendanceDate(),
                                        existing.isPresent() ? "UPDATE" : "CREATE",
                                        requestDTO.getReason(),
                                        oldValues,
                                        newValues);

                        attendanceMarked.increment();

                        logger.info(
                                        "Attendance marked successfully: attendanceId={}, ownerId={}, employeeId={}, date={}, status={}",
                                        savedAttendance.getId(),
                                        ownerId,
                                        requestDTO.getEmployeeId(),
                                        requestDTO.getAttendanceDate(),
                                        savedAttendance.getStatus());

                        return mapToResponseDTO(savedAttendance);
                });
        }

        @Override
        @Transactional(readOnly = true)
        public List<AttendanceResponseDTO> getMonthlyAttendance(
                        int year,
                        int month) {

                return monthlyAttendanceTimer.record(() -> {
                        Long ownerId = currentOwnerService.getCurrentOwnerId();
                        monthlyQueries.increment();

                        LocalDate startDate = LocalDate.of(year, month, 1);
                        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                        List<AttendanceResponseDTO> result = attendanceRepository
                                        .findAllByOwnerIdAndMonth(ownerId, startDate, endDate)
                                        .stream()
                                        .filter(attendance -> {
                                                Employee employee = attendance.getEmployee();
                                                LocalDate joiningDate = employee.getJoiningDate();

                                                return joiningDate != null
                                                                && !attendance.getAttendanceDate()
                                                                                .isBefore(joiningDate);
                                        })
                                        .map(this::mapToResponseDTO)
                                        .collect(Collectors.toList());

                        logger.info(
                                        "Monthly attendance fetched: ownerId={}, year={}, month={}, records={}",
                                        ownerId,
                                        year,
                                        month,
                                        result.size());

                        return result;
                });
        }

        @Override
        @Transactional(readOnly = true)
        public List<AttendanceResponseDTO> getEmployeeMonthlyAttendance(
                        Long employeeId,
                        int year,
                        int month) {

                return employeeMonthlyAttendanceTimer.record(() -> {
                        Long ownerId = currentOwnerService.getCurrentOwnerId();
                        employeeMonthlyQueries.increment();

                        LocalDate startDate = LocalDate.of(year, month, 1);
                        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                        Employee employee = employeeRepository
                                        .findByIdAndOwnerId(employeeId, ownerId)
                                        .orElseThrow(() -> {
                                                employeeNotFound.increment();
                                                return new ResourceNotFoundException("Employee not found.");
                                        });

                        LocalDate joiningDate = employee.getJoiningDate();

                        // A month entirely before joining has no eligible attendance.
                        if (joiningDate != null && endDate.isBefore(joiningDate)) {
                                return Collections.emptyList();
                        }

                        List<AttendanceResponseDTO> result = attendanceRepository
                                        .findByOwnerIdAndEmployeeIdAndMonth(
                                                        ownerId,
                                                        employeeId,
                                                        startDate,
                                                        endDate)
                                        .stream()
                                        .filter(attendance -> joiningDate == null
                                                        || !attendance.getAttendanceDate().isBefore(joiningDate))
                                        .map(this::mapToResponseDTO)
                                        .collect(Collectors.toList());

                        logger.info(
                                        "Employee monthly attendance fetched: ownerId={}, employeeId={}, year={}, month={}, records={}",
                                        ownerId,
                                        employeeId,
                                        year,
                                        month,
                                        result.size());

                        return result;
                });
        }

        @Override
        @Transactional
        public List<AttendanceResponseDTO> markBulkAttendance(
                        BulkAttendanceRequestDTO requestDTO) {

                return markBulkAttendanceTimer.record(() -> {
                        Owner currentOwner = currentOwnerService.getCurrentOwner();
                        Long ownerId = currentOwner.getId();

                        LocalDate attendanceDate = requestDTO.getAttendanceDate();

                        List<Employee> employees = employeeRepository.findAllByOwnerIdAndBlockedFalse(ownerId);

                        if (employees.isEmpty()) {
                                logger.info("Bulk attendance skipped: no active employees, ownerId={}", ownerId);
                                return Collections.emptyList();
                        }

                        // Include only employees who have actually joined by the selected date.
                        List<Employee> eligibleEmployees = employees.stream()
                                        .filter(employee -> employee.getJoiningDate() != null)
                                        .filter(employee -> !attendanceDate.isBefore(employee.getJoiningDate()))
                                        .collect(Collectors.toList());

                        int skippedEmployees = employees.size() - eligibleEmployees.size();

                        if (eligibleEmployees.isEmpty()) {
                                logger.info(
                                                "Bulk attendance skipped: no eligible employees, ownerId={}, date={}, skipped={}",
                                                ownerId,
                                                attendanceDate,
                                                skippedEmployees);
                                return Collections.emptyList();
                        }

                        logger.info(
                                        "Bulk attendance started: ownerId={}, date={}, eligible={}, skipped={}",
                                        ownerId,
                                        attendanceDate,
                                        eligibleEmployees.size(),
                                        skippedEmployees);

                        // Validate all eligible employees before modifying attendance records.
                        for (Employee employee : eligibleEmployees) {
                                hisabService.validateAttendanceMutation(
                                                ownerId,
                                                employee.getId(),
                                                attendanceDate,
                                                requestDTO.getReason());
                        }

                        List<Attendance> attendancesToSave = new ArrayList<>();
                        Map<Long, String> oldStatuses = new HashMap<>();

                        for (Employee employee : eligibleEmployees) {
                                Optional<Attendance> existing = attendanceRepository
                                                .findByOwnerIdAndEmployeeIdAndAttendanceDate(
                                                                ownerId,
                                                                employee.getId(),
                                                                attendanceDate);

                                Attendance attendance;

                                if (existing.isPresent()) {
                                        attendance = existing.get();

                                        oldStatuses.put(
                                                        employee.getId(),
                                                        attendance.getStatus().name());

                                        attendance.setStatus(requestDTO.getStatus());
                                        attendance.setReason(requestDTO.getReason());

                                        attendanceUpdated.increment();
                                } else {
                                        attendance = new Attendance();
                                        attendance.setEmployee(employee);
                                        attendance.setOwner(currentOwner);
                                        attendance.setAttendanceDate(attendanceDate);
                                        attendance.setStatus(requestDTO.getStatus());
                                        attendance.setReason(requestDTO.getReason());

                                        attendanceCreated.increment();
                                }

                                attendancesToSave.add(attendance);
                        }

                        List<Attendance> savedAttendances = attendanceRepository.saveAll(attendancesToSave);

                        for (Attendance saved : savedAttendances) {
                                Long employeeId = saved.getEmployee().getId();

                                Map<String, Object> oldValues = new LinkedHashMap<>();
                                oldValues.put("status", oldStatuses.get(employeeId));

                                Map<String, Object> newValues = new LinkedHashMap<>();
                                newValues.put("status", saved.getStatus().name());
                                newValues.put("attendanceDate", saved.getAttendanceDate());

                                hisabService.recordAttendanceChange(
                                                ownerId,
                                                employeeId,
                                                saved.getAttendanceDate(),
                                                oldStatuses.containsKey(employeeId) ? "UPDATE" : "CREATE",
                                                requestDTO.getReason(),
                                                oldValues,
                                                newValues);
                        }

                        bulkAttendanceMarked.increment();

                        logger.info(
                                        "Bulk attendance completed: ownerId={}, date={}, saved={}, skipped={}",
                                        ownerId,
                                        attendanceDate,
                                        savedAttendances.size(),
                                        skippedEmployees);

                        return savedAttendances.stream()
                                        .map(this::mapToResponseDTO)
                                        .collect(Collectors.toList());
                });
        }

        @Override
        @Transactional
        public int deleteAttendance(Long attendanceId, String reason) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();

                Attendance existing = attendanceRepository
                                .findByIdAndOwnerId(attendanceId, ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found."));

                LocalDate date = existing.getAttendanceDate();
                Long employeeId = existing.getEmployee().getId();

                hisabService.validateAttendanceMutation(ownerId, employeeId, date, reason);

                Map<String, Object> oldValues = new LinkedHashMap<>();
                oldValues.put("status", existing.getStatus().name());
                oldValues.put("attendanceDate", date);

                int deleted = attendanceRepository.deleteByIdAndOwnerId(attendanceId, ownerId);

                if (deleted == 0) {
                        throw new ResourceNotFoundException("Attendance record not found.");
                }

                hisabService.recordAttendanceChange(
                                ownerId, employeeId, date, "DELETE", reason, oldValues, null);

                return deleted;
        }

        @Override
        @Transactional
        public int deleteEmployeeAttendance(
                        Long employeeId,
                        LocalDate date,
                        String reason) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                employeeRepository.findByIdAndOwnerId(employeeId, ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

                Attendance existing = attendanceRepository
                                .findByOwnerIdAndEmployeeIdAndAttendanceDate(ownerId, employeeId, date)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Attendance record not found for this employee on the selected date."));

                hisabService.validateAttendanceMutation(ownerId, employeeId, date, reason);

                Map<String, Object> oldValues = new LinkedHashMap<>();
                oldValues.put("status", existing.getStatus().name());
                oldValues.put("attendanceDate", date);

                int deleted = attendanceRepository
                                .deleteByEmployeeIdAndOwnerIdAndDate(employeeId, ownerId, date);

                if (deleted == 0) {
                        throw new ResourceNotFoundException("Attendance record not found.");
                }

                hisabService.recordAttendanceChange(
                                ownerId, employeeId, date, "DELETE", reason, oldValues, null);

                return deleted;
        }

        @Override
        @Transactional
        public int deleteBulkAttendance(LocalDate date, String reason) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();

                List<Attendance> existingRows = attendanceRepository.findAllByOwnerIdAndMonth(ownerId, date, date);

                for (Attendance row : existingRows) {
                        hisabService.validateAttendanceMutation(
                                        ownerId, row.getEmployee().getId(), date, reason);
                }

                int deleted = attendanceRepository.deleteAllByOwnerIdAndDate(ownerId, date);

                for (Attendance row : existingRows) {
                        Map<String, Object> oldValues = new LinkedHashMap<>();
                        oldValues.put("status", row.getStatus().name());
                        oldValues.put("attendanceDate", date);

                        hisabService.recordAttendanceChange(
                                        ownerId,
                                        row.getEmployee().getId(),
                                        date,
                                        "DELETE",
                                        reason,
                                        oldValues,
                                        null);
                }

                return deleted;
        }

        @Override
        @Transactional(readOnly = true)
        public AttendanceSummaryDTO getDailyAttendanceSummary(LocalDate date) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();

                AttendanceSummaryDTO summary = attendanceRepository.findDailySummaryByOwnerAndDate(ownerId, date);

                if (summary == null) {
                        return new AttendanceSummaryDTO(date, 0, 0, 0);
                }

                return summary;
        }

        @Override
        @Transactional(readOnly = true)
        public List<AttendanceSummaryDTO> getMonthlyAttendanceSummary(
                        int year,
                        int month) {

                if (month < 1 || month > 12) {
                        throw new IllegalArgumentException(
                                        "Invalid month provided: " + month
                                                        + ". Month must be between 1 and 12.");
                }

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                List<AttendanceSummaryDTO> dbResults = attendanceRepository.findMonthlySummaryByOwnerAndDateRange(
                                ownerId, startDate, endDate);

                Map<LocalDate, AttendanceSummaryDTO> resultMap = dbResults.stream()
                                .collect(Collectors.toMap(
                                                AttendanceSummaryDTO::getAttendanceDate,
                                                Function.identity()));

                List<AttendanceSummaryDTO> completeMonthlyList = new ArrayList<>();
                LocalDate currentDay = startDate;

                while (!currentDay.isAfter(endDate)) {
                        AttendanceSummaryDTO daySummary = resultMap.getOrDefault(
                                        currentDay,
                                        new AttendanceSummaryDTO(currentDay, 0, 0, 0));

                        completeMonthlyList.add(daySummary);
                        currentDay = currentDay.plusDays(1);
                }

                return completeMonthlyList;
        }

        private void validateEmployeeJoiningDate(
                        Employee employee,
                        LocalDate attendanceDate) {

                LocalDate joiningDate = employee.getJoiningDate();

                if (joiningDate == null) {
                        throw new IllegalArgumentException(
                                        "Joining date is not set for employee: "
                                                        + employee.getName()
                                                        + ". Please update the employee's joining date first.");
                }

                if (attendanceDate.isBefore(joiningDate)) {
                        throw new IllegalArgumentException(
                                        "Attendance cannot be marked before the employee's joining date: "
                                                        + joiningDate);
                }
        }

        private AttendanceResponseDTO mapToResponseDTO(Attendance attendance) {
                Employee employee = attendance.getEmployee();

                return AttendanceResponseDTO.builder()
                                .id(attendance.getId())
                                .attendanceDate(attendance.getAttendanceDate())
                                .status(attendance.getStatus())
                                .reason(attendance.getReason())
                                .createdAt(attendance.getCreatedAt())
                                .employeeId(employee != null ? employee.getId() : null)
                                .employeeName(employee != null ? employee.getName() : null)
                                .employeeMobile(employee != null ? employee.getMobile() : null)
                                .build();
        }
}

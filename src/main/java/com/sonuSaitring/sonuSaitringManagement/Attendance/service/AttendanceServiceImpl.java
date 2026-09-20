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
                MeterRegistry meterRegistry) {

                this.attendanceRepository = attendanceRepository;
                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;

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
                                .findByIdAndOwnerId(
                                        requestDTO.getEmployeeId(),
                                        ownerId)
                                .orElseThrow(() -> {

                                        employeeNotFound.increment();

                                        logger.warn(
                                                "Attendance rejected because employee was not found: ownerId={}, employeeId={}",
                                                ownerId,
                                                requestDTO.getEmployeeId());

                                        return new ResourceNotFoundException(
                                                "Employee not found.");
                                });

                        Optional<Attendance> existing = attendanceRepository
                                .findByOwnerIdAndEmployeeIdAndAttendanceDate(
                                        ownerId,
                                        requestDTO.getEmployeeId(),
                                        requestDTO.getAttendanceDate());

                        Attendance attendance;

                        if (existing.isPresent()) {

                                attendance = existing.get();

                                attendance.setStatus(requestDTO.getStatus());
                                attendance.setReason(requestDTO.getReason());

                                attendanceUpdated.increment();

                                logger.debug(
                                        "Existing attendance updated: attendanceId={}, ownerId={}, employeeId={}, date={}",
                                        attendance.getId(),
                                        ownerId,
                                        requestDTO.getEmployeeId(),
                                        requestDTO.getAttendanceDate());

                        } else {

                                attendance = new Attendance();

                                attendance.setEmployee(employee);
                                attendance.setOwner(currentOwnerService.getCurrentOwner());
                                attendance.setAttendanceDate(requestDTO.getAttendanceDate());
                                attendance.setStatus(requestDTO.getStatus());
                                attendance.setReason(requestDTO.getReason());

                                attendanceCreated.increment();

                                logger.debug(
                                        "New attendance record created: ownerId={}, employeeId={}, date={}",
                                        ownerId,
                                        requestDTO.getEmployeeId(),
                                        requestDTO.getAttendanceDate());
                        }

                        Attendance savedAttendance = attendanceRepository.save(attendance);

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

                        logger.info(
                                "Fetching monthly attendance: ownerId={}, year={}, month={}, startDate={}, endDate={}",
                                ownerId,
                                year,
                                month,
                                startDate,
                                endDate);

                        List<AttendanceResponseDTO> result = attendanceRepository
                                .findAllByOwnerIdAndMonth(
                                        ownerId,
                                        startDate,
                                        endDate)
                                .stream()
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());

                        logger.info(
                                "Monthly attendance fetched successfully: ownerId={}, year={}, month={}, records={}",
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

                        logger.info(
                                "Fetching employee monthly attendance: ownerId={}, employeeId={}, year={}, month={}",
                                ownerId,
                                employeeId,
                                year,
                                month);

                        employeeRepository
                                .findByIdAndOwnerId(employeeId, ownerId)
                                .orElseThrow(() -> {

                                        employeeNotFound.increment();

                                        logger.warn(
                                                "Employee attendance lookup rejected because employee was not found: ownerId={}, employeeId={}",
                                                ownerId,
                                                employeeId);

                                        return new ResourceNotFoundException(
                                                "Employee not found.");
                                });

                        List<AttendanceResponseDTO> result = attendanceRepository
                                .findByOwnerIdAndEmployeeIdAndMonth(
                                        ownerId,
                                        employeeId,
                                        startDate,
                                        endDate)
                                .stream()
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());

                        logger.info(
                                "Employee monthly attendance fetched successfully: ownerId={}, employeeId={}, year={}, month={}, records={}",
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
        public List<AttendanceResponseDTO> markBulkAttendance(BulkAttendanceRequestDTO requestDTO) {
                return markBulkAttendanceTimer.record(() -> {
                        Owner currentOwner = currentOwnerService.getCurrentOwner();
                        Long ownerId = currentOwner.getId();

                        logger.info(
                                "Marking bulk attendance: ownerId={}, date={}, status={}",
                                ownerId,
                                requestDTO.getAttendanceDate(),
                                requestDTO.getStatus());

                        List<Employee> employees = employeeRepository.findAllByOwnerId(currentOwner.getId());
                        if (employees.isEmpty()) {
                                logger.info("No employees found for bulk attendance: ownerId={}", ownerId);
                                return Collections.emptyList();
                        }

                        List<Attendance> attendancesToSave = new ArrayList<>();

                        for (Employee employee : employees) {
                                Optional<Attendance> existing = attendanceRepository
                                        .findByOwnerIdAndEmployeeIdAndAttendanceDate(
                                                ownerId,
                                                employee.getId(),
                                                requestDTO.getAttendanceDate()
                                        );

                                Attendance attendance;

                                if (existing.isPresent()) {
                                        attendance = existing.get();
                                        attendance.setStatus(requestDTO.getStatus());
                                        attendance.setReason(requestDTO.getReason());
                                        attendanceUpdated.increment();
                                } else {
                                        attendance = new Attendance();
                                        attendance.setEmployee(employee);
                                        attendance.setOwner(currentOwner);
                                        attendance.setAttendanceDate(requestDTO.getAttendanceDate());
                                        attendance.setStatus(requestDTO.getStatus());
                                        attendance.setReason(requestDTO.getReason());
                                        attendanceCreated.increment();
                                }

                                attendancesToSave.add(attendance);
                        }

                        List<Attendance> savedAttendances = attendanceRepository.saveAll(attendancesToSave);

                        bulkAttendanceMarked.increment();

                        logger.info(
                                "Bulk attendance marked successfully: ownerId={}, date={}, totalRecords={}",
                                ownerId,
                                requestDTO.getAttendanceDate(),
                                savedAttendances.size());

                        return savedAttendances.stream()
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());
                });
        }

        @Override
        @Transactional(readOnly = true)
        public AttendanceSummaryDTO getDailyAttendanceSummary(LocalDate date) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                logger.debug("Fetching daily attendance summary for ownerId: {} on date: {}", ownerId, date);

                AttendanceSummaryDTO summary = attendanceRepository.findDailySummaryByOwnerAndDate(ownerId, date);

                if (summary == null) {
                        return new AttendanceSummaryDTO(date, 0, 0, 0);
                }
                return summary;
        }

        @Override
        @Transactional(readOnly = true)
        public List<AttendanceSummaryDTO> getMonthlyAttendanceSummary(int year, int month) {
                if (month < 1 || month > 12) {
                        throw new IllegalArgumentException("Invalid month provided: " + month + ". Month must be between 1 and 12.");
                }

                Long ownerId = currentOwnerService.getCurrentOwnerId();
                logger.debug("Fetching monthly attendance summary for ownerId: {} for year: {}, month: {}", ownerId, year, month);

                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

                List<AttendanceSummaryDTO> dbResults = attendanceRepository.findMonthlySummaryByOwnerAndDateRange(ownerId, startDate, endDate);

                Map<LocalDate, AttendanceSummaryDTO> resultMap = dbResults.stream()
                        .collect(Collectors.toMap(AttendanceSummaryDTO::getAttendanceDate, Function.identity()));

                List<AttendanceSummaryDTO> completeMonthlyList = new ArrayList<>();
                LocalDate currentDay = startDate;

                while (!currentDay.isAfter(endDate)) {
                        AttendanceSummaryDTO daySummary = resultMap.getOrDefault(currentDay,
                                new AttendanceSummaryDTO(currentDay, 0, 0, 0));
                        completeMonthlyList.add(daySummary);
                        currentDay = currentDay.plusDays(1);
                }

                return completeMonthlyList;
        }

        private AttendanceResponseDTO mapToResponseDTO(
                Attendance attendance) {

                Employee emp = attendance.getEmployee();

                return AttendanceResponseDTO.builder()
                        .id(attendance.getId())
                        .attendanceDate(attendance.getAttendanceDate())
                        .status(attendance.getStatus())
                        .reason(attendance.getReason())
                        .createdAt(attendance.getCreatedAt())
                        .employeeId(emp != null ? emp.getId() : null)
                        .employeeName(emp != null ? emp.getName() : null)
                        .employeeMobile(emp != null ? emp.getMobile() : null)
                        .build();
        }
}
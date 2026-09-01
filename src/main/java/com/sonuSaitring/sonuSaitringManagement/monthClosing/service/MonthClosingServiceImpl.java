package com.sonuSaitring.sonuSaitringManagement.monthClosing.service;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;
import com.sonuSaitring.sonuSaitringManagement.Attendance.repository.AttendanceRepository;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.monthClosing.dto.MonthClosingRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.monthClosing.entity.MonthClosing;
import com.sonuSaitring.sonuSaitringManagement.monthClosing.entity.MonthClosingDetail;
import com.sonuSaitring.sonuSaitringManagement.monthClosing.repository.MonthClosingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MonthClosingServiceImpl implements MonthClosingService {

    @Autowired
    private MonthClosingRepository monthClosingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeAdvanceRepository advanceRepository;

    /**
     * Helper method to compute calculations dynamically without saving to DB (used
     * for preview)
     */
    private MonthClosing buildMonthClosingObject(MonthClosingRequestDTO requestDTO) {
        LocalDate startDate = LocalDate.of(requestDTO.getYear(), requestDTO.getMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Employee> activeEmployees = employeeRepository.findAll();

        MonthClosing monthClosing = new MonthClosing();
        monthClosing.setYear(requestDTO.getYear());
        monthClosing.setMonth(requestDTO.getMonth());
        monthClosing.setClosedAt(null); // Null indicates it's a preview/un-settled state

        List<MonthClosingDetail> details = new ArrayList<>();
        Map<Long, BigDecimal> extraMoneyMap = requestDTO.getEmployeeExtraMoneyMap() != null
                ? requestDTO.getEmployeeExtraMoneyMap()
                : Map.of();

        for (Employee employee : activeEmployees) {
            // 1. Calculate Total Presences (Present = 1.0, Half = 0.5) handling Enum status
            // safely
            List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndMonth(employee.getId(), startDate,
                    endDate);
            BigDecimal totalPresences = BigDecimal.ZERO;

            for (Attendance attendance : attendances) {
                String statusStr = attendance.getStatus() != null ? attendance.getStatus().name() : "";
                if ("PRESENT".equalsIgnoreCase(statusStr)) {
                    totalPresences = totalPresences.add(BigDecimal.ONE);
                } else if ("HALF_DAY".equalsIgnoreCase(statusStr)) {
                    totalPresences = totalPresences.add(new BigDecimal("0.5"));
                }
            }

            // 2. Calculate Total Advance for the month
            List<EmployeeAdvance> advances = advanceRepository.findByEmployeeIdAndPaymentDateBetween(employee.getId(),
                    startDate, endDate);
            Double totalAdvanceDouble = advances.stream()
                    .map(EmployeeAdvance::getAmount)
                    .filter(amt -> amt != null)
                    .reduce(0.0, Double::sum);
            BigDecimal totalAdvance = BigDecimal.valueOf(totalAdvanceDouble);

            // 3. Rate & Total Earning
            BigDecimal rate = employee.getInitialRate() != null ? employee.getInitialRate() : BigDecimal.ZERO;
            BigDecimal totalEarning = totalPresences.multiply(rate);

            // 4. Extra Money (+ or -)
            BigDecimal extraMoney = extraMoneyMap.getOrDefault(employee.getId(), BigDecimal.ZERO);

            // 5. Net Payable
            BigDecimal netPayable = totalEarning.subtract(totalAdvance).add(extraMoney);

            // Create Detail record
            MonthClosingDetail detail = new MonthClosingDetail();
            detail.setMonthClosing(monthClosing);
            detail.setEmployee(employee);
            detail.setEmployeeName(employee.getName());
            detail.setTotalPresences(totalPresences);
            detail.setRate(rate);
            detail.setTotalEarning(totalEarning);
            detail.setTotalAdvance(totalAdvance);
            detail.setExtraMoney(extraMoney);
            detail.setNetPayable(netPayable);

            details.add(detail);
        }

        monthClosing.setDetails(details);
        return monthClosing;
    }

    @Override
    public MonthClosing previewMonthClosing(MonthClosingRequestDTO requestDTO) {
        // Just calculate and return without database insert checks or saving
        return buildMonthClosingObject(requestDTO);
    }

    @Override
    @Transactional
    public MonthClosing closeMonth(MonthClosingRequestDTO requestDTO) {
        if (monthClosingRepository.existsByYearAndMonth(requestDTO.getYear(), requestDTO.getMonth())) {
            throw new IllegalStateException(
                    "Month closing for " + requestDTO.getMonth() + "/" + requestDTO.getYear() + " already exists.");
        }

        // Build object and stamp closing time for final settlement record
        MonthClosing monthClosing = buildMonthClosingObject(requestDTO);
        monthClosing.setClosedAt(LocalDateTime.now());

        return monthClosingRepository.save(monthClosing);
    }

    @Override
    public List<MonthClosing> getAllMonthClosings() {
        return monthClosingRepository.findAll();
    }

    @Override
    public MonthClosing getMonthClosingById(Long id) {
        return monthClosingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Month closing report not found with id: " + id));
    }

    @Override
    public MonthClosing getMonthClosingByYearAndMonth(int year, int month) {
        return monthClosingRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new RuntimeException("Month closing report not found for " + month + "/" + year));
    }
}
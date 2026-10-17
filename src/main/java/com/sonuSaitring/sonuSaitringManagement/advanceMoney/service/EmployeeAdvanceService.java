package com.sonuSaitring.sonuSaitringManagement.advanceMoney.service;

import com.sonuSaitring.sonuSaitringManagement.Hisab.service.HisabService;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.dto.AdvanceResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.entity.EmployeeAdvance;
import com.sonuSaitring.sonuSaitringManagement.advanceMoney.repository.EmployeeAdvanceRepository;
import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@Transactional
public class EmployeeAdvanceService {

        private final EmployeeAdvanceRepository advanceRepository;
        private final EmployeeRepository employeeRepository;
        private final CurrentOwnerService currentOwnerService;
        private final HisabService hisabService;

        public EmployeeAdvanceService(
                        EmployeeAdvanceRepository advanceRepository,
                        EmployeeRepository employeeRepository,
                        CurrentOwnerService currentOwnerService,
                        HisabService hisabService) {

                this.advanceRepository = advanceRepository;
                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;
                this.hisabService = hisabService;
        }

        public AdvanceResponseDTO create(AdvanceRequestDTO request) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                Employee employee = employeeRepository.findByIdAndOwnerId(request.getEmployeeId(), ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

                requireReasonIfClosed(ownerId, request.getPaymentDate(), request.getReason());

                EmployeeAdvance advance = new EmployeeAdvance();
                advance.setOwner(currentOwnerService.getCurrentOwner());
                advance.setEmployee(employee);
                advance.setAmount(request.getAmount());
                advance.setPaymentDate(request.getPaymentDate());
                advance.setNote(request.getNote());

                EmployeeAdvance saved = advanceRepository.save(advance);

                hisabService.recalculateEmployeeFrom(ownerId, employee.getId(), saved.getPaymentDate(),
                                "Advance created: " + reasonOrDefault(request.getReason()));

                return toResponse(saved, runningTotal(saved));
        }

        @Transactional(readOnly = true)
        public List<AdvanceResponseDTO> list(Integer year, Integer month, Long employeeId, LocalDate fromDate,
                        LocalDate toDate) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                LocalDate from = fromDate;
                LocalDate to = toDate;

                if (year != null || month != null) {
                        if (year == null || month == null || month < 1 || month > 12) {
                                throw new BadRequestException("Provide a valid year and month together.");
                        }
                        from = YearMonth.of(year, month).atDay(1);
                        to = YearMonth.of(year, month).atEndOfMonth();
                }

                if (from == null)
                        from = LocalDate.of(2000, 1, 1);
                if (to == null)
                        to = LocalDate.of(2200, 12, 31);
                if (to.isBefore(from))
                        throw new BadRequestException("toDate must be on or after fromDate.");

                List<EmployeeAdvance> records = advanceRepository
                                .findByOwnerIdAndPaymentDateBetweenOrderByPaymentDateAscIdAsc(ownerId, from, to);
                if (employeeId != null) {
                        records = records.stream().filter(a -> a.getEmployee().getId().equals(employeeId)).toList();
                }

                Map<Long, BigDecimal> totals = new HashMap<>();
                Map<Long, YearMonth> months = new HashMap<>();
                List<AdvanceResponseDTO> response = new ArrayList<>();

                for (EmployeeAdvance row : records) {
                        Long id = row.getEmployee().getId();
                        YearMonth period = YearMonth.from(row.getPaymentDate());
                        if (!period.equals(months.get(id))) {
                                totals.put(id, BigDecimal.ZERO);
                                months.put(id, period);
                        }
                        totals.merge(id, row.getAmount(), BigDecimal::add);
                        response.add(toResponse(row, totals.get(id)));
                }

                return response;
        }

        @Transactional(readOnly = true)
        public AdvanceResponseDTO get(Long id) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                EmployeeAdvance row = advanceRepository.findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Advance record not found."));
                return toResponse(row, runningTotal(row));
        }

        public AdvanceResponseDTO update(Long id, AdvanceRequestDTO request) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                EmployeeAdvance row = advanceRepository.findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Advance record not found."));

                LocalDate oldDate = row.getPaymentDate();
                Long oldEmployeeId = row.getEmployee().getId();

                boolean closed = hisabService.isClosed(ownerId, oldDate)
                                || hisabService.isClosed(ownerId, request.getPaymentDate());
                if (closed && (request.getReason() == null || request.getReason().isBlank())) {
                        throw new BadRequestException("A reason is required to change an advance in a closed month.");
                }

                Employee employee = employeeRepository.findByIdAndOwnerId(request.getEmployeeId(), ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

                row.setEmployee(employee);
                row.setAmount(request.getAmount());
                row.setPaymentDate(request.getPaymentDate());
                row.setNote(request.getNote());

                EmployeeAdvance saved = advanceRepository.save(row);

                hisabService.recalculateEmployeeFrom(ownerId, oldEmployeeId, oldDate,
                                "Advance updated: " + reasonOrDefault(request.getReason()));
                hisabService.recalculateEmployeeFrom(ownerId, employee.getId(), saved.getPaymentDate(),
                                "Advance updated: " + reasonOrDefault(request.getReason()));

                return toResponse(saved, runningTotal(saved));
        }

        public void delete(Long id, String reason) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                EmployeeAdvance row = advanceRepository.findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Advance record not found."));

                requireReasonIfClosed(ownerId, row.getPaymentDate(), reason);

                Long employeeId = row.getEmployee().getId();
                LocalDate date = row.getPaymentDate();

                advanceRepository.delete(row);

                hisabService.recalculateEmployeeFrom(ownerId, employeeId, date,
                                "Advance deleted: " + reasonOrDefault(reason));
        }

        private void requireReasonIfClosed(Long ownerId, LocalDate date, String reason) {
                if (hisabService.isClosed(ownerId, date) && (reason == null || reason.isBlank())) {
                        throw new BadRequestException("A reason is required to change a closed month.");
                }
        }

        private String reasonOrDefault(String reason) {
                return reason == null || reason.isBlank() ? "Advance transaction changed" : reason;
        }

        private BigDecimal runningTotal(EmployeeAdvance a) {
                YearMonth ym = YearMonth.from(a.getPaymentDate());
                return advanceRepository.findByOwnerIdAndEmployeeIdAndPaymentDateBetween(
                                a.getOwner().getId(), a.getEmployee().getId(), ym.atDay(1), a.getPaymentDate())
                                .stream()
                                .map(EmployeeAdvance::getAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private AdvanceResponseDTO toResponse(EmployeeAdvance a, BigDecimal total) {
                return new AdvanceResponseDTO(
                                a.getId(),
                                a.getEmployee().getId(),
                                a.getEmployee().getName(),
                                a.getAmount(),
                                a.getPaymentDate(),
                                a.getNote(),
                                total,
                                a.getCreatedAt(),
                                a.getUpdatedAt());
        }
}
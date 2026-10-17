package com.sonuSaitring.sonuSaitringManagement.sattlement.service;

import com.sonuSaitring.sonuSaitringManagement.Hisab.entity.HisabDetail;
import com.sonuSaitring.sonuSaitringManagement.Hisab.service.HisabService;
import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.SettlementRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.dto.SettlementResponseDTO;
import com.sonuSaitring.sonuSaitringManagement.sattlement.entity.EmployeeSettlement;
import com.sonuSaitring.sonuSaitringManagement.sattlement.repository.EmployeeSettlementRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@Transactional
public class EmployeeSettlementService {

        private final EmployeeSettlementRepository settlementRepository;
        private final EmployeeRepository employeeRepository;
        private final CurrentOwnerService currentOwnerService;
        private final HisabService hisabService;

        public EmployeeSettlementService(
                        EmployeeSettlementRepository settlementRepository,
                        EmployeeRepository employeeRepository,
                        CurrentOwnerService currentOwnerService,
                        HisabService hisabService) {

                this.settlementRepository = settlementRepository;
                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;
                this.hisabService = hisabService;
        }

        public SettlementResponseDTO create(SettlementRequestDTO request) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                validatePeriod(request);
                validateDate(request.getSettlementDate(), request.getYear(), request.getMonth());
                requireReasonIfClosed(ownerId, request.getSettlementDate(), request.getReason());

                Employee employee = employeeRepository.findByIdAndOwnerId(request.getEmployeeId(), ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

                HisabDetail detail = hisabService.getOrCreateDetail(ownerId, employee.getId(), request.getYear(),
                                request.getMonth());

                EmployeeSettlement settlement = new EmployeeSettlement();
                settlement.setOwner(currentOwnerService.getCurrentOwner());
                settlement.setEmployee(employee);
                settlement.setHisabDetail(detail);
                settlement.setAmountPaid(request.getAmountPaid());
                settlement.setSettlementDate(request.getSettlementDate());
                settlement.setNote(request.getNote());

                EmployeeSettlement saved = settlementRepository.save(settlement);

                hisabService.recalculateEmployeeFrom(ownerId, employee.getId(), request.getSettlementDate(),
                                "Settlement created: " + reasonOrDefault(request.getReason()));

                return toResponse(saved);
        }

        @Transactional(readOnly = true)
        public List<SettlementResponseDTO> list(Integer year, Integer month, Long employeeId, LocalDate fromDate,
                        LocalDate toDate) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                LocalDate from = fromDate == null ? LocalDate.of(2000, 1, 1) : fromDate;
                LocalDate to = toDate == null ? LocalDate.of(2200, 12, 31) : toDate;

                if (year != null || month != null) {
                        if (year == null || month == null || month < 1 || month > 12) {
                                throw new BadRequestException("Provide a valid year and month together.");
                        }
                        from = YearMonth.of(year, month).atDay(1);
                        to = YearMonth.of(year, month).atEndOfMonth();
                }

                if (to.isBefore(from))
                        throw new BadRequestException("toDate must be on or after fromDate.");

                List<EmployeeSettlement> records = settlementRepository
                                .findByOwnerIdAndSettlementDateBetweenOrderBySettlementDateAscIdAsc(ownerId, from, to);
                if (employeeId != null) {
                        records = records.stream().filter(s -> s.getEmployee().getId().equals(employeeId)).toList();
                }

                return records.stream().map(this::toResponse).toList();
        }

        @Transactional(readOnly = true)
        public SettlementResponseDTO get(Long id) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                return toResponse(settlementRepository.findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Settlement record not found.")));
        }

        public SettlementResponseDTO update(Long id, SettlementRequestDTO request) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                validatePeriod(request);
                validateDate(request.getSettlementDate(), request.getYear(), request.getMonth());

                EmployeeSettlement row = settlementRepository.findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Settlement record not found."));

                LocalDate oldDate = row.getSettlementDate();
                Long oldEmployeeId = row.getEmployee().getId();

                boolean closed = (oldDate != null && hisabService.isClosed(ownerId, oldDate))
                                || hisabService.isClosed(ownerId, request.getSettlementDate());
                if (closed && (request.getReason() == null || request.getReason().isBlank())) {
                        throw new BadRequestException("A reason is required to change a settlement in a closed month.");
                }

                Employee employee = employeeRepository.findByIdAndOwnerId(request.getEmployeeId(), ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));

                HisabDetail detail = hisabService.getOrCreateDetail(ownerId, employee.getId(), request.getYear(),
                                request.getMonth());

                row.setEmployee(employee);
                row.setHisabDetail(detail);
                row.setAmountPaid(request.getAmountPaid());
                row.setSettlementDate(request.getSettlementDate());
                row.setNote(request.getNote());

                EmployeeSettlement saved = settlementRepository.save(row);

                if (oldDate != null) {
                        hisabService.recalculateEmployeeFrom(ownerId, oldEmployeeId, oldDate,
                                        "Settlement updated: " + reasonOrDefault(request.getReason()));
                }
                hisabService.recalculateEmployeeFrom(ownerId, employee.getId(), request.getSettlementDate(),
                                "Settlement updated: " + reasonOrDefault(request.getReason()));

                return toResponse(saved);
        }

        public void delete(Long id, String reason) {
                Long ownerId = currentOwnerService.getCurrentOwnerId();
                EmployeeSettlement row = settlementRepository.findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Settlement record not found."));

                if (row.getSettlementDate() != null) {
                        requireReasonIfClosed(ownerId, row.getSettlementDate(), reason);
                } else if (row.getHisabDetail() != null
                                && row.getHisabDetail().getHisab()
                                                .getStatus() == com.sonuSaitring.sonuSaitringManagement.Hisab.entity.Hisab.Status.CLOSED
                                && (reason == null || reason.isBlank())) {
                        throw new BadRequestException("A reason is required to change a settlement in a closed month.");
                }

                Long employeeId = row.getEmployee().getId();
                LocalDate date = row.getSettlementDate();
                if (date == null && row.getHisabDetail() != null) {
                        date = YearMonth.of(row.getHisabDetail().getHisab().getYear(),
                                        row.getHisabDetail().getHisab().getMonth())
                                        .atDay(1);
                }

                settlementRepository.delete(row);

                if (date != null) {
                        hisabService.recalculateEmployeeFrom(ownerId, employeeId, date,
                                        "Settlement deleted: " + reasonOrDefault(reason));
                }
        }

        private void validatePeriod(SettlementRequestDTO r) {
                if (r.getYear() < 2000 || r.getYear() > 2200 || r.getMonth() < 1 || r.getMonth() > 12) {
                        throw new BadRequestException("Invalid year or month.");
                }
        }

        private void validateDate(LocalDate date, int year, int month) {
                if (date.getYear() != year || date.getMonthValue() != month) {
                        throw new BadRequestException("Settlement date must be inside the selected Hisab month.");
                }
        }

        private void requireReasonIfClosed(Long ownerId, LocalDate date, String reason) {
                if (date != null && hisabService.isClosed(ownerId, date) && (reason == null || reason.isBlank())) {
                        throw new BadRequestException("A reason is required to change a closed month.");
                }
        }

        private String reasonOrDefault(String reason) {
                return reason == null || reason.isBlank() ? "Settlement transaction changed" : reason;
        }

        private SettlementResponseDTO toResponse(EmployeeSettlement s) {
                Integer year = s.getHisabDetail() == null
                                ? (s.getSettlementDate() == null ? null : s.getSettlementDate().getYear())
                                : s.getHisabDetail().getHisab().getYear();

                Integer month = s.getHisabDetail() == null
                                ? (s.getSettlementDate() == null ? null : s.getSettlementDate().getMonthValue())
                                : s.getHisabDetail().getHisab().getMonth();

                return new SettlementResponseDTO(
                                s.getId(),
                                s.getEmployee().getId(),
                                s.getEmployee().getName(),
                                s.getHisabDetail() == null ? null : s.getHisabDetail().getId(),
                                year,
                                month,
                                s.getAmountPaid(),
                                s.getSettlementDate(),
                                s.getNote(),
                                s.getCreatedAt(),
                                s.getUpdatedAt());
        }
}
package com.sonuSaitring.sonuSaitringManagement.employee.service;

import com.sonuSaitring.sonuSaitringManagement.common.exception.ConflictException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.dto.EmployeeRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

        private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

        private final EmployeeRepository employeeRepository;
        private final CurrentOwnerService currentOwnerService;

        private final Counter employeeCreated;
        private final Counter employeeUpdated;
        private final Counter employeeDeleted;
        private final Counter employeeBlockToggled;
        private final Counter employeeNotFound;
        private final Counter employeeConflict;

        private final Timer employeeCreateTimer;
        private final Timer employeeUpdateTimer;
        private final Timer employeeDeleteTimer;
        private final Timer employeeBlockToggleTimer;
        private final Timer employeeListTimer;
        private final Timer employeeLookupTimer;

        public EmployeeServiceImpl(
                        EmployeeRepository employeeRepository,
                        CurrentOwnerService currentOwnerService,
                        MeterRegistry meterRegistry) {

                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;

                this.employeeCreated = Counter.builder("employee.created")
                                .description("Number of employees created")
                                .register(meterRegistry);

                this.employeeUpdated = Counter.builder("employee.updated")
                                .description("Number of employees updated")
                                .register(meterRegistry);

                this.employeeDeleted = Counter.builder("employee.deleted")
                                .description("Number of employees deleted")
                                .register(meterRegistry);

                this.employeeBlockToggled = Counter.builder("employee.block.toggle")
                                .description("Number of employee block status changes")
                                .register(meterRegistry);

                this.employeeNotFound = Counter.builder("employee.not_found")
                                .description("Number of employee lookup operations where employee was not found")
                                .register(meterRegistry);

                this.employeeConflict = Counter.builder("employee.conflict")
                                .description("Number of employee operations rejected because of conflicting data")
                                .register(meterRegistry);

                this.employeeCreateTimer = Timer.builder("employee.create.duration")
                                .description("Time taken to create an employee")
                                .register(meterRegistry);

                this.employeeUpdateTimer = Timer.builder("employee.update.duration")
                                .description("Time taken to update an employee")
                                .register(meterRegistry);

                this.employeeDeleteTimer = Timer.builder("employee.delete.duration")
                                .description("Time taken to delete an employee")
                                .register(meterRegistry);

                this.employeeBlockToggleTimer = Timer.builder("employee.block.toggle.duration")
                                .description("Time taken to change employee block status")
                                .register(meterRegistry);

                this.employeeListTimer = Timer.builder("employee.list.duration")
                                .description("Time taken to fetch employees")
                                .register(meterRegistry);

                this.employeeLookupTimer = Timer.builder("employee.lookup.duration")
                                .description("Time taken to fetch an employee")
                                .register(meterRegistry);
        }

        @Override
        public Employee addEmployee(
                        EmployeeRequestDTO requestDTO) {

                return employeeCreateTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Attempting to add employee for owner: {} with mobile: {}",
                                        ownerId,
                                        requestDTO.getMobile());

                        try {

                                if (employeeRepository.existsByOwnerIdAndMobile(
                                                ownerId,
                                                requestDTO.getMobile())) {

                                        employeeConflict.increment();

                                        logger.warn(
                                                        "Failed to add employee: mobile already exists for owner {}",
                                                        ownerId);

                                        throw new ConflictException(
                                                        "Employee with this mobile number already exists.");
                                }

                                Employee employee = new Employee();

                                employee.setName(
                                                requestDTO.getName());

                                employee.setMobile(
                                                requestDTO.getMobile());

                                if (requestDTO.getInitialRate() != null) {

                                        employee.setInitialRate(
                                                        requestDTO.getInitialRate());

                                } else {

                                        employee.setInitialRate(
                                                        BigDecimal.ZERO);
                                }

                                employee.setBlocked(false);

                                employee.setOwner(
                                                currentOwnerService.getCurrentOwner());

                                Employee savedEmployee = employeeRepository.save(employee);

                                employeeCreated.increment();

                                logger.info(
                                                "Successfully added employee ID: {} for owner: {}",
                                                savedEmployee.getId(),
                                                ownerId);

                                return savedEmployee;

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Employee creation failed: ownerId={}, exception={}",
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        public Employee updateEmployee(
                        Long id,
                        EmployeeRequestDTO requestDTO) {

                return employeeUpdateTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Attempting to update employee ID: {} for owner: {}",
                                        id,
                                        ownerId);

                        try {

                                Employee employee = employeeRepository
                                                .findByIdAndOwnerId(id, ownerId)
                                                .orElseThrow(() -> {

                                                        employeeNotFound.increment();

                                                        logger.warn(
                                                                        "Employee {} not found for owner {}",
                                                                        id,
                                                                        ownerId);

                                                        return new ResourceNotFoundException(
                                                                        "Employee not found with id: " + id);
                                                });

                                if (!employee.getMobile().equals(
                                                requestDTO.getMobile())
                                                && employeeRepository
                                                                .existsByOwnerIdAndMobile(
                                                                                ownerId,
                                                                                requestDTO.getMobile())) {

                                        employeeConflict.increment();

                                        logger.warn(
                                                        "Mobile {} already belongs to another employee of owner {}",
                                                        requestDTO.getMobile(),
                                                        ownerId);

                                        throw new ConflictException(
                                                        "Mobile number already in use by another employee.");
                                }

                                employee.setName(
                                                requestDTO.getName());

                                employee.setMobile(
                                                requestDTO.getMobile());

                                if (requestDTO.getInitialRate() != null) {

                                        employee.setInitialRate(
                                                        requestDTO.getInitialRate());
                                }

                                Employee updatedEmployee = employeeRepository.save(employee);

                                employeeUpdated.increment();

                                logger.info(
                                                "Successfully updated employee ID: {} for owner: {}",
                                                id,
                                                ownerId);

                                return updatedEmployee;

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Employee update failed: employeeId={}, ownerId={}, exception={}",
                                                id,
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        public void deleteEmployee(Long id) {

                employeeDeleteTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Attempting to delete employee ID: {} for owner: {}",
                                        id,
                                        ownerId);

                        try {

                                Employee employee = employeeRepository
                                                .findByIdAndOwnerId(id, ownerId)
                                                .orElseThrow(() -> {

                                                        employeeNotFound.increment();

                                                        logger.warn(
                                                                        "Employee {} not found for owner {}",
                                                                        id,
                                                                        ownerId);

                                                        return new ResourceNotFoundException(
                                                                        "Employee not found with id: " + id);
                                                });

                                employeeRepository.delete(employee);

                                employeeDeleted.increment();

                                logger.info(
                                                "Successfully deleted employee ID: {} for owner: {}",
                                                id,
                                                ownerId);

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Employee deletion failed: employeeId={}, ownerId={}, exception={}",
                                                id,
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        public Employee toggleBlockStatus(Long id) {

                return employeeBlockToggleTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Toggling block status for employee ID: {} for owner: {}",
                                        id,
                                        ownerId);

                        try {

                                Employee employee = employeeRepository
                                                .findByIdAndOwnerId(id, ownerId)
                                                .orElseThrow(() -> {

                                                        employeeNotFound.increment();

                                                        logger.warn(
                                                                        "Employee {} not found for owner {}",
                                                                        id,
                                                                        ownerId);

                                                        return new ResourceNotFoundException(
                                                                        "Employee not found with id: " + id);
                                                });

                                employee.setBlocked(
                                                !employee.isBlocked());

                                Employee savedEmployee = employeeRepository.save(employee);

                                employeeBlockToggled.increment();

                                logger.info(
                                                "Employee ID {} block status changed to {} for owner {}",
                                                id,
                                                savedEmployee.isBlocked(),
                                                ownerId);

                                return savedEmployee;

                        } catch (RuntimeException ex) {

                                logger.error(
                                                "Employee block status update failed: employeeId={}, ownerId={}, exception={}",
                                                id,
                                                ownerId,
                                                ex.getClass().getSimpleName(),
                                                ex);

                                throw ex;
                        }
                });
        }

        @Override
        public List<Employee> getAllEmployees() {

                return employeeListTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Fetching employees for owner: {}",
                                        ownerId);

                        List<Employee> employees = employeeRepository.findAllByOwnerId(ownerId);

                        logger.info(
                                        "Employees fetched: ownerId={}, count={}",
                                        ownerId,
                                        employees.size());

                        return employees;
                });
        }

        @Override
        public Employee getEmployeeById(Long id) {

                return employeeLookupTimer.record(() -> {

                        Long ownerId = currentOwnerService.getCurrentOwnerId();

                        logger.info(
                                        "Fetching employee ID: {} for owner: {}",
                                        id,
                                        ownerId);

                        return employeeRepository
                                        .findByIdAndOwnerId(id, ownerId)
                                        .orElseThrow(() -> {

                                                employeeNotFound.increment();

                                                logger.warn(
                                                                "Employee {} not found for owner {}",
                                                                id,
                                                                ownerId);

                                                return new ResourceNotFoundException(
                                                                "Employee not found with id: " + id);
                                        });
                });
        }
}
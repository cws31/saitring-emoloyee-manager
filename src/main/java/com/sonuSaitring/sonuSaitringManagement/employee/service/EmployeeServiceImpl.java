package com.sonuSaitring.sonuSaitringManagement.employee.service;

import com.sonuSaitring.sonuSaitringManagement.common.exception.ConflictException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.employee.dto.EmployeeRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;
import com.sonuSaitring.sonuSaitringManagement.security.CurrentOwnerService;

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

        public EmployeeServiceImpl(
                        EmployeeRepository employeeRepository,
                        CurrentOwnerService currentOwnerService) {

                this.employeeRepository = employeeRepository;
                this.currentOwnerService = currentOwnerService;
        }

        @Override
        public Employee addEmployee(
                        EmployeeRequestDTO requestDTO) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Attempting to add employee for owner: {} with mobile: {}",
                                ownerId,
                                requestDTO.getMobile());

                if (employeeRepository.existsByOwnerIdAndMobile(
                                ownerId,
                                requestDTO.getMobile())) {

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

                logger.info(
                                "Successfully added employee ID: {} for owner: {}",
                                savedEmployee.getId(),
                                ownerId);

                return savedEmployee;
        }

        @Override
        public Employee updateEmployee(
                        Long id,
                        EmployeeRequestDTO requestDTO) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Attempting to update employee ID: {} for owner: {}",
                                id,
                                ownerId);

                Employee employee = employeeRepository
                                .findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> {

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

                logger.info(
                                "Successfully updated employee ID: {} for owner: {}",
                                id,
                                ownerId);

                return updatedEmployee;
        }

        @Override
        public void deleteEmployee(Long id) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Attempting to delete employee ID: {} for owner: {}",
                                id,
                                ownerId);

                Employee employee = employeeRepository
                                .findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> {

                                        logger.warn(
                                                        "Employee {} not found for owner {}",
                                                        id,
                                                        ownerId);

                                        return new ResourceNotFoundException(
                                                        "Employee not found with id: " + id);
                                });

                employeeRepository.delete(employee);

                logger.info(
                                "Successfully deleted employee ID: {} for owner: {}",
                                id,
                                ownerId);
        }

        @Override
        public Employee toggleBlockStatus(Long id) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Toggling block status for employee ID: {} for owner: {}",
                                id,
                                ownerId);

                Employee employee = employeeRepository
                                .findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> {

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

                logger.info(
                                "Employee ID {} block status changed to {} for owner {}",
                                id,
                                savedEmployee.isBlocked(),
                                ownerId);

                return savedEmployee;
        }

        @Override
        public List<Employee> getAllEmployees() {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Fetching employees for owner: {}",
                                ownerId);

                return employeeRepository
                                .findAllByOwnerId(ownerId);
        }

        @Override
        public Employee getEmployeeById(Long id) {

                Long ownerId = currentOwnerService.getCurrentOwnerId();

                logger.info(
                                "Fetching employee ID: {} for owner: {}",
                                id,
                                ownerId);

                return employeeRepository
                                .findByIdAndOwnerId(id, ownerId)
                                .orElseThrow(() -> {

                                        logger.warn(
                                                        "Employee {} not found for owner {}",
                                                        id,
                                                        ownerId);

                                        return new ResourceNotFoundException(
                                                        "Employee not found with id: " + id);
                                });
        }
}
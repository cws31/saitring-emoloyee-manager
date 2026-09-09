package com.sonuSaitring.sonuSaitringManagement.employee.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sonuSaitring.sonuSaitringManagement.employee.dto.EmployeeRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Employee addEmployee(EmployeeRequestDTO requestDTO) {
        logger.info("Attempting to add new employee with mobile: {}", requestDTO.getMobile());
        if (employeeRepository.existsByMobile(requestDTO.getMobile())) {
            logger.warn("Failed to add employee: Mobile number {} already exists.", requestDTO.getMobile());
            throw new IllegalArgumentException(
                    "Employee with mobile number " + requestDTO.getMobile() + " already exists.");
        }

        Employee employee = new Employee();
        employee.setName(requestDTO.getName());
        employee.setMobile(requestDTO.getMobile());

        if (requestDTO.getInitialRate() != null) {
            employee.setInitialRate(requestDTO.getInitialRate());
        } else {
            employee.setInitialRate(BigDecimal.ZERO);
        }

        employee.setBlocked(false);
        Employee savedEmployee = employeeRepository.save(employee);
        logger.info("Successfully added employee with ID: {}", savedEmployee.getId());
        return savedEmployee;
    }

    @Override
    public Employee updateEmployee(Long id, EmployeeRequestDTO requestDTO) {
        logger.info("Attempting to update employee ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Employee not found with id: {} during update", id);
                    return new RuntimeException("Employee not found with id: " + id);
                });

        if (!employee.getMobile().equals(requestDTO.getMobile()) &&
                employeeRepository.existsByMobile(requestDTO.getMobile())) {
            logger.warn("Failed to update employee ID {}: Mobile number {} is already in use", id,
                    requestDTO.getMobile());
            throw new IllegalArgumentException("Mobile number already in use by another employee.");
        }

        employee.setName(requestDTO.getName());
        employee.setMobile(requestDTO.getMobile());
        if (requestDTO.getInitialRate() != null) {
            employee.setInitialRate(requestDTO.getInitialRate());
        }

        Employee updatedEmployee = employeeRepository.save(employee);
        logger.info("Successfully updated employee ID: {}", id);
        return updatedEmployee;
    }

    @Override
    public void deleteEmployee(Long id) {
        logger.info("Attempting to delete employee ID: {}", id);
        if (!employeeRepository.existsById(id)) {
            logger.error("Failed to delete: Employee not found with id: {}", id);
            throw new RuntimeException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
        logger.info("Successfully deleted employee ID: {}", id);
    }

    @Override
    public Employee toggleBlockStatus(Long id) {
        logger.info("Toggling block status for employee ID: {}", id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Employee not found with id: {} during block status toggle", id);
                    return new RuntimeException("Employee not found with id: " + id);
                });

        employee.setBlocked(!employee.isBlocked());
        Employee savedEmployee = employeeRepository.save(employee);
        logger.info("Employee ID {} block status changed to: {}", id, savedEmployee.isBlocked());
        return savedEmployee;
    }

    @Override
    public List<Employee> getAllEmployees() {
        logger.info("Fetching list of all employees.");
        return employeeRepository.findAll();
    }

    @Override
    public Employee getEmployeeById(Long id) {
        logger.info("Fetching employee details for ID: {}", id);
        return employeeRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Employee not found with id: {}", id);
                    return new RuntimeException("Employee not found with id: " + id);
                });
    }
}
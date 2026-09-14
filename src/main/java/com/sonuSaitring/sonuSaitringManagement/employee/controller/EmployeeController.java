package com.sonuSaitring.sonuSaitringManagement.employee.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.sonuSaitring.sonuSaitringManagement.employee.dto.EmployeeRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.service.EmployeeService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    @Autowired
    private EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<Employee> addEmployee(
            @Valid @RequestBody EmployeeRequestDTO requestDTO) {

        logger.info(
                "Employee creation request received: mobile={}",
                requestDTO.getMobile());

        Employee createdEmployee = employeeService.addEmployee(requestDTO);

        logger.info(
                "Employee creation request completed: employeeId={}",
                createdEmployee.getId());

        return new ResponseEntity<>(
                createdEmployee,
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDTO requestDTO) {

        logger.info(
                "Employee update request received: employeeId={}",
                id);

        Employee updatedEmployee = employeeService.updateEmployee(id, requestDTO);

        logger.info(
                "Employee update request completed: employeeId={}",
                id);

        return ResponseEntity.ok(updatedEmployee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(
            @PathVariable Long id) {

        logger.info(
                "Employee deletion request received: employeeId={}",
                id);

        employeeService.deleteEmployee(id);

        logger.info(
                "Employee deletion request completed: employeeId={}",
                id);

        return ResponseEntity.ok(
                "Employee deleted successfully.");
    }

    @PatchMapping("/{id}/toggle-block")
    public ResponseEntity<Employee> toggleBlockStatus(
            @PathVariable Long id) {

        logger.info(
                "Employee block status request received: employeeId={}",
                id);

        Employee updatedEmployee = employeeService.toggleBlockStatus(id);

        logger.info(
                "Employee block status request completed: employeeId={}, blocked={}",
                id,
                updatedEmployee.isBlocked());

        return ResponseEntity.ok(updatedEmployee);
    }

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {

        logger.info("Employee list request received");

        List<Employee> employees = employeeService.getAllEmployees();

        logger.info(
                "Employee list request completed: count={}",
                employees.size());

        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(
            @PathVariable Long id) {

        logger.info(
                "Employee lookup request received: employeeId={}",
                id);

        Employee employee = employeeService.getEmployeeById(id);

        logger.info(
                "Employee lookup request completed: employeeId={}",
                id);

        return ResponseEntity.ok(employee);
    }
}
package com.sonuSaitring.sonuSaitringManagement.employee.controller;


import jakarta.validation.Valid;
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

    @Autowired
    private EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<Employee> addEmployee(@Valid @RequestBody EmployeeRequestDTO requestDTO) {
        Employee createdEmployee = employeeService.addEmployee(requestDTO);
        return new ResponseEntity<>(createdEmployee, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequestDTO requestDTO) {
        Employee updatedEmployee = employeeService.updateEmployee(id, requestDTO);
        return ResponseEntity.ok(updatedEmployee);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok("Employee deleted successfully.");
    }

    @PatchMapping("/{id}/toggle-block")
    public ResponseEntity<Employee> toggleBlockStatus(@PathVariable Long id) {
        Employee updatedEmployee = employeeService.toggleBlockStatus(id);
        return ResponseEntity.ok(updatedEmployee);
    }

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }
}
package com.sonuSaitring.sonuSaitringManagement.employee.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sonuSaitring.sonuSaitringManagement.employee.dto.EmployeeRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import com.sonuSaitring.sonuSaitringManagement.employee.repository.EmployeeRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Employee addEmployee(EmployeeRequestDTO requestDTO) {
        if (employeeRepository.existsByMobile(requestDTO.getMobile())) {
            throw new IllegalArgumentException("Employee with mobile number " + requestDTO.getMobile() + " already exists.");
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
        return employeeRepository.save(employee);
    }

    @Override
    public Employee updateEmployee(Long id, EmployeeRequestDTO requestDTO) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

        if (!employee.getMobile().equals(requestDTO.getMobile()) && 
                employeeRepository.existsByMobile(requestDTO.getMobile())) {
            throw new IllegalArgumentException("Mobile number already in use by another employee.");
        }

        employee.setName(requestDTO.getName());
        employee.setMobile(requestDTO.getMobile());
        if (requestDTO.getInitialRate() != null) {
            employee.setInitialRate(requestDTO.getInitialRate());
        }

        return employeeRepository.save(employee);
    }

    @Override
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
    }

    @Override
    public Employee toggleBlockStatus(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        
        employee.setBlocked(!employee.isBlocked());
        return employeeRepository.save(employee);
    }

    @Override

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }
}
package com.sonuSaitring.sonuSaitringManagement.employee.service;



import java.util.List;

import com.sonuSaitring.sonuSaitringManagement.employee.dto.EmployeeRequestDTO;
import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;

public interface EmployeeService {
    Employee addEmployee(EmployeeRequestDTO requestDTO);
    Employee updateEmployee(Long id, EmployeeRequestDTO requestDTO);
    void deleteEmployee(Long id);
    Employee toggleBlockStatus(Long id);
    List<Employee> getAllEmployees();
    Employee getEmployeeById(Long id);
}

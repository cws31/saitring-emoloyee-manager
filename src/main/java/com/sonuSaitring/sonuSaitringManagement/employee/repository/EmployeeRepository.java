package com.sonuSaitring.sonuSaitringManagement.employee.repository;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByMobile(String mobile);
    boolean existsByMobile(String mobile);
}
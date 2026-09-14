package com.sonuSaitring.sonuSaitringManagement.employee.repository;

import com.sonuSaitring.sonuSaitringManagement.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByOwnerIdAndMobile(
            Long ownerId,
            String mobile);

    Optional<Employee> findByIdAndOwnerId(
            Long id,
            Long ownerId);

    Optional<Employee> findByMobileAndOwnerId(
            String mobile,
            Long ownerId);

    List<Employee> findAllByOwnerId(
            Long ownerId);

    void deleteByIdAndOwnerId(
            Long id,
            Long ownerId);
}
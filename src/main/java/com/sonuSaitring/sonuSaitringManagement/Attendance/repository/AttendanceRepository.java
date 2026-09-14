package com.sonuSaitring.sonuSaitringManagement.Attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

        @Query("""
                        SELECT a
                        FROM Attendance a
                        JOIN FETCH a.employee
                        WHERE a.owner.id = :ownerId
                          AND a.employee.id = :employeeId
                          AND a.attendanceDate = :attendanceDate
                        """)
        Optional<Attendance> findByOwnerIdAndEmployeeIdAndAttendanceDate(
                        @Param("ownerId") Long ownerId,
                        @Param("employeeId") Long employeeId,
                        @Param("attendanceDate") LocalDate attendanceDate);

        @Query("""
                        SELECT a
                        FROM Attendance a
                        JOIN FETCH a.employee
                        WHERE a.owner.id = :ownerId
                          AND a.employee.id = :employeeId
                          AND a.attendanceDate BETWEEN :startDate AND :endDate
                        """)
        List<Attendance> findByOwnerIdAndEmployeeIdAndMonth(
                        @Param("ownerId") Long ownerId,
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("""
                        SELECT a
                        FROM Attendance a
                        JOIN FETCH a.employee
                        WHERE a.owner.id = :ownerId
                          AND a.attendanceDate BETWEEN :startDate AND :endDate
                        """)
        List<Attendance> findAllByOwnerIdAndMonth(
                        @Param("ownerId") Long ownerId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
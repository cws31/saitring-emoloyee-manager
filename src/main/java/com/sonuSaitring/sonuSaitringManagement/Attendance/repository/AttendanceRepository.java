package com.sonuSaitring.sonuSaitringManagement.Attendance.repository;

import com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceSummaryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import com.sonuSaitring.sonuSaitringManagement.Attendance.entity.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

        @Query("SELECT a FROM Attendance a JOIN FETCH a.employee WHERE a.id = :id AND a.owner.id = :ownerId")
        Optional<Attendance> findByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

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

        @Query("""
                        SELECT NEW com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceSummaryDTO(
                            a.attendanceDate,
                            SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END),
                            SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END),
                            SUM(CASE WHEN a.status = 'HALF_DAY' THEN 1 ELSE 0 END)
                        )
                        FROM Attendance a
                        WHERE a.owner.id = :ownerId
                          AND a.attendanceDate = :date
                        GROUP BY a.attendanceDate
                        """)
        AttendanceSummaryDTO findDailySummaryByOwnerAndDate(
                        @Param("ownerId") Long ownerId,
                        @Param("date") LocalDate date);

        @Query("""
                        SELECT NEW com.sonuSaitring.sonuSaitringManagement.Attendance.dto.AttendanceSummaryDTO(
                            a.attendanceDate,
                            SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END),
                            SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END),
                            SUM(CASE WHEN a.status = 'HALF_DAY' THEN 1 ELSE 0 END)
                        )
                        FROM Attendance a
                        WHERE a.owner.id = :ownerId
                          AND a.attendanceDate BETWEEN :startDate AND :endDate
                        GROUP BY a.attendanceDate
                        ORDER BY a.attendanceDate ASC
                        """)
        List<AttendanceSummaryDTO> findMonthlySummaryByOwnerAndDateRange(
                        @Param("ownerId") Long ownerId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("""
                        SELECT a FROM Attendance a JOIN FETCH a.employee
                        WHERE a.owner.id = :ownerId AND a.employee.id = :employeeId
                        ORDER BY a.attendanceDate ASC
                        """)
        List<Attendance> findAllForEmployeeAndOwner(
                        @Param("ownerId") Long ownerId, @Param("employeeId") Long employeeId);

        @Modifying
        @Query("""
                        DELETE FROM Attendance a
                        WHERE a.id = :attendanceId
                          AND a.owner.id = :ownerId
                        """)
        int deleteByIdAndOwnerId(
                        @Param("attendanceId") Long attendanceId,
                        @Param("ownerId") Long ownerId);

        @Modifying
        @Query("""
                        DELETE FROM Attendance a
                        WHERE a.employee.id = :employeeId
                          AND a.owner.id = :ownerId
                          AND a.attendanceDate = :date
                        """)
        int deleteByEmployeeIdAndOwnerIdAndDate(
                        @Param("employeeId") Long employeeId,
                        @Param("ownerId") Long ownerId,
                        @Param("date") LocalDate date);

        @Modifying
        @Query("""
                        DELETE FROM Attendance a
                        WHERE a.owner.id = :ownerId
                          AND a.attendanceDate = :date
                        """)
        int deleteAllByOwnerIdAndDate(
                        @Param("ownerId") Long ownerId,
                        @Param("date") LocalDate date);
}

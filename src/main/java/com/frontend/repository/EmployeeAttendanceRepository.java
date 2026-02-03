package com.frontend.repository;

import com.frontend.entity.EmployeeAttendance;
import com.frontend.entity.Employees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for EmployeeAttendance entity - uses 'employee_attendance' table
 */
@Repository
public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, Integer> {

    /**
     * Find all absences for a specific date
     */
    List<EmployeeAttendance> findByAttendanceDate(LocalDate attendanceDate);

    /**
     * Find specific absence record for an employee on a date
     */
    Optional<EmployeeAttendance> findByEmployeeAndAttendanceDate(Employees employee, LocalDate attendanceDate);

    /**
     * Delete absence record for an employee on a date (mark as present)
     */
    void deleteByEmployeeAndAttendanceDate(Employees employee, LocalDate attendanceDate);

    /**
     * Check if an employee has an absence record for a date
     */
    boolean existsByEmployeeAndAttendanceDate(Employees employee, LocalDate attendanceDate);

    /**
     * Count total absences for a specific date
     */
    long countByAttendanceDate(LocalDate attendanceDate);

    /**
     * Find absences within a date range (for future reporting)
     */
    @Query("SELECT ea FROM EmployeeAttendance ea WHERE ea.attendanceDate BETWEEN :startDate AND :endDate ORDER BY ea.attendanceDate, ea.employee.firstName")
    List<EmployeeAttendance> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count absences for a specific employee within a date range (for salary calculation)
     */
    @Query("SELECT COUNT(ea) FROM EmployeeAttendance ea WHERE ea.employee.employeeId = :employeeId AND ea.attendanceDate BETWEEN :startDate AND :endDate")
    long countByEmployeeIdAndDateRange(@Param("employeeId") Integer employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find absences for a specific employee within a date range
     */
    @Query("SELECT ea FROM EmployeeAttendance ea WHERE ea.employee.employeeId = :employeeId AND ea.attendanceDate BETWEEN :startDate AND :endDate ORDER BY ea.attendanceDate")
    List<EmployeeAttendance> findByEmployeeIdAndDateRange(@Param("employeeId") Integer employeeId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

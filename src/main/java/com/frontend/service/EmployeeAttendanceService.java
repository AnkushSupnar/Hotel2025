package com.frontend.service;

import com.frontend.entity.EmployeeAttendance;
import com.frontend.entity.Employees;
import com.frontend.repository.EmployeeAttendanceRepository;
import com.frontend.repository.EmployeesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for Employee Attendance operations.
 * A record existing = ABSENT. No record = PRESENT.
 */
@Service
public class EmployeeAttendanceService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeAttendanceService.class);

    @Autowired
    private EmployeeAttendanceRepository attendanceRepository;

    @Autowired
    private EmployeesRepository employeesRepository;

    /**
     * Get all absence records for a specific date
     */
    public List<EmployeeAttendance> getAbsencesForDate(LocalDate date) {
        try {
            LOG.info("Fetching absences for date: {}", date);
            return attendanceRepository.findByAttendanceDate(date);
        } catch (Exception e) {
            LOG.error("Error fetching absences for date: {}", date, e);
            throw new RuntimeException("Error fetching absences: " + e.getMessage(), e);
        }
    }

    /**
     * Mark an employee as absent for a date with optional reason
     */
    @Transactional
    public EmployeeAttendance markAbsent(Integer employeeId, LocalDate date, String reason) {
        try {
            LOG.info("Marking employee {} as absent on {}", employeeId, date);

            Employees employee = employeesRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

            // Check if record already exists
            Optional<EmployeeAttendance> existing = attendanceRepository.findByEmployeeAndAttendanceDate(employee, date);

            if (existing.isPresent()) {
                // Update existing record with new reason
                EmployeeAttendance record = existing.get();
                record.setReason(reason);
                return attendanceRepository.save(record);
            } else {
                // Create new absence record
                EmployeeAttendance record = new EmployeeAttendance(employee, date, reason);
                return attendanceRepository.save(record);
            }
        } catch (Exception e) {
            LOG.error("Error marking employee {} absent on {}", employeeId, date, e);
            throw new RuntimeException("Error marking absent: " + e.getMessage(), e);
        }
    }

    /**
     * Mark an employee as present by removing absence record
     */
    @Transactional
    public void markPresent(Integer employeeId, LocalDate date) {
        try {
            LOG.info("Marking employee {} as present on {}", employeeId, date);

            Employees employee = employeesRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

            attendanceRepository.deleteByEmployeeAndAttendanceDate(employee, date);
        } catch (Exception e) {
            LOG.error("Error marking employee {} present on {}", employeeId, date, e);
            throw new RuntimeException("Error marking present: " + e.getMessage(), e);
        }
    }

    /**
     * Batch save attendance for a date. Iterates entries and marks absent or present.
     */
    @Transactional
    public void saveAttendanceForDate(LocalDate date, List<AttendanceEntry> entries) {
        try {
            LOG.info("Saving attendance for date: {} with {} entries", date, entries.size());

            for (AttendanceEntry entry : entries) {
                if (entry.isAbsent()) {
                    markAbsent(entry.getEmployeeId(), date, entry.getReason());
                } else {
                    markPresent(entry.getEmployeeId(), date);
                }
            }

            LOG.info("Attendance saved successfully for date: {}", date);
        } catch (Exception e) {
            LOG.error("Error saving attendance for date: {}", date, e);
            throw new RuntimeException("Error saving attendance: " + e.getMessage(), e);
        }
    }

    /**
     * Count absent employees for a date
     */
    public long countAbsentForDate(LocalDate date) {
        try {
            return attendanceRepository.countByAttendanceDate(date);
        } catch (Exception e) {
            LOG.error("Error counting absences for date: {}", date, e);
            return 0;
        }
    }

    /**
     * Inner class representing a single attendance entry for batch save
     */
    public static class AttendanceEntry {
        private final Integer employeeId;
        private final boolean absent;
        private final String reason;

        public AttendanceEntry(Integer employeeId, boolean absent, String reason) {
            this.employeeId = employeeId;
            this.absent = absent;
            this.reason = reason;
        }

        public Integer getEmployeeId() {
            return employeeId;
        }

        public boolean isAbsent() {
            return absent;
        }

        public String getReason() {
            return reason;
        }
    }
}

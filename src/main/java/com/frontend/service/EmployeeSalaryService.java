package com.frontend.service;

import com.frontend.entity.EmployeeSalaryPayment;
import com.frontend.entity.Employees;
import com.frontend.repository.EmployeeAttendanceRepository;
import com.frontend.repository.EmployeeSalaryPaymentRepository;
import com.frontend.repository.EmployeesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for employee salary calculation and payment.
 * Logic: Only absent days are recorded. All other days = present.
 * Salary = (monthlySalary / totalDaysInMonth) * presentDays
 */
@Service
public class EmployeeSalaryService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeSalaryService.class);

    @Autowired
    private EmployeesRepository employeesRepository;

    @Autowired
    private EmployeeAttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeSalaryPaymentRepository salaryPaymentRepository;

    /**
     * Calculate salary data for all active employees for a given month/year.
     * Returns a list of SalaryData objects for display.
     */
    public List<SalaryData> calculateSalaryForMonth(int month, int year) {
        LOG.info("Calculating salary for {}/{}", month, year);

        YearMonth yearMonth = YearMonth.of(year, month);
        int totalDays = yearMonth.lengthOfMonth();
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Employees> activeEmployees = employeesRepository.findByActiveStatusTrueOrderByFirstNameAsc();
        List<SalaryData> result = new ArrayList<>();

        for (Employees emp : activeEmployees) {
            long absentDays = attendanceRepository.countByEmployeeIdAndDateRange(
                    emp.getEmployeeId(), startDate, endDate);
            int presentDays = totalDays - (int) absentDays;

            Float monthlySalary = emp.getCurrentSalary();
            if (monthlySalary == null) monthlySalary = 0f;

            float perDaySalary = monthlySalary / totalDays;
            float calculatedSalary = Math.round(perDaySalary * presentDays * 100f) / 100f;

            boolean isPaid = salaryPaymentRepository.existsByEmployeeEmployeeIdAndSalaryMonthAndSalaryYear(
                    emp.getEmployeeId(), month, year);

            SalaryData data = new SalaryData(
                    emp.getEmployeeId(),
                    emp.getFullName(),
                    emp.getDesignation() != null ? emp.getDesignation() : "",
                    monthlySalary,
                    totalDays,
                    (int) absentDays,
                    presentDays,
                    calculatedSalary,
                    isPaid
            );
            result.add(data);
        }

        LOG.info("Calculated salary for {} employees for {}/{}", result.size(), month, year);
        return result;
    }

    /**
     * Record a salary payment for an employee for a given month/year.
     */
    @Transactional
    public EmployeeSalaryPayment paySalary(Integer employeeId, int month, int year,
                                            int totalDays, int absentDays, int presentDays,
                                            float monthlySalary, float calculatedSalary,
                                            float paidAmount, String remarks) {
        LOG.info("Paying salary to employee {} for {}/{}: amount={}", employeeId, month, year, paidAmount);

        // Check if already paid
        if (salaryPaymentRepository.existsByEmployeeEmployeeIdAndSalaryMonthAndSalaryYear(employeeId, month, year)) {
            throw new RuntimeException("Salary already paid for this employee for " + month + "/" + year);
        }

        Employees employee = employeesRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        EmployeeSalaryPayment payment = new EmployeeSalaryPayment();
        payment.setEmployee(employee);
        payment.setSalaryMonth(month);
        payment.setSalaryYear(year);
        payment.setTotalDays(totalDays);
        payment.setAbsentDays(absentDays);
        payment.setPresentDays(presentDays);
        payment.setMonthlySalary(monthlySalary);
        payment.setCalculatedSalary(calculatedSalary);
        payment.setPaidAmount(paidAmount);
        payment.setRemarks(remarks);

        EmployeeSalaryPayment saved = salaryPaymentRepository.save(payment);
        LOG.info("Salary paid to {} for {}/{}: {}", employee.getFullName(), month, year, paidAmount);
        return saved;
    }

    /**
     * Check if salary is already paid for an employee for a given month/year.
     */
    public boolean isSalaryPaid(Integer employeeId, int month, int year) {
        return salaryPaymentRepository.existsByEmployeeEmployeeIdAndSalaryMonthAndSalaryYear(employeeId, month, year);
    }

    /**
     * Data class for salary calculation results.
     */
    public static class SalaryData {
        private final int employeeId;
        private final String name;
        private final String designation;
        private final float monthlySalary;
        private final int totalDays;
        private final int absentDays;
        private final int presentDays;
        private final float calculatedSalary;
        private final boolean paid;

        public SalaryData(int employeeId, String name, String designation, float monthlySalary,
                          int totalDays, int absentDays, int presentDays, float calculatedSalary, boolean paid) {
            this.employeeId = employeeId;
            this.name = name;
            this.designation = designation;
            this.monthlySalary = monthlySalary;
            this.totalDays = totalDays;
            this.absentDays = absentDays;
            this.presentDays = presentDays;
            this.calculatedSalary = calculatedSalary;
            this.paid = paid;
        }

        public int getEmployeeId() { return employeeId; }
        public String getName() { return name; }
        public String getDesignation() { return designation; }
        public float getMonthlySalary() { return monthlySalary; }
        public int getTotalDays() { return totalDays; }
        public int getAbsentDays() { return absentDays; }
        public int getPresentDays() { return presentDays; }
        public float getCalculatedSalary() { return calculatedSalary; }
        public boolean isPaid() { return paid; }
    }
}

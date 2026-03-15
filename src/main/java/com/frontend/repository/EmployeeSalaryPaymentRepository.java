package com.frontend.repository;

import com.frontend.entity.EmployeeSalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSalaryPaymentRepository extends JpaRepository<EmployeeSalaryPayment, Integer> {

    /**
     * Check if salary is already paid for an employee in a given month/year
     */
    boolean existsByEmployeeEmployeeIdAndSalaryMonthAndSalaryYear(Integer employeeId, Integer salaryMonth, Integer salaryYear);

    /**
     * Find payment record for an employee in a given month/year
     */
    Optional<EmployeeSalaryPayment> findByEmployeeEmployeeIdAndSalaryMonthAndSalaryYear(Integer employeeId, Integer salaryMonth, Integer salaryYear);

    /**
     * Find all payments for a given month/year
     */
    List<EmployeeSalaryPayment> findBySalaryMonthAndSalaryYear(Integer salaryMonth, Integer salaryYear);

    /**
     * Find all payments for a given employee
     */
    List<EmployeeSalaryPayment> findByEmployeeEmployeeIdOrderBySalaryYearDescSalaryMonthDesc(Integer employeeId);
}

package com.frontend.repository;

import com.frontend.entity.EmployeeAdvancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeAdvancePaymentRepository extends JpaRepository<EmployeeAdvancePayment, Integer> {

    /**
     * Find all advance payments for a specific employee, ordered by date desc
     */
    List<EmployeeAdvancePayment> findByEmployeeEmployeeIdOrderByPaidAtDesc(Integer employeeId);

    /**
     * Find all advance payments ordered by date desc
     */
    List<EmployeeAdvancePayment> findAllByOrderByPaidAtDesc();

    /**
     * Find advance payments within a date range
     */
    List<EmployeeAdvancePayment> findByPaidAtBetweenOrderByPaidAtDesc(LocalDateTime start, LocalDateTime end);
}

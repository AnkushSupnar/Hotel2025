package com.frontend.service;

import com.frontend.entity.EmployeeAdvancePayment;
import com.frontend.entity.Employees;
import com.frontend.repository.EmployeeAdvancePaymentRepository;
import com.frontend.repository.EmployeesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeAdvanceService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeAdvanceService.class);

    @Autowired
    private EmployeeAdvancePaymentRepository advancePaymentRepository;

    @Autowired
    private EmployeesRepository employeesRepository;

    @Autowired
    private BankTransactionService bankTransactionService;

    /**
     * Record an advance payment for an employee.
     * If payment mode is BANK, creates a bank withdrawal transaction.
     */
    @Transactional
    public EmployeeAdvancePayment recordAdvance(Integer employeeId, Double amount, String reason,
                                                 Integer bankId, String paymentMode, String remarks) {
        LOG.info("Recording advance for employee {}: amount={}, mode={}", employeeId, amount, paymentMode);

        Employees employee = employeesRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        EmployeeAdvancePayment advance = new EmployeeAdvancePayment();
        advance.setEmployee(employee);
        advance.setAmount(amount);
        advance.setReason(reason);
        advance.setBankId(bankId);
        advance.setPaymentMode(paymentMode);
        advance.setRemarks(remarks);

        EmployeeAdvancePayment saved = advancePaymentRepository.save(advance);

        // If BANK mode, record withdrawal
        if ("BANK".equals(paymentMode) && bankId != null) {
            String particulars = "Advance to " + employee.getFullName();
            bankTransactionService.recordWithdrawal(bankId, amount, particulars,
                    "EMPLOYEE_ADVANCE", saved.getId(), remarks);
            LOG.info("Bank withdrawal recorded for advance ID {}", saved.getId());
        }

        LOG.info("Advance recorded: ID={}, Employee={}, Amount={}", saved.getId(), employee.getFullName(), amount);
        return saved;
    }

    /**
     * Get all advance payments for a specific employee
     */
    public List<EmployeeAdvancePayment> getAdvancesByEmployee(Integer employeeId) {
        return advancePaymentRepository.findByEmployeeEmployeeIdOrderByPaidAtDesc(employeeId);
    }

    /**
     * Get all advance payments
     */
    public List<EmployeeAdvancePayment> getAllAdvances() {
        return advancePaymentRepository.findAllByOrderByPaidAtDesc();
    }

    /**
     * Get total advance amount for a specific employee
     */
    public Double getTotalAdvanceByEmployee(Integer employeeId) {
        List<EmployeeAdvancePayment> advances = advancePaymentRepository
                .findByEmployeeEmployeeIdOrderByPaidAtDesc(employeeId);
        return advances.stream()
                .mapToDouble(EmployeeAdvancePayment::getAmount)
                .sum();
    }
}

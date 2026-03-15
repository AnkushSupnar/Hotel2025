package com.frontend.service;

import com.frontend.entity.Bank;
import com.frontend.entity.BankTransaction;
import com.frontend.repository.BankRepository;
import com.frontend.repository.BankTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for BankTransaction operations
 * Handles all bank transaction tracking for deposits and withdrawals
 */
@Service
public class BankTransactionService {

    private static final Logger LOG = LoggerFactory.getLogger(BankTransactionService.class);

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private BankRepository bankRepository;

    /**
     * Record a deposit transaction (money coming into the bank)
     */
    @Transactional
    public BankTransaction recordDeposit(Integer bankId, Double amount, String particulars,
                                          String referenceType, Integer referenceId, String remarks) {
        try {
            if (bankId == null || amount == null || amount <= 0) {
                throw new RuntimeException("Invalid bank ID or amount for deposit");
            }

            // Get current bank balance
            Optional<Bank> optBank = bankRepository.findById(bankId);
            if (optBank.isEmpty()) {
                throw new RuntimeException("Bank not found: " + bankId);
            }

            Bank bank = optBank.get();
            Double currentBalance = bank.getBankBalance() != null ? bank.getBankBalance() : 0.0;
            Double newBalance = currentBalance + amount;

            // Create transaction record
            BankTransaction transaction = new BankTransaction();
            transaction.setBankId(bankId);
            transaction.setParticulars(particulars);
            transaction.setDeposit(amount);
            transaction.setWithdraw(0.0);
            transaction.setBalance(newBalance);
            transaction.setTransactionType("DEPOSIT");
            transaction.setReferenceType(referenceType);
            transaction.setReferenceId(referenceId);
            transaction.setRemarks(remarks);
            transaction.setTransactionDate(LocalDate.now());

            // Save transaction
            BankTransaction savedTransaction = bankTransactionRepository.save(transaction);

            // Update bank balance
            bank.setBankBalance(newBalance);
            bankRepository.save(bank);

            LOG.info("Deposit recorded: Bank={}, Amount={}, NewBalance={}, Ref={}:{}",
                    bankId, amount, newBalance, referenceType, referenceId);

            return savedTransaction;

        } catch (Exception e) {
            LOG.error("Error recording deposit for bank {}: {}", bankId, e.getMessage(), e);
            throw new RuntimeException("Error recording deposit: " + e.getMessage(), e);
        }
    }

    /**
     * Record a withdrawal transaction (money going out of the bank)
     */
    @Transactional
    public BankTransaction recordWithdrawal(Integer bankId, Double amount, String particulars,
                                             String referenceType, Integer referenceId, String remarks) {
        try {
            if (bankId == null || amount == null || amount <= 0) {
                throw new RuntimeException("Invalid bank ID or amount for withdrawal");
            }

            // Get current bank balance
            Optional<Bank> optBank = bankRepository.findById(bankId);
            if (optBank.isEmpty()) {
                throw new RuntimeException("Bank not found: " + bankId);
            }

            Bank bank = optBank.get();
            Double currentBalance = bank.getBankBalance() != null ? bank.getBankBalance() : 0.0;
            Double newBalance = currentBalance - amount;

            // Create transaction record
            BankTransaction transaction = new BankTransaction();
            transaction.setBankId(bankId);
            transaction.setParticulars(particulars);
            transaction.setDeposit(0.0);
            transaction.setWithdraw(amount);
            transaction.setBalance(newBalance);
            transaction.setTransactionType("WITHDRAW");
            transaction.setReferenceType(referenceType);
            transaction.setReferenceId(referenceId);
            transaction.setRemarks(remarks);
            transaction.setTransactionDate(LocalDate.now());

            // Save transaction
            BankTransaction savedTransaction = bankTransactionRepository.save(transaction);

            // Update bank balance
            bank.setBankBalance(newBalance);
            bankRepository.save(bank);

            LOG.info("Withdrawal recorded: Bank={}, Amount={}, NewBalance={}, Ref={}:{}",
                    bankId, amount, newBalance, referenceType, referenceId);

            return savedTransaction;

        } catch (Exception e) {
            LOG.error("Error recording withdrawal for bank {}: {}", bankId, e.getMessage(), e);
            throw new RuntimeException("Error recording withdrawal: " + e.getMessage(), e);
        }
    }

    /**
     * Record a bill payment deposit
     */
    @Transactional
    public BankTransaction recordBillPayment(Integer bankId, Integer billNo, Double amount, String tableName) {
        String particulars = "Bill Payment #" + billNo;
        if (tableName != null && !tableName.isEmpty()) {
            particulars += " (" + tableName + ")";
        }
        String remarks = "Bill-no-" + billNo;
        return recordDeposit(bankId, amount, particulars, "BILL_PAYMENT", billNo, remarks);
    }

    /**
     * Get all transactions for a bank
     */
    public List<BankTransaction> getTransactionsByBankId(Integer bankId) {
        return bankTransactionRepository.findByBankIdOrderByTransactionDateDescIdDesc(bankId);
    }

    /**
     * Get transactions for a bank within date range
     */
    public List<BankTransaction> getTransactionsByBankIdAndDateRange(Integer bankId,
                                                                       LocalDate startDate, LocalDate endDate) {
        return bankTransactionRepository.findByBankIdAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(
                bankId, startDate, endDate);
    }

    /**
     * Get transactions by date
     */
    public List<BankTransaction> getTransactionsByDate(LocalDate date) {
        return bankTransactionRepository.findByTransactionDateOrderByIdDesc(date);
    }

    /**
     * Get transactions by date range
     */
    public List<BankTransaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return bankTransactionRepository.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(startDate, endDate);
    }

    /**
     * Get all transactions
     */
    public List<BankTransaction> getAllTransactions() {
        return bankTransactionRepository.findAllByOrderByTransactionDateDescIdDesc();
    }

    /**
     * Get transaction by ID
     */
    public Optional<BankTransaction> getTransactionById(Integer id) {
        return bankTransactionRepository.findById(id);
    }

    /**
     * Get transactions by reference
     */
    public List<BankTransaction> getTransactionsByReference(String referenceType, Integer referenceId) {
        return bankTransactionRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    /**
     * Get total deposits for a bank
     */
    public Double getTotalDeposits(Integer bankId) {
        return bankTransactionRepository.getTotalDepositsByBankId(bankId);
    }

    /**
     * Get total withdrawals for a bank
     */
    public Double getTotalWithdrawals(Integer bankId) {
        return bankTransactionRepository.getTotalWithdrawalsByBankId(bankId);
    }

    /**
     * Get total deposits for a bank in date range
     */
    public Double getTotalDepositsInDateRange(Integer bankId, LocalDate startDate, LocalDate endDate) {
        return bankTransactionRepository.getTotalDepositsByBankIdAndDateRange(bankId, startDate, endDate);
    }

    /**
     * Get total withdrawals for a bank in date range
     */
    public Double getTotalWithdrawalsInDateRange(Integer bankId, LocalDate startDate, LocalDate endDate) {
        return bankTransactionRepository.getTotalWithdrawalsByBankIdAndDateRange(bankId, startDate, endDate);
    }

    /**
     * Get last transaction for a bank
     */
    public BankTransaction getLastTransaction(Integer bankId) {
        return bankTransactionRepository.findLastTransactionByBankId(bankId);
    }

    /**
     * Get transaction count for a bank
     */
    public long getTransactionCount(Integer bankId) {
        return bankTransactionRepository.countByBankId(bankId);
    }

    /**
     * Delete a transaction (use with caution - may affect balance)
     */
    @Transactional
    public void deleteTransaction(Integer id) {
        try {
            Optional<BankTransaction> optTransaction = bankTransactionRepository.findById(id);
            if (optTransaction.isEmpty()) {
                throw new RuntimeException("Transaction not found: " + id);
            }

            BankTransaction transaction = optTransaction.get();

            // Reverse the effect on bank balance
            Optional<Bank> optBank = bankRepository.findById(transaction.getBankId());
            if (optBank.isPresent()) {
                Bank bank = optBank.get();
                Double currentBalance = bank.getBankBalance() != null ? bank.getBankBalance() : 0.0;

                if ("DEPOSIT".equals(transaction.getTransactionType())) {
                    // Reverse deposit - subtract
                    bank.setBankBalance(currentBalance - transaction.getDeposit());
                } else if ("WITHDRAW".equals(transaction.getTransactionType())) {
                    // Reverse withdrawal - add back
                    bank.setBankBalance(currentBalance + transaction.getWithdraw());
                }

                bankRepository.save(bank);
            }

            bankTransactionRepository.deleteById(id);
            LOG.info("Transaction {} deleted and balance reversed", id);

        } catch (Exception e) {
            LOG.error("Error deleting transaction {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error deleting transaction: " + e.getMessage(), e);
        }
    }
}

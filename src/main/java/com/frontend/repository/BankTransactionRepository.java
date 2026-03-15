package com.frontend.repository;

import com.frontend.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for BankTransaction entity
 */
@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Integer> {

    /**
     * Find transactions by bank ID
     */
    List<BankTransaction> findByBankIdOrderByTransactionDateDescIdDesc(Integer bankId);

    /**
     * Find transactions by bank ID and date range
     */
    List<BankTransaction> findByBankIdAndTransactionDateBetweenOrderByTransactionDateDescIdDesc(
            Integer bankId, LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by date
     */
    List<BankTransaction> findByTransactionDateOrderByIdDesc(LocalDate transactionDate);

    /**
     * Find transactions by date range
     */
    List<BankTransaction> findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(
            LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by transaction type (DEPOSIT/WITHDRAW)
     */
    List<BankTransaction> findByTransactionTypeOrderByTransactionDateDescIdDesc(String transactionType);

    /**
     * Find transactions by reference type and ID (e.g., BILL_PAYMENT and billNo)
     */
    List<BankTransaction> findByReferenceTypeAndReferenceId(String referenceType, Integer referenceId);

    /**
     * Find transactions by reference type
     */
    List<BankTransaction> findByReferenceTypeOrderByTransactionDateDescIdDesc(String referenceType);

    /**
     * Get total deposits for a bank
     */
    @Query("SELECT COALESCE(SUM(bt.deposit), 0) FROM BankTransaction bt WHERE bt.bankId = :bankId")
    Double getTotalDepositsByBankId(@Param("bankId") Integer bankId);

    /**
     * Get total withdrawals for a bank
     */
    @Query("SELECT COALESCE(SUM(bt.withdraw), 0) FROM BankTransaction bt WHERE bt.bankId = :bankId")
    Double getTotalWithdrawalsByBankId(@Param("bankId") Integer bankId);

    /**
     * Get total deposits for a bank in date range
     */
    @Query("SELECT COALESCE(SUM(bt.deposit), 0) FROM BankTransaction bt WHERE bt.bankId = :bankId AND bt.transactionDate BETWEEN :startDate AND :endDate")
    Double getTotalDepositsByBankIdAndDateRange(@Param("bankId") Integer bankId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * Get total withdrawals for a bank in date range
     */
    @Query("SELECT COALESCE(SUM(bt.withdraw), 0) FROM BankTransaction bt WHERE bt.bankId = :bankId AND bt.transactionDate BETWEEN :startDate AND :endDate")
    Double getTotalWithdrawalsByBankIdAndDateRange(@Param("bankId") Integer bankId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * Get last transaction for a bank (to get running balance)
     */
    @Query("SELECT bt FROM BankTransaction bt WHERE bt.bankId = :bankId ORDER BY bt.id DESC LIMIT 1")
    BankTransaction findLastTransactionByBankId(@Param("bankId") Integer bankId);

    /**
     * Count transactions by bank ID
     */
    long countByBankId(Integer bankId);

    /**
     * Find all transactions ordered by date desc
     */
    List<BankTransaction> findAllByOrderByTransactionDateDescIdDesc();
}

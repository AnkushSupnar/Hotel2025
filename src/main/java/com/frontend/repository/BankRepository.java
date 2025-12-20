package com.frontend.repository;

import com.frontend.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Bank entity
 */
@Repository
public interface BankRepository extends JpaRepository<Bank, Integer> {

    /**
     * Find bank by account number
     */
    Optional<Bank> findByAccountNo(String accountNo);

    /**
     * Find banks by bank name
     */
    List<Bank> findByBankName(String bankName);

    /**
     * Find banks by bank name containing (search)
     */
    List<Bank> findByBankNameContainingIgnoreCase(String bankName);

    /**
     * Find all active banks
     */
    List<Bank> findByStatus(String status);

    /**
     * Find active banks only
     */
    @Query("SELECT b FROM Bank b WHERE b.status = 'ACTIVE'")
    List<Bank> findActiveBanks();

    /**
     * Find bank by bank code
     */
    Optional<Bank> findByBankCode(String bankCode);

    /**
     * Find banks by account type
     */
    List<Bank> findByAccountType(String accountType);

    /**
     * Find banks by person name
     */
    List<Bank> findByPersonNameContainingIgnoreCase(String personName);

    /**
     * Get total balance of all active banks
     */
    @Query("SELECT SUM(b.bankBalance) FROM Bank b WHERE b.status = 'ACTIVE'")
    Double getTotalActiveBalance();

    /**
     * Check if account number already exists
     */
    boolean existsByAccountNo(String accountNo);

    /**
     * Check if bank code already exists
     */
    boolean existsByBankCode(String bankCode);

    /**
     * Update bank balance
     */
    @Query("UPDATE Bank b SET b.bankBalance = :newBalance WHERE b.id = :bankId")
    void updateBankBalance(@Param("bankId") Integer bankId, @Param("newBalance") Double newBalance);
}

package com.frontend.service;

import com.frontend.entity.Bank;
import com.frontend.repository.BankRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for Bank operations
 * Handles bank account management for payment processing
 */
@Service
public class BankService {

    private static final Logger LOG = LoggerFactory.getLogger(BankService.class);

    @Autowired
    private BankRepository bankRepository;

    /**
     * Get all banks
     */
    public List<Bank> getAllBanks() {
        return bankRepository.findAll();
    }

    /**
     * Get all active banks
     */
    @Cacheable("banks")
    public List<Bank> getActiveBanks() {
        return bankRepository.findActiveBanks();
    }

    /**
     * Get bank by ID
     */
    public Optional<Bank> getBankById(Integer id) {
        return bankRepository.findById(id);
    }

    /**
     * Get bank by account number
     */
    public Optional<Bank> getBankByAccountNo(String accountNo) {
        return bankRepository.findByAccountNo(accountNo);
    }

    /**
     * Get bank by bank code
     */
    public Optional<Bank> getBankByCode(String bankCode) {
        return bankRepository.findByBankCode(bankCode);
    }

    /**
     * Search banks by name
     */
    public List<Bank> searchBanksByName(String bankName) {
        return bankRepository.findByBankNameContainingIgnoreCase(bankName);
    }

    /**
     * Get banks by account type
     */
    public List<Bank> getBanksByAccountType(String accountType) {
        return bankRepository.findByAccountType(accountType);
    }

    /**
     * Save bank
     */
    @Transactional
    @CacheEvict(value = "banks", allEntries = true)
    public Bank saveBank(Bank bank) {
        try {
            // Check for duplicate account number
            if (bank.getId() == null && bankRepository.existsByAccountNo(bank.getAccountNo())) {
                throw new RuntimeException("Account number already exists: " + bank.getAccountNo());
            }

            Bank savedBank = bankRepository.save(bank);
            LOG.info("Bank saved: {} - {}", savedBank.getBankName(), savedBank.getAccountNo());
            return savedBank;

        } catch (Exception e) {
            LOG.error("Error saving bank", e);
            throw new RuntimeException("Error saving bank: " + e.getMessage(), e);
        }
    }

    /**
     * Update bank
     */
    @Transactional
    public Bank updateBank(Bank bank) {
        try {
            if (bank.getId() == null) {
                throw new RuntimeException("Bank ID is required for update");
            }

            Bank updatedBank = bankRepository.save(bank);
            LOG.info("Bank updated: {} - {}", updatedBank.getBankName(), updatedBank.getAccountNo());
            return updatedBank;

        } catch (Exception e) {
            LOG.error("Error updating bank", e);
            throw new RuntimeException("Error updating bank: " + e.getMessage(), e);
        }
    }

    /**
     * Update bank balance
     */
    @Transactional
    public void updateBankBalance(Integer bankId, Double newBalance) {
        try {
            Optional<Bank> optBank = bankRepository.findById(bankId);
            if (optBank.isEmpty()) {
                throw new RuntimeException("Bank not found: " + bankId);
            }

            Bank bank = optBank.get();
            bank.setBankBalance(newBalance);
            bankRepository.save(bank);
            LOG.info("Bank {} balance updated to {}", bankId, newBalance);

        } catch (Exception e) {
            LOG.error("Error updating bank balance", e);
            throw new RuntimeException("Error updating bank balance: " + e.getMessage(), e);
        }
    }

    /**
     * Add amount to bank balance
     */
    @Transactional
    public void addToBalance(Integer bankId, Double amount) {
        try {
            Optional<Bank> optBank = bankRepository.findById(bankId);
            if (optBank.isEmpty()) {
                throw new RuntimeException("Bank not found: " + bankId);
            }

            Bank bank = optBank.get();
            Double currentBalance = bank.getBankBalance() != null ? bank.getBankBalance() : 0.0;
            bank.setBankBalance(currentBalance + amount);
            bankRepository.save(bank);
            LOG.info("Added {} to bank {} balance. New balance: {}", amount, bankId, bank.getBankBalance());

        } catch (Exception e) {
            LOG.error("Error adding to bank balance", e);
            throw new RuntimeException("Error adding to bank balance: " + e.getMessage(), e);
        }
    }

    /**
     * Subtract amount from bank balance
     */
    @Transactional
    public void subtractFromBalance(Integer bankId, Double amount) {
        try {
            Optional<Bank> optBank = bankRepository.findById(bankId);
            if (optBank.isEmpty()) {
                throw new RuntimeException("Bank not found: " + bankId);
            }

            Bank bank = optBank.get();
            Double currentBalance = bank.getBankBalance() != null ? bank.getBankBalance() : 0.0;
            bank.setBankBalance(currentBalance - amount);
            bankRepository.save(bank);
            LOG.info("Subtracted {} from bank {} balance. New balance: {}", amount, bankId, bank.getBankBalance());

        } catch (Exception e) {
            LOG.error("Error subtracting from bank balance", e);
            throw new RuntimeException("Error subtracting from bank balance: " + e.getMessage(), e);
        }
    }

    /**
     * Get total balance of all active banks
     */
    public Double getTotalActiveBalance() {
        Double total = bankRepository.getTotalActiveBalance();
        return total != null ? total : 0.0;
    }

    /**
     * Delete bank
     */
    @Transactional
    public void deleteBank(Integer id) {
        try {
            bankRepository.deleteById(id);
            LOG.info("Bank {} deleted", id);

        } catch (Exception e) {
            LOG.error("Error deleting bank {}", id, e);
            throw new RuntimeException("Error deleting bank: " + e.getMessage(), e);
        }
    }

    /**
     * Deactivate bank (soft delete)
     */
    @Transactional
    public void deactivateBank(Integer id) {
        try {
            Optional<Bank> optBank = bankRepository.findById(id);
            if (optBank.isEmpty()) {
                throw new RuntimeException("Bank not found: " + id);
            }

            Bank bank = optBank.get();
            bank.setStatus("INACTIVE");
            bankRepository.save(bank);
            LOG.info("Bank {} deactivated", id);

        } catch (Exception e) {
            LOG.error("Error deactivating bank {}", id, e);
            throw new RuntimeException("Error deactivating bank: " + e.getMessage(), e);
        }
    }

    /**
     * Activate bank
     */
    @Transactional
    public void activateBank(Integer id) {
        try {
            Optional<Bank> optBank = bankRepository.findById(id);
            if (optBank.isEmpty()) {
                throw new RuntimeException("Bank not found: " + id);
            }

            Bank bank = optBank.get();
            bank.setStatus("ACTIVE");
            bankRepository.save(bank);
            LOG.info("Bank {} activated", id);

        } catch (Exception e) {
            LOG.error("Error activating bank {}", id, e);
            throw new RuntimeException("Error activating bank: " + e.getMessage(), e);
        }
    }

    /**
     * Get bank names for dropdown
     */
    public List<String> getBankNames() {
        List<Bank> banks = bankRepository.findActiveBanks();
        return banks.stream()
                .map(Bank::getDisplayName)
                .toList();
    }
}

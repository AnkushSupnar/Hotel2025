package com.frontend.service;

import com.frontend.entity.Employees;
import com.frontend.repository.EmployeesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Employees operations - uses 'employees' table
 */
@Service
public class EmployeesService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeesService.class);

    @Autowired
    private EmployeesRepository employeesRepository;

    /**
     * Get all employees
     */
    public List<Employees> getAllEmployees() {
        try {
            LOG.info("Fetching all employees");
            return employeesRepository.findAllByOrderByFirstNameAsc();
        } catch (Exception e) {
            LOG.error("Error fetching all employees", e);
            throw new RuntimeException("Error fetching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Get all active employees
     */
    public List<Employees> getActiveEmployees() {
        try {
            LOG.info("Fetching active employees");
            return employeesRepository.findByActiveStatusTrueOrderByFirstNameAsc();
        } catch (Exception e) {
            LOG.error("Error fetching active employees", e);
            throw new RuntimeException("Error fetching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Get employee by ID
     */
    public Employees getEmployeeById(Integer id) {
        try {
            LOG.info("Fetching employee by ID: {}", id);
            return employeesRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));
        } catch (Exception e) {
            LOG.error("Error fetching employee by ID: {}", id, e);
            throw new RuntimeException("Error fetching employee: " + e.getMessage(), e);
        }
    }

    /**
     * Get employee by mobile number
     */
    public Employees getEmployeeByMobile(String mobileNo) {
        try {
            LOG.info("Fetching employee by mobile: {}", mobileNo);
            return employeesRepository.findByMobileNo(mobileNo)
                    .orElseThrow(() -> new RuntimeException("Employee not found with mobile: " + mobileNo));
        } catch (Exception e) {
            LOG.error("Error fetching employee by mobile: {}", mobileNo, e);
            throw new RuntimeException("Error fetching employee: " + e.getMessage(), e);
        }
    }

    /**
     * Search employees by first name
     */
    public List<Employees> searchByFirstName(String firstName) {
        try {
            LOG.info("Searching employees by first name: {}", firstName);
            return employeesRepository.findByFirstNameContainingIgnoreCase(firstName);
        } catch (Exception e) {
            LOG.error("Error searching employees by first name: {}", firstName, e);
            throw new RuntimeException("Error searching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Search employees by name
     */
    public List<Employees> searchByName(String name) {
        try {
            LOG.info("Searching employees by name: {}", name);
            return employeesRepository.searchByName(name);
        } catch (Exception e) {
            LOG.error("Error searching employees by name: {}", name, e);
            throw new RuntimeException("Error searching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Get employees by designation
     */
    public List<Employees> getEmployeesByDesignation(String designation) {
        try {
            LOG.info("Fetching employees by designation: {}", designation);
            return employeesRepository.findByDesignation(designation);
        } catch (Exception e) {
            LOG.error("Error fetching employees by designation: {}", designation, e);
            throw new RuntimeException("Error fetching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Get waiter names for billing
     */
    public List<String> getWaiterNames() {
        try {
            LOG.info("Fetching waiter names");
            List<Employees> waiters = employeesRepository.findActiveWaiters();
            return waiters.stream()
                    .map(Employees::getFirstName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Error fetching waiter names", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get all active waiters
     */
    public List<Employees> getActiveWaiters() {
        try {
            LOG.info("Fetching active waiters");
            return employeesRepository.findActiveWaiters();
        } catch (Exception e) {
            LOG.error("Error fetching active waiters", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get all employee names
     */
    public List<String> getAllEmployeeNames() {
        try {
            LOG.info("Fetching all employee names");
            return employeesRepository.findByActiveStatusTrue().stream()
                    .map(Employees::getFirstName)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Error fetching all employee names", e);
            throw new RuntimeException("Error fetching employee names: " + e.getMessage(), e);
        }
    }

    /**
     * Helper to convert empty strings to null (avoids unique constraint violations)
     */
    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Normalize empty strings to null for fields that may have unique constraints
     */
    private void normalizeEmployee(Employees employee) {
        employee.setMiddleName(emptyToNull(employee.getMiddleName()));
        employee.setLastName(emptyToNull(employee.getLastName()));
        employee.setAlternateMobileNo(emptyToNull(employee.getAlternateMobileNo()));
        employee.setEmailId(emptyToNull(employee.getEmailId()));
        employee.setAddressLine(emptyToNull(employee.getAddressLine()));
        employee.setCity(emptyToNull(employee.getCity()));
        employee.setTaluka(emptyToNull(employee.getTaluka()));
        employee.setDistrict(emptyToNull(employee.getDistrict()));
        employee.setState(emptyToNull(employee.getState()));
        employee.setPincode(emptyToNull(employee.getPincode()));
        employee.setAadharNo(emptyToNull(employee.getAadharNo()));
        employee.setEmergencyContactName(emptyToNull(employee.getEmergencyContactName()));
        employee.setEmergencyContactNo(emptyToNull(employee.getEmergencyContactNo()));
        employee.setRemarks(emptyToNull(employee.getRemarks()));
        employee.setPhotoPath(emptyToNull(employee.getPhotoPath()));
    }

    /**
     * Create new employee
     */
    @Transactional
    public Employees createEmployee(Employees employee) {
        try {
            LOG.info("Creating new employee: {} {}", employee.getFirstName(), employee.getLastName());

            // Normalize empty strings to null to avoid unique constraint violations
            normalizeEmployee(employee);

            // Validate mobile is unique if provided
            if (employee.getMobileNo() != null && !employee.getMobileNo().isEmpty()) {
                if (employeesRepository.existsByMobileNo(employee.getMobileNo())) {
                    throw new RuntimeException("Employee with mobile '" + employee.getMobileNo() + "' already exists");
                }
            }

            // Validate aadhar is unique if provided
            if (employee.getAadharNo() != null && !employee.getAadharNo().isEmpty()) {
                if (employeesRepository.existsByAadharNo(employee.getAadharNo())) {
                    throw new RuntimeException("Employee with Aadhar '" + employee.getAadharNo() + "' already exists");
                }
            }

            Employees savedEmployee = employeesRepository.save(employee);
            LOG.info("Employee created successfully with ID: {}", savedEmployee.getEmployeeId());
            return savedEmployee;

        } catch (Exception e) {
            LOG.error("Error creating employee", e);
            throw new RuntimeException("Error creating employee: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing employee
     */
    @Transactional
    public Employees updateEmployee(Integer id, Employees employee) {
        try {
            LOG.info("Updating employee with ID: {}", id);

            // Normalize empty strings to null to avoid unique constraint violations
            normalizeEmployee(employee);

            Employees existingEmployee = employeesRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

            // Check if mobile is being changed and if new mobile already exists
            if (employee.getMobileNo() != null && !employee.getMobileNo().isEmpty()) {
                if (existingEmployee.getMobileNo() == null || !existingEmployee.getMobileNo().equals(employee.getMobileNo())) {
                    if (employeesRepository.existsByMobileNo(employee.getMobileNo())) {
                        throw new RuntimeException("Employee with mobile '" + employee.getMobileNo() + "' already exists");
                    }
                }
            }

            // Update fields
            existingEmployee.setFirstName(employee.getFirstName());
            existingEmployee.setMiddleName(employee.getMiddleName());
            existingEmployee.setLastName(employee.getLastName());
            existingEmployee.setMobileNo(employee.getMobileNo());
            existingEmployee.setAlternateMobileNo(employee.getAlternateMobileNo());
            existingEmployee.setEmailId(employee.getEmailId());
            existingEmployee.setAddressLine(employee.getAddressLine());
            existingEmployee.setCity(employee.getCity());
            existingEmployee.setTaluka(employee.getTaluka());
            existingEmployee.setDistrict(employee.getDistrict());
            existingEmployee.setState(employee.getState());
            existingEmployee.setPincode(employee.getPincode());
            existingEmployee.setAadharNo(employee.getAadharNo());
            existingEmployee.setDesignation(employee.getDesignation());
            existingEmployee.setCurrentSalary(employee.getCurrentSalary());
            existingEmployee.setDateJoin(employee.getDateJoin());
            existingEmployee.setDateResign(employee.getDateResign());
            existingEmployee.setEmergencyContactName(employee.getEmergencyContactName());
            existingEmployee.setEmergencyContactNo(employee.getEmergencyContactNo());
            existingEmployee.setRemarks(employee.getRemarks());
            existingEmployee.setActiveStatus(employee.getActiveStatus());

            Employees updatedEmployee = employeesRepository.save(existingEmployee);
            LOG.info("Employee updated successfully with ID: {}", updatedEmployee.getEmployeeId());
            return updatedEmployee;

        } catch (Exception e) {
            LOG.error("Error updating employee with ID: {}", id, e);
            throw new RuntimeException("Error updating employee: " + e.getMessage(), e);
        }
    }

    /**
     * Delete employee by ID
     */
    @Transactional
    public void deleteEmployee(Integer id) {
        try {
            LOG.info("Deleting employee with ID: {}", id);

            if (!employeesRepository.existsById(id)) {
                throw new RuntimeException("Employee not found with ID: " + id);
            }

            employeesRepository.deleteById(id);
            LOG.info("Employee deleted successfully with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Error deleting employee with ID: {}", id, e);
            throw new RuntimeException("Error deleting employee: " + e.getMessage(), e);
        }
    }

    /**
     * Deactivate employee (soft delete)
     */
    @Transactional
    public Employees deactivateEmployee(Integer id) {
        try {
            LOG.info("Deactivating employee with ID: {}", id);

            Employees employee = employeesRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

            employee.setActiveStatus(false);
            Employees updatedEmployee = employeesRepository.save(employee);

            LOG.info("Employee deactivated successfully with ID: {}", id);
            return updatedEmployee;

        } catch (Exception e) {
            LOG.error("Error deactivating employee with ID: {}", id, e);
            throw new RuntimeException("Error deactivating employee: " + e.getMessage(), e);
        }
    }

    /**
     * Check if employee exists by ID
     */
    public boolean existsById(Integer id) {
        return employeesRepository.existsById(id);
    }

    /**
     * Check if employee exists by mobile
     */
    public boolean existsByMobile(String mobileNo) {
        return employeesRepository.existsByMobileNo(mobileNo);
    }
}

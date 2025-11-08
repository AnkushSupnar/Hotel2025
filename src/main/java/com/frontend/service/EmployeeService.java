package com.frontend.service;

import com.frontend.entity.Employee;
import com.frontend.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Employee operations
 */
@Service
public class EmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeService.class);

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Get all employees
     */
    public List<Employee> getAllEmployees() {
        try {
            LOG.info("Fetching all employees");
            return employeeRepository.findAllByOrderByFirstNameAsc();
        } catch (Exception e) {
            LOG.error("Error fetching all employees", e);
            throw new RuntimeException("Error fetching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Get employee by ID
     */
    public Employee getEmployeeById(Integer id) {
        try {
            LOG.info("Fetching employee by ID: {}", id);
            return employeeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));
        } catch (Exception e) {
            LOG.error("Error fetching employee by ID: {}", id, e);
            throw new RuntimeException("Error fetching employee: " + e.getMessage(), e);
        }
    }

    /**
     * Get employee by contact
     */
    public Employee getEmployeeByContact(String contact) {
        try {
            LOG.info("Fetching employee by contact: {}", contact);
            return employeeRepository.findByContact(contact)
                    .orElseThrow(() -> new RuntimeException("Employee not found with contact: " + contact));
        } catch (Exception e) {
            LOG.error("Error fetching employee by contact: {}", contact, e);
            throw new RuntimeException("Error fetching employee: " + e.getMessage(), e);
        }
    }

    /**
     * Search employees by first name
     */
    public List<Employee> searchByFirstName(String firstName) {
        try {
            LOG.info("Searching employees by first name: {}", firstName);
            return employeeRepository.findByFirstNameContainingIgnoreCase(firstName);
        } catch (Exception e) {
            LOG.error("Error searching employees by first name: {}", firstName, e);
            throw new RuntimeException("Error searching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Search employees by last name
     */
    public List<Employee> searchByLastName(String lastName) {
        try {
            LOG.info("Searching employees by last name: {}", lastName);
            return employeeRepository.findByLastNameContainingIgnoreCase(lastName);
        } catch (Exception e) {
            LOG.error("Error searching employees by last name: {}", lastName, e);
            throw new RuntimeException("Error searching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Search employees by contact
     */
    public List<Employee> searchByContact(String contact) {
        try {
            LOG.info("Searching employees by contact: {}", contact);
            return employeeRepository.findByContactContaining(contact);
        } catch (Exception e) {
            LOG.error("Error searching employees by contact: {}", contact, e);
            throw new RuntimeException("Error searching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Get employees by designation
     */
    public List<Employee> getEmployeesByDesignation(String designation) {
        try {
            LOG.info("Fetching employees by designation: {}", designation);
            return employeeRepository.findByDesignation(designation);
        } catch (Exception e) {
            LOG.error("Error fetching employees by designation: {}", designation, e);
            throw new RuntimeException("Error fetching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Get employees by status
     */
    public List<Employee> getEmployeesByStatus(String status) {
        try {
            LOG.info("Fetching employees by status: {}", status);
            return employeeRepository.findByStatus(status);
        } catch (Exception e) {
            LOG.error("Error fetching employees by status: {}", status, e);
            throw new RuntimeException("Error fetching employees: " + e.getMessage(), e);
        }
    }

    /**
     * Create new employee
     */
    @Transactional
    public Employee createEmployee(Employee employee) {
        try {
            LOG.info("Creating new employee: {} {} {}", employee.getFirstName(), employee.getMiddleName(), employee.getLastName());

            // Validate contact is unique
            if (employeeRepository.existsByContact(employee.getContact())) {
                throw new RuntimeException("Employee with contact '"+employee.getContact() + "' already exists");
            }

            Employee savedEmployee = employeeRepository.save(employee);

            LOG.info("Employee created successfully with ID: {}", savedEmployee.getId());
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
    public Employee updateEmployee(Integer id, Employee employee) {
        try {
            LOG.info("Updating employee with ID: {}", id);

            Employee existingEmployee = employeeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));

            // Check if contact is being changed and if new contact already exists
            if (!existingEmployee.getContact().equals(employee.getContact())) {
                if (employeeRepository.existsByContact(employee.getContact())) {
                    throw new RuntimeException("Employee with contact '" + employee.getContact() + "' already exists");
                }
            }

            // Update fields
            existingEmployee.setFirstName(employee.getFirstName());
            existingEmployee.setMiddleName(employee.getMiddleName());
            existingEmployee.setLastName(employee.getLastName());
            existingEmployee.setAddress(employee.getAddress());
            existingEmployee.setContact(employee.getContact());
            existingEmployee.setDesignation(employee.getDesignation());
            existingEmployee.setSalary(employee.getSalary());
            existingEmployee.setSalaryType(employee.getSalaryType());
            existingEmployee.setStatus(employee.getStatus());

            Employee updatedEmployee = employeeRepository.save(existingEmployee);

            LOG.info("Employee updated successfully with ID: {}", updatedEmployee.getId());
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

            if (!employeeRepository.existsById(id)) {
                throw new RuntimeException("Employee not found with ID: " + id);
            }

            employeeRepository.deleteById(id);
            LOG.info("Employee deleted successfully with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Error deleting employee with ID: {}", id, e);
            throw new RuntimeException("Error deleting employee: " + e.getMessage(), e);
        }
    }

    /**
     * Check if employee exists by ID
     */
    public boolean existsById(Integer id) {
        return employeeRepository.existsById(id);
    }

    /**
     * Check if employee exists by contact
     */
    public boolean existsByContact(String contact) {
        return employeeRepository.existsByContact(contact);
    }
}

package com.frontend.service;

import com.frontend.entity.Customer;
import com.frontend.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Customer operations
 */
@Service
public class CustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Get all customers
     */
    public List<Customer> getAllCustomers() {
        try {
            LOG.info("Fetching all customers");
            return customerRepository.findAllByOrderByFirstNameAsc();
        } catch (Exception e) {
            LOG.error("Error fetching all customers", e);
            throw new RuntimeException("Error fetching customers: " + e.getMessage(), e);
        }
    }

    /**
     * Get customer by ID
     */
    public Customer getCustomerById(Integer id) {
        try {
            LOG.info("Fetching customer by ID: {}", id);
            return customerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
        } catch (Exception e) {
            LOG.error("Error fetching customer by ID: {}", id, e);
            throw new RuntimeException("Error fetching customer: " + e.getMessage(), e);
        }
    }

    /**
     * Get customer by customer key
     */
    public Customer getCustomerByKey(String customerKey) {
        try {
            LOG.info("Fetching customer by key: {}", customerKey);
            return customerRepository.findByCustomerKey(customerKey)
                    .orElseThrow(() -> new RuntimeException("Customer not found with key: " + customerKey));
        } catch (Exception e) {
            LOG.error("Error fetching customer by key: {}", customerKey, e);
            throw new RuntimeException("Error fetching customer: " + e.getMessage(), e);
        }
    }

    /**
     * Get customer by mobile number
     */
    public Customer getCustomerByMobile(String mobileNo) {
        try {
            LOG.info("Fetching customer by mobile: {}", mobileNo);
            return customerRepository.findByMobileNo(mobileNo)
                    .orElseThrow(() -> new RuntimeException("Customer not found with mobile: " + mobileNo));
        } catch (Exception e) {
            LOG.error("Error fetching customer by mobile: {}", mobileNo, e);
            throw new RuntimeException("Error fetching customer: " + e.getMessage(), e);
        }
    }

    /**
     * Search customers by first name
     */
    public List<Customer> searchByFirstName(String firstName) {
        try {
            LOG.info("Searching customers by first name: {}", firstName);
            return customerRepository.findByFirstNameContainingIgnoreCase(firstName);
        } catch (Exception e) {
            LOG.error("Error searching customers by first name: {}", firstName, e);
            throw new RuntimeException("Error searching customers: " + e.getMessage(), e);
        }
    }

    /**
     * Search customers by last name
     */
    public List<Customer> searchByLastName(String lastName) {
        try {
            LOG.info("Searching customers by last name: {}", lastName);
            return customerRepository.findByLastNameContainingIgnoreCase(lastName);
        } catch (Exception e) {
            LOG.error("Error searching customers by last name: {}", lastName, e);
            throw new RuntimeException("Error searching customers: " + e.getMessage(), e);
        }
    }

    /**
     * Search customers by mobile number
     */
    public List<Customer> searchByMobile(String mobileNo) {
        try {
            LOG.info("Searching customers by mobile: {}", mobileNo);
            return customerRepository.findByMobileNoContaining(mobileNo);
        } catch (Exception e) {
            LOG.error("Error searching customers by mobile: {}", mobileNo, e);
            throw new RuntimeException("Error searching customers: " + e.getMessage(), e);
        }
    }

    /**
     * Get customers by city
     */
    public List<Customer> getCustomersByCity(String city) {
        try {
            LOG.info("Fetching customers by city: {}", city);
            return customerRepository.findByCity(city);
        } catch (Exception e) {
            LOG.error("Error fetching customers by city: {}", city, e);
            throw new RuntimeException("Error fetching customers: " + e.getMessage(), e);
        }
    }

    /**
     * Create new customer
     */
    @Transactional
    public Customer createCustomer(Customer customer) {
        try {
            LOG.info("Creating new customer: {}", customer.getCustomerKey());

            // Validate customer key is unique
            if (customerRepository.existsByCustomerKey(customer.getCustomerKey())) {
                throw new RuntimeException("Customer with key '" + customer.getCustomerKey() + "' already exists");
            }

            // Validate mobile number is unique
            if (customerRepository.existsByMobileNo(customer.getMobileNo())) {
                throw new RuntimeException("Customer with mobile number '" + customer.getMobileNo() + "' already exists");
            }

            // Validate email is unique
            if (customerRepository.existsByEmailId(customer.getEmailId())) {
                throw new RuntimeException("Customer with email '" + customer.getEmailId() + "' already exists");
            }

            Customer savedCustomer = customerRepository.save(customer);

            LOG.info("Customer created successfully with ID: {}", savedCustomer.getId());
            return savedCustomer;

        } catch (Exception e) {
            LOG.error("Error creating customer", e);
            throw new RuntimeException("Error creating customer: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing customer
     */
    @Transactional
    public Customer updateCustomer(Integer id, Customer customer) {
        try {
            LOG.info("Updating customer with ID: {}", id);

            Customer existingCustomer = customerRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));

            // Check if customer key is being changed and if new key already exists
            if (!existingCustomer.getCustomerKey().equals(customer.getCustomerKey())) {
                if (customerRepository.existsByCustomerKey(customer.getCustomerKey())) {
                    throw new RuntimeException("Customer with key '" + customer.getCustomerKey() + "' already exists");
                }
            }

            // Check if mobile number is being changed and if new number already exists
            if (!existingCustomer.getMobileNo().equals(customer.getMobileNo())) {
                if (customerRepository.existsByMobileNo(customer.getMobileNo())) {
                    throw new RuntimeException("Customer with mobile number '" + customer.getMobileNo() + "' already exists");
                }
            }

            // Check if email is being changed and if new email already exists
            if (!existingCustomer.getEmailId().equals(customer.getEmailId())) {
                if (customerRepository.existsByEmailId(customer.getEmailId())) {
                    throw new RuntimeException("Customer with email '" + customer.getEmailId() + "' already exists");
                }
            }

            // Update fields
            existingCustomer.setCustomerKey(customer.getCustomerKey());
            existingCustomer.setFirstName(customer.getFirstName());
            existingCustomer.setMiddleName(customer.getMiddleName());
            existingCustomer.setLastName(customer.getLastName());
            existingCustomer.setMobileNo(customer.getMobileNo());
            existingCustomer.setEmailId(customer.getEmailId());
            existingCustomer.setFlatNo(customer.getFlatNo());
            existingCustomer.setStreetName(customer.getStreetName());
            existingCustomer.setCity(customer.getCity());
            existingCustomer.setDistrict(customer.getDistrict());
            existingCustomer.setTaluka(customer.getTaluka());

            Customer updatedCustomer = customerRepository.save(existingCustomer);

            LOG.info("Customer updated successfully with ID: {}", updatedCustomer.getId());
            return updatedCustomer;

        } catch (Exception e) {
            LOG.error("Error updating customer with ID: {}", id, e);
            throw new RuntimeException("Error updating customer: " + e.getMessage(), e);
        }
    }

    /**
     * Delete customer by ID
     */
    @Transactional
    public void deleteCustomer(Integer id) {
        try {
            LOG.info("Deleting customer with ID: {}", id);

            if (!customerRepository.existsById(id)) {
                throw new RuntimeException("Customer not found with ID: " + id);
            }

            customerRepository.deleteById(id);
            LOG.info("Customer deleted successfully with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Error deleting customer with ID: {}", id, e);
            throw new RuntimeException("Error deleting customer: " + e.getMessage(), e);
        }
    }

    /**
     * Check if customer exists by ID
     */
    public boolean existsById(Integer id) {
        return customerRepository.existsById(id);
    }

    /**
     * Check if customer exists by customer key
     */
    public boolean existsByCustomerKey(String customerKey) {
        return customerRepository.existsByCustomerKey(customerKey);
    }

    /**
     * Check if customer exists by mobile number
     */
    public boolean existsByMobileNo(String mobileNo) {
        return customerRepository.existsByMobileNo(mobileNo);
    }

    /**
     * Check if customer exists by email
     */
    public boolean existsByEmailId(String emailId) {
        return customerRepository.existsByEmailId(emailId);
    }
}

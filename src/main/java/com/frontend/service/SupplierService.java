package com.frontend.service;

import com.frontend.entity.Supplier;
import com.frontend.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Supplier operations
 */
@Service
public class SupplierService {

    private static final Logger LOG = LoggerFactory.getLogger(SupplierService.class);

    @Autowired
    private SupplierRepository supplierRepository;

    /**
     * Get all suppliers
     */
    public List<Supplier> getAllSuppliers() {
        try {
            LOG.info("Fetching all suppliers");
            return supplierRepository.findAllByOrderByNameAsc();
        } catch (Exception e) {
            LOG.error("Error fetching all suppliers", e);
            throw new RuntimeException("Error fetching suppliers: " + e.getMessage(), e);
        }
    }

    /**
     * Get all active suppliers
     */
    public List<Supplier> getActiveSuppliers() {
        try {
            LOG.info("Fetching active suppliers");
            return supplierRepository.findByIsActiveTrueOrderByNameAsc();
        } catch (Exception e) {
            LOG.error("Error fetching active suppliers", e);
            throw new RuntimeException("Error fetching suppliers: " + e.getMessage(), e);
        }
    }

    /**
     * Get supplier by ID
     */
    public Supplier getSupplierById(Integer id) {
        try {
            LOG.info("Fetching supplier by ID: {}", id);
            return supplierRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Supplier not found with ID: " + id));
        } catch (Exception e) {
            LOG.error("Error fetching supplier by ID: {}", id, e);
            throw new RuntimeException("Error fetching supplier: " + e.getMessage(), e);
        }
    }

    /**
     * Get supplier by name
     */
    public Supplier getSupplierByName(String name) {
        try {
            LOG.info("Fetching supplier by name: {}", name);
            return supplierRepository.findByName(name)
                    .orElseThrow(() -> new RuntimeException("Supplier not found with name: " + name));
        } catch (Exception e) {
            LOG.error("Error fetching supplier by name: {}", name, e);
            throw new RuntimeException("Error fetching supplier: " + e.getMessage(), e);
        }
    }

    /**
     * Search suppliers by name
     */
    public List<Supplier> searchByName(String name) {
        try {
            LOG.info("Searching suppliers by name: {}", name);
            return supplierRepository.findByNameContainingIgnoreCase(name);
        } catch (Exception e) {
            LOG.error("Error searching suppliers by name: {}", name, e);
            throw new RuntimeException("Error searching suppliers: " + e.getMessage(), e);
        }
    }

    /**
     * Get suppliers by city
     */
    public List<Supplier> getSuppliersByCity(String city) {
        try {
            LOG.info("Fetching suppliers by city: {}", city);
            return supplierRepository.findByCity(city);
        } catch (Exception e) {
            LOG.error("Error fetching suppliers by city: {}", city, e);
            throw new RuntimeException("Error fetching suppliers: " + e.getMessage(), e);
        }
    }

    /**
     * Create new supplier
     */
    @Transactional
    public Supplier createSupplier(Supplier supplier) {
        try {
            LOG.info("Creating new supplier: {}", supplier.getName());

            // Validate supplier name is unique
            if (supplierRepository.existsByName(supplier.getName())) {
                throw new RuntimeException("Supplier with name '" + supplier.getName() + "' already exists");
            }

            // Validate contact is unique if provided
            if (supplier.getContact() != null && !supplier.getContact().isEmpty()) {
                if (supplierRepository.existsByContact(supplier.getContact())) {
                    throw new RuntimeException("Supplier with contact '" + supplier.getContact() + "' already exists");
                }
            }

            // Validate email is unique if provided
            if (supplier.getEmail() != null && !supplier.getEmail().isEmpty()) {
                if (supplierRepository.existsByEmail(supplier.getEmail())) {
                    throw new RuntimeException("Supplier with email '" + supplier.getEmail() + "' already exists");
                }
            }

            // Validate GST is unique if provided
            if (supplier.getGstNo() != null && !supplier.getGstNo().isEmpty()) {
                if (supplierRepository.existsByGstNo(supplier.getGstNo())) {
                    throw new RuntimeException("Supplier with GST No '" + supplier.getGstNo() + "' already exists");
                }
            }

            Supplier savedSupplier = supplierRepository.save(supplier);

            LOG.info("Supplier created successfully with ID: {}", savedSupplier.getId());
            return savedSupplier;

        } catch (Exception e) {
            LOG.error("Error creating supplier", e);
            throw new RuntimeException("Error creating supplier: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing supplier
     */
    @Transactional
    public Supplier updateSupplier(Integer id, Supplier supplier) {
        try {
            LOG.info("Updating supplier with ID: {}", id);

            Supplier existingSupplier = supplierRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Supplier not found with ID: " + id));

            // Check if name is being changed and if new name already exists
            if (!existingSupplier.getName().equals(supplier.getName())) {
                if (supplierRepository.existsByName(supplier.getName())) {
                    throw new RuntimeException("Supplier with name '" + supplier.getName() + "' already exists");
                }
            }

            // Check if contact is being changed and if new contact already exists
            if (supplier.getContact() != null && !supplier.getContact().isEmpty()) {
                if (existingSupplier.getContact() == null || !existingSupplier.getContact().equals(supplier.getContact())) {
                    if (supplierRepository.existsByContact(supplier.getContact())) {
                        throw new RuntimeException("Supplier with contact '" + supplier.getContact() + "' already exists");
                    }
                }
            }

            // Check if email is being changed and if new email already exists
            if (supplier.getEmail() != null && !supplier.getEmail().isEmpty()) {
                if (existingSupplier.getEmail() == null || !existingSupplier.getEmail().equals(supplier.getEmail())) {
                    if (supplierRepository.existsByEmail(supplier.getEmail())) {
                        throw new RuntimeException("Supplier with email '" + supplier.getEmail() + "' already exists");
                    }
                }
            }

            // Check if GST is being changed and if new GST already exists
            if (supplier.getGstNo() != null && !supplier.getGstNo().isEmpty()) {
                if (existingSupplier.getGstNo() == null || !existingSupplier.getGstNo().equals(supplier.getGstNo())) {
                    if (supplierRepository.existsByGstNo(supplier.getGstNo())) {
                        throw new RuntimeException("Supplier with GST No '" + supplier.getGstNo() + "' already exists");
                    }
                }
            }

            // Update fields
            existingSupplier.setName(supplier.getName());
            existingSupplier.setAddress(supplier.getAddress());
            existingSupplier.setContact(supplier.getContact());
            existingSupplier.setEmail(supplier.getEmail());
            existingSupplier.setGstNo(supplier.getGstNo());
            existingSupplier.setPanNo(supplier.getPanNo());
            existingSupplier.setCity(supplier.getCity());
            existingSupplier.setState(supplier.getState());
            existingSupplier.setPincode(supplier.getPincode());
            existingSupplier.setIsActive(supplier.getIsActive());

            Supplier updatedSupplier = supplierRepository.save(existingSupplier);

            LOG.info("Supplier updated successfully with ID: {}", updatedSupplier.getId());
            return updatedSupplier;

        } catch (Exception e) {
            LOG.error("Error updating supplier with ID: {}", id, e);
            throw new RuntimeException("Error updating supplier: " + e.getMessage(), e);
        }
    }

    /**
     * Delete supplier by ID
     */
    @Transactional
    public void deleteSupplier(Integer id) {
        try {
            LOG.info("Deleting supplier with ID: {}", id);

            if (!supplierRepository.existsById(id)) {
                throw new RuntimeException("Supplier not found with ID: " + id);
            }

            supplierRepository.deleteById(id);
            LOG.info("Supplier deleted successfully with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Error deleting supplier with ID: {}", id, e);
            throw new RuntimeException("Error deleting supplier: " + e.getMessage(), e);
        }
    }

    /**
     * Deactivate supplier (soft delete)
     */
    @Transactional
    public Supplier deactivateSupplier(Integer id) {
        try {
            LOG.info("Deactivating supplier with ID: {}", id);

            Supplier supplier = supplierRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Supplier not found with ID: " + id));

            supplier.setIsActive(false);
            Supplier updatedSupplier = supplierRepository.save(supplier);

            LOG.info("Supplier deactivated successfully with ID: {}", id);
            return updatedSupplier;

        } catch (Exception e) {
            LOG.error("Error deactivating supplier with ID: {}", id, e);
            throw new RuntimeException("Error deactivating supplier: " + e.getMessage(), e);
        }
    }

    /**
     * Activate supplier
     */
    @Transactional
    public Supplier activateSupplier(Integer id) {
        try {
            LOG.info("Activating supplier with ID: {}", id);

            Supplier supplier = supplierRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Supplier not found with ID: " + id));

            supplier.setIsActive(true);
            Supplier updatedSupplier = supplierRepository.save(supplier);

            LOG.info("Supplier activated successfully with ID: {}", id);
            return updatedSupplier;

        } catch (Exception e) {
            LOG.error("Error activating supplier with ID: {}", id, e);
            throw new RuntimeException("Error activating supplier: " + e.getMessage(), e);
        }
    }

    /**
     * Check if supplier exists by ID
     */
    public boolean existsById(Integer id) {
        return supplierRepository.existsById(id);
    }

    /**
     * Check if supplier exists by name
     */
    public boolean existsByName(String name) {
        return supplierRepository.existsByName(name);
    }

    /**
     * Check if supplier exists by contact
     */
    public boolean existsByContact(String contact) {
        return supplierRepository.existsByContact(contact);
    }
}

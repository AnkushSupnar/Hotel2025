package com.frontend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontend.entity.ApplicationSetting;
import com.frontend.entity.TableMaster;
import com.frontend.repository.TableMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for TableMaster operations
 */
@Service
public class TableMasterService {

    private static final Logger LOG = LoggerFactory.getLogger(TableMasterService.class);
    private static final String SECTION_SEQUENCE_SETTING = "section_sequence";

    @Autowired
    private TableMasterRepository tableMasterRepository;

    @Autowired
    private ApplicationSettingService applicationSettingService;

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get all tables
     */
    @Cacheable("tables")
    public List<TableMaster> getAllTables() {
        try {
            LOG.info("Fetching all tables");
            return tableMasterRepository.findAllByOrderByTableNameAsc();
        } catch (Exception e) {
            LOG.error("Error fetching all tables", e);
            throw new RuntimeException("Error fetching tables: " + e.getMessage(), e);
        }
    }

    /**
     * Get table by ID
     */
    public TableMaster getTableById(Integer id) {
        try {
            LOG.info("Fetching table by ID: {}", id);
            return tableMasterRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Table not found with ID: " + id));
        } catch (Exception e) {
            LOG.error("Error fetching table by ID: {}", id, e);
            throw new RuntimeException("Error fetching table: " + e.getMessage(), e);
        }
    }

    /**
     * Get table by name
     */
    public TableMaster getTableByName(String tableName) {
        try {
            LOG.info("Fetching table by name: {}", tableName);
            return tableMasterRepository.findByTableName(tableName)
                    .orElseThrow(() -> new RuntimeException("Table not found with name: " + tableName));
        } catch (Exception e) {
            LOG.error("Error fetching table by name: {}", tableName, e);
            throw new RuntimeException("Error fetching table: " + e.getMessage(), e);
        }
    }

    /**
     * Search tables by name
     */
    public List<TableMaster> searchTablesByName(String name) {
        try {
            LOG.info("Searching tables by name: {}", name);
            return tableMasterRepository.findByTableNameContainingIgnoreCase(name);
        } catch (Exception e) {
            LOG.error("Error searching tables by name: {}", name, e);
            throw new RuntimeException("Error searching tables: " + e.getMessage(), e);
        }
    }

    /**
     * Search tables by description
     */
    public List<TableMaster> searchTablesByDescription(String description) {
        try {
            LOG.info("Searching tables by description: {}", description);
            return tableMasterRepository.findByDescriptionContainingIgnoreCase(description);
        } catch (Exception e) {
            LOG.error("Error searching tables by description: {}", description, e);
            throw new RuntimeException("Error searching tables: " + e.getMessage(), e);
        }
    }

    /**
     * Create new table
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public TableMaster createTable(TableMaster tableMaster) {
        try {
            LOG.info("Creating new table: {}", tableMaster.getTableName());

            // Validate table name is unique
            if (tableMasterRepository.existsByTableName(tableMaster.getTableName())) {
                throw new RuntimeException("Table with name '" + tableMaster.getTableName() + "' already exists");
            }

            TableMaster savedTable = tableMasterRepository.save(tableMaster);

            LOG.info("Table created successfully with ID: {}", savedTable.getId());
            return savedTable;

        } catch (Exception e) {
            LOG.error("Error creating table", e);
            throw new RuntimeException("Error creating table: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing table
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public TableMaster updateTable(Integer id, TableMaster tableMaster) {
        try {
            LOG.info("Updating table with ID: {}", id);

            TableMaster existingTable = tableMasterRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Table not found with ID: " + id));

            // Check if name is being changed and if new name already exists
            if (!existingTable.getTableName().equals(tableMaster.getTableName())) {
                if (tableMasterRepository.existsByTableName(tableMaster.getTableName())) {
                    throw new RuntimeException("Table with name '" + tableMaster.getTableName() + "' already exists");
                }
            }

            // Update fields
            existingTable.setTableName(tableMaster.getTableName());
            existingTable.setDescription(tableMaster.getDescription());

            TableMaster updatedTable = tableMasterRepository.save(existingTable);

            LOG.info("Table updated successfully with ID: {}", updatedTable.getId());
            return updatedTable;

        } catch (Exception e) {
            LOG.error("Error updating table with ID: {}", id, e);
            throw new RuntimeException("Error updating table: " + e.getMessage(), e);
        }
    }

    /**
     * Delete table by ID
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void deleteTable(Integer id) {
        try {
            LOG.info("Deleting table with ID: {}", id);

            if (!tableMasterRepository.existsById(id)) {
                throw new RuntimeException("Table not found with ID: " + id);
            }

            tableMasterRepository.deleteById(id);
            LOG.info("Table deleted successfully with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Error deleting table with ID: {}", id, e);
            throw new RuntimeException("Error deleting table: " + e.getMessage(), e);
        }
    }

    /**
     * Check if table exists by ID
     */
    public boolean existsById(Integer id) {
        return tableMasterRepository.existsById(id);
    }

    /**
     * Check if table exists by name
     */
    public boolean existsByTableName(String tableName) {
        return tableMasterRepository.existsByTableName(tableName);
    }

    /**
     * Get all unique descriptions (sections) from tablemaster
     */
    public List<String> getUniqueDescriptions() {
        try {
            LOG.info("Fetching unique descriptions from tablemaster");
            return tableMasterRepository.findDistinctDescriptions();
        } catch (Exception e) {
            LOG.error("Error fetching unique descriptions", e);
            throw new RuntimeException("Error fetching descriptions: " + e.getMessage(), e);
        }
    }

    /**
     * Get all tables for a specific description (section)
     */
    public List<TableMaster> getTablesByDescription(String description) {
        try {
            LOG.info("Fetching tables for description: {}", description);
            return tableMasterRepository.findByDescription(description);
        } catch (Exception e) {
            LOG.error("Error fetching tables for description: {}", description, e);
            throw new RuntimeException("Error fetching tables: " + e.getMessage(), e);
        }
    }

    /**
     * Get all unique descriptions (sections) ordered by configured sequence
     * Falls back to alphabetical order if no sequence is configured
     */
    public List<String> getUniqueDescriptionsOrdered() {
        try {
            LOG.info("Fetching unique descriptions ordered by sequence");
            List<String> sections = tableMasterRepository.findDistinctDescriptions();

            // Load sequence settings
            Map<String, Integer> sequenceMap = loadSectionSequences();

            if (sequenceMap.isEmpty()) {
                LOG.info("No section sequence configured, using default alphabetical order");
                return sections;
            }

            // Sort sections by sequence
            sections.sort(Comparator.comparingInt(section ->
                sequenceMap.getOrDefault(section, Integer.MAX_VALUE)));

            LOG.info("Sections ordered by sequence: {}", sections);
            return sections;

        } catch (Exception e) {
            LOG.error("Error fetching ordered descriptions", e);
            // Fallback to default method
            return getUniqueDescriptions();
        }
    }

    /**
     * Load section sequence settings from database
     */
    private Map<String, Integer> loadSectionSequences() {
        Map<String, Integer> sequences = new HashMap<>();

        try {
            Optional<ApplicationSetting> settingOpt = applicationSettingService.getSettingByName(SECTION_SEQUENCE_SETTING);

            if (settingOpt.isPresent()) {
                String jsonValue = settingOpt.get().getSettingValue();
                if (jsonValue != null && !jsonValue.trim().isEmpty()) {
                    sequences = objectMapper.readValue(jsonValue, new TypeReference<Map<String, Integer>>() {});
                    LOG.info("Loaded section sequences: {}", sequences);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error loading section sequences: {}", e.getMessage());
        }

        return sequences;
    }
}

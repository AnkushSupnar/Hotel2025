package com.frontend.service;

import com.frontend.dto.CategoryMasterDto;
import com.frontend.entity.CategoryMaster;
import com.frontend.repository.CategoryMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryApiService {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryApiService.class);

    private final CategoryMasterRepository categoryRepository;
    private final SessionService sessionService;

    @Autowired
    public CategoryApiService(CategoryMasterRepository categoryRepository, SessionService sessionService) {
        this.categoryRepository = categoryRepository;
        this.sessionService = sessionService;
    }

    /**
     * Get all categories from database
     */
    public List<CategoryMasterDto> getAllCategories() {
        LOG.debug("Fetching all categories from database");

        List<CategoryMaster> categories = categoryRepository.findAll();
        LOG.info("Successfully fetched {} categories", categories.size());

        // Convert entities to DTOs
        return convertToDto(categories);
    }

    /**
     * Convert entity to DTO
     */
    private CategoryMasterDto convertToDto(CategoryMaster entity) {
        CategoryMasterDto dto = new CategoryMasterDto();
        dto.setId(entity.getId());
        dto.setCategory(entity.getCategory());
        dto.setStock(entity.getStock());
        return dto;
    }

    /**
     * Convert entity list to DTO list
     */
    private List<CategoryMasterDto> convertToDto(List<CategoryMaster> entities) {
        List<CategoryMasterDto> dtos = new ArrayList<>();
        for (CategoryMaster entity : entities) {
            dtos.add(convertToDto(entity));
        }
        return dtos;
    }

    /**
     * Convert DTO to entity
     */
    private CategoryMaster convertToEntity(CategoryMasterDto dto) {
        CategoryMaster entity = new CategoryMaster();
        if (dto.getId() != null) {
            entity.setId(dto.getId());
        }
        entity.setCategory(dto.getCategory());
        entity.setStock(dto.getStock());
        return entity;
    }

    /**
     * Create new category
     */
    public CategoryMasterDto createCategory(CategoryMasterDto categoryDto) {
        LOG.debug("Creating new category: {}", categoryDto.getCategory());

        // Check if user is logged in
        if (!SessionService.isLoggedIn()) {
            throw new RuntimeException("User not logged in. Please login first.");
        }

        // Check if category already exists
        if (categoryRepository.existsByCategory(categoryDto.getCategory())) {
            LOG.error("Category already exists: {}", categoryDto.getCategory());
            throw new RuntimeException("Category already exists");
        }

        CategoryMaster entity = convertToEntity(categoryDto);
        CategoryMaster saved = categoryRepository.save(entity);

        LOG.info("Category created successfully: {}", categoryDto.getCategory());
        return convertToDto(saved);
    }

    /**
     * Update existing category
     */
    public CategoryMasterDto updateCategory(Integer id, CategoryMasterDto categoryDto) {
        LOG.debug("Updating category with ID: {}", id);

        // Check if user is logged in
        if (!SessionService.isLoggedIn()) {
            throw new RuntimeException("User not logged in. Please login first.");
        }

        Optional<CategoryMaster> existingOptional = categoryRepository.findById(id);
        if (existingOptional.isEmpty()) {
            LOG.error("Category not found with ID: {}", id);
            throw new RuntimeException("Category not found");
        }

        CategoryMaster existing = existingOptional.get();
        existing.setCategory(categoryDto.getCategory());
        existing.setStock(categoryDto.getStock());

        CategoryMaster updated = categoryRepository.save(existing);

        LOG.info("Category updated successfully: {}", categoryDto.getCategory());
        return convertToDto(updated);
    }

    /**
     * Delete category by ID
     */
    public boolean deleteCategory(Integer id) {
        LOG.debug("Deleting category with ID: {}", id);

        // Check if user is logged in
        if (!SessionService.isLoggedIn()) {
            throw new RuntimeException("User not logged in. Please login first.");
        }

        if (!categoryRepository.existsById(id)) {
            LOG.error("Category not found with ID: {}", id);
            throw new RuntimeException("Category not found");
        }

        categoryRepository.deleteById(id);
        LOG.info("Category deleted successfully with ID: {}", id);
        return true;
    }

    /**
     * Get category by ID
     */
    public CategoryMasterDto getCategoryById(Integer id) {
        LOG.debug("Fetching category with ID: {}", id);

        // Check if user is logged in
        if (!SessionService.isLoggedIn()) {
            throw new RuntimeException("User not logged in. Please login first.");
        }

        Optional<CategoryMaster> categoryOptional = categoryRepository.findById(id);
        if (categoryOptional.isEmpty()) {
            LOG.error("Category not found with ID: {}", id);
            throw new RuntimeException("Category not found");
        }

        LOG.info("Category found with ID: {}", id);
        return convertToDto(categoryOptional.get());
    }

    /**
     * Search category by name
     */
    public Optional<CategoryMaster> getCategoryByName(String name) {
        LOG.debug("Searching category by name: {}", name);

        Optional<CategoryMaster> categoryOptional = categoryRepository.findByCategoryIgnoreCase(name);

        LOG.info("Category found with name: {}", name);
        return categoryOptional;
    }
}
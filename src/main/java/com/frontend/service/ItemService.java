package com.frontend.service;

import com.frontend.dto.ItemDto;
import com.frontend.entity.Item;
import com.frontend.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Item operations
 */
@Service
public class ItemService {

    private static final Logger LOG = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;

    /**
     * Get all items with categories
     */
    public List<ItemDto> getAllItems() {
        try {
            LOG.info("Fetching all items with categories");
            List<Item> items = itemRepository.findAllWithCategory();
            return items.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Error fetching all items", e);
            throw new RuntimeException("Error fetching items: " + e.getMessage(), e);
        }
    }

    /**
     * Get item by ID
     */
    public ItemDto getItemById(Integer id) {
        try {
            LOG.info("Fetching item by ID: {}", id);
            Item item = itemRepository.findByIdWithCategory(id)
                    .orElseThrow(() -> new RuntimeException("Item not found with ID: " + id));
            return convertToDto(item);
        } catch (Exception e) {
            LOG.error("Error fetching item by ID: {}", id, e);
            throw new RuntimeException("Error fetching item: " + e.getMessage(), e);
        }
    }

    /**
     * Get items by category ID
     */
    public List<ItemDto> getItemsByCategoryId(Integer categoryId) {
        try {
            LOG.info("Fetching items by category ID: {}", categoryId);
            List<Item> items = itemRepository.findByCategoryIdWithCategory(categoryId);
            return items.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Error fetching items by category ID: {}", categoryId, e);
            throw new RuntimeException("Error fetching items: " + e.getMessage(), e);
        }
    }

    /**
     * Search items by name
     */
    public List<ItemDto> searchItemsByName(String name) {
        try {
            LOG.info("Searching items by name: {}", name);
            List<Item> items = itemRepository.findByItemNameContainingIgnoreCaseWithCategory(name);
            return items.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Error searching items by name: {}", name, e);
            throw new RuntimeException("Error searching items: " + e.getMessage(), e);
        }
    }

    /**
     * Create new item
     */
    @Transactional
    public ItemDto createItem(ItemDto itemDto) {
        try {
            LOG.info("Creating new item: {}", itemDto.getItemName());

            // Validate item name is unique
            if (itemRepository.existsByItemName(itemDto.getItemName())) {
                throw new RuntimeException("Item with name '" + itemDto.getItemName() + "' already exists");
            }

            // Validate item code is unique
            if (itemRepository.existsByItemCode(itemDto.getItemCode())) {
                throw new RuntimeException("Item with code '" + itemDto.getItemCode() + "' already exists");
            }

            Item item = convertToEntity(itemDto);
            Item savedItem = itemRepository.save(item);

            LOG.info("Item created successfully with ID: {}", savedItem.getId());
            return convertToDto(savedItem);

        } catch (Exception e) {
            LOG.error("Error creating item", e);
            throw new RuntimeException("Error creating item: " + e.getMessage(), e);
        }
    }

    /**
     * Update existing item
     */
    @Transactional
    public ItemDto updateItem(Integer id, ItemDto itemDto) {
        try {
            LOG.info("Updating item with ID: {}", id);

            Item existingItem = itemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Item not found with ID: " + id));

            // Check if name is being changed and if new name already exists
            if (!existingItem.getItemName().equals(itemDto.getItemName())) {
                if (itemRepository.existsByItemName(itemDto.getItemName())) {
                    throw new RuntimeException("Item with name '" + itemDto.getItemName() + "' already exists");
                }
            }

            // Check if item code is being changed and if new code already exists
            if (!existingItem.getItemCode().equals(itemDto.getItemCode())) {
                if (itemRepository.existsByItemCode(itemDto.getItemCode())) {
                    throw new RuntimeException("Item with code '" + itemDto.getItemCode() + "' already exists");
                }
            }

            // Update fields
            existingItem.setItemName(itemDto.getItemName());
            existingItem.setCategoryId(itemDto.getCategoryId());
            existingItem.setRate(itemDto.getRate());
            existingItem.setItemCode(itemDto.getItemCode());

            Item updatedItem = itemRepository.save(existingItem);

            LOG.info("Item updated successfully with ID: {}", updatedItem.getId());
            return convertToDto(updatedItem);

        } catch (Exception e) {
            LOG.error("Error updating item with ID: {}", id, e);
            throw new RuntimeException("Error updating item: " + e.getMessage(), e);
        }
    }

    /**
     * Delete item by ID
     */
    @Transactional
    public void deleteItem(Integer id) {
        try {
            LOG.info("Deleting item with ID: {}", id);

            if (!itemRepository.existsById(id)) {
                throw new RuntimeException("Item not found with ID: " + id);
            }

            itemRepository.deleteById(id);
            LOG.info("Item deleted successfully with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Error deleting item with ID: {}", id, e);
            throw new RuntimeException("Error deleting item: " + e.getMessage(), e);
        }
    }

    /**
     * Convert Item entity to DTO
     */
    private ItemDto convertToDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setItemName(item.getItemName());
        dto.setCategoryId(item.getCategoryId());
        dto.setRate(item.getRate());
        dto.setItemCode(item.getItemCode());

        // Set category name if category is loaded
        if (item.getCategory() != null) {
            dto.setCategoryName(item.getCategory().getCategory());
        }

        return dto;
    }

    /**
     * Convert DTO to Item entity
     */
    private Item convertToEntity(ItemDto dto) {
        Item item = new Item();
        item.setId(dto.getId());
        item.setItemName(dto.getItemName());
        item.setCategoryId(dto.getCategoryId());
        item.setRate(dto.getRate());
        item.setItemCode(dto.getItemCode());
        return item;
    }
}

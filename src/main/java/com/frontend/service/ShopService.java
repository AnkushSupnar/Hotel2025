package com.frontend.service;

import com.frontend.entity.Shop;
import com.frontend.repository.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing shop/restaurant information
 */
@Service
public class ShopService {

    private static final Logger LOG = LoggerFactory.getLogger(ShopService.class);

    @Autowired
    private ShopRepository shopRepository;

    /**
     * Create a new shop
     */
    @Transactional
    public Shop createShop(Shop shop) {
        LOG.info("Creating new shop: {}", shop.getRestaurantName());

        // Validate unique constraints
        if (shopRepository.existsByRestaurantName(shop.getRestaurantName())) {
            LOG.error("Shop creation failed: Restaurant name already exists - {}", shop.getRestaurantName());
            throw new IllegalArgumentException("Restaurant name already exists: " + shop.getRestaurantName());
        }

        if (shop.getContactNumber() != null && shopRepository.existsByContactNumber(shop.getContactNumber())) {
            LOG.error("Shop creation failed: Contact number already exists - {}", shop.getContactNumber());
            throw new IllegalArgumentException("Contact number already exists: " + shop.getContactNumber());
        }

        if (shop.getLicenseKey() != null && !shop.getLicenseKey().isEmpty()
            && shopRepository.existsByLicenseKey(shop.getLicenseKey())) {
            LOG.error("Shop creation failed: License key already exists - {}", shop.getLicenseKey());
            throw new IllegalArgumentException("License key already exists: " + shop.getLicenseKey());
        }

        Shop savedShop = shopRepository.save(shop);
        LOG.info("Shop created successfully with ID: {}", savedShop.getShopId());

        return savedShop;
    }

    /**
     * Get all shops
     */
    public List<Shop> getAllShops() {
        LOG.debug("Fetching all shops");
        return shopRepository.findAll();
    }

    /**
     * Get shop by ID
     */
    public Optional<Shop> getShopById(Long shopId) {
        LOG.debug("Fetching shop by ID: {}", shopId);
        return shopRepository.findById(shopId);
    }

    /**
     * Get shop by restaurant name
     */
    public Optional<Shop> getShopByRestaurantName(String restaurantName) {
        LOG.debug("Fetching shop by restaurant name: {}", restaurantName);
        return shopRepository.findByRestaurantName(restaurantName);
    }

    /**
     * Get shop by contact number
     */
    public Optional<Shop> getShopByContactNumber(String contactNumber) {
        LOG.debug("Fetching shop by contact number: {}", contactNumber);
        return shopRepository.findByContactNumber(contactNumber);
    }

    /**
     * Get the first shop (useful for single-shop applications)
     */
    public Optional<Shop> getFirstShop() {
        LOG.debug("Fetching first shop");
        return shopRepository.findFirstShop();
    }

    /**
     * Update shop information
     */
    @Transactional
    public Shop updateShop(Long shopId, Shop updatedShop) {
        LOG.info("Updating shop: {}", shopId);

        Shop existingShop = shopRepository.findById(shopId)
            .orElseThrow(() -> new IllegalArgumentException("Shop not found with ID: " + shopId));

        // Validate unique constraints if changed
        if (!existingShop.getRestaurantName().equals(updatedShop.getRestaurantName())
            && shopRepository.existsByRestaurantName(updatedShop.getRestaurantName())) {
            throw new IllegalArgumentException("Restaurant name already exists: " + updatedShop.getRestaurantName());
        }

        if (!existingShop.getContactNumber().equals(updatedShop.getContactNumber())
            && shopRepository.existsByContactNumber(updatedShop.getContactNumber())) {
            throw new IllegalArgumentException("Contact number already exists: " + updatedShop.getContactNumber());
        }

        if (updatedShop.getLicenseKey() != null && !updatedShop.getLicenseKey().isEmpty()
            && !updatedShop.getLicenseKey().equals(existingShop.getLicenseKey())
            && shopRepository.existsByLicenseKey(updatedShop.getLicenseKey())) {
            throw new IllegalArgumentException("License key already exists: " + updatedShop.getLicenseKey());
        }

        // Update fields
        existingShop.setRestaurantName(updatedShop.getRestaurantName());
        existingShop.setAddress(updatedShop.getAddress());
        existingShop.setContactNumber(updatedShop.getContactNumber());
        existingShop.setLicenseKey(updatedShop.getLicenseKey());
        existingShop.setOwnerName(updatedShop.getOwnerName());

        Shop savedShop = shopRepository.save(existingShop);
        LOG.info("Shop updated successfully: {}", shopId);

        return savedShop;
    }

    /**
     * Delete shop by ID
     */
    @Transactional
    public void deleteShop(Long shopId) {
        LOG.info("Deleting shop: {}", shopId);

        if (!shopRepository.existsById(shopId)) {
            throw new IllegalArgumentException("Shop not found with ID: " + shopId);
        }

        shopRepository.deleteById(shopId);
        LOG.info("Shop deleted successfully: {}", shopId);
    }

    /**
     * Check if any shop exists
     */
    public boolean shopExists() {
        return shopRepository.count() > 0;
    }

    /**
     * Count total shops
     */
    public long countShops() {
        return shopRepository.countShops();
    }

    /**
     * Check if restaurant name exists
     */
    public boolean restaurantNameExists(String restaurantName) {
        return shopRepository.existsByRestaurantName(restaurantName);
    }
}

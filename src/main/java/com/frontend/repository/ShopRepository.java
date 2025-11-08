package com.frontend.repository;

import com.frontend.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    /**
     * Find shop by restaurant name
     */
    Optional<Shop> findByRestaurantName(String restaurantName);

    /**
     * Find shop by contact number
     */
    Optional<Shop> findByContactNumber(String contactNumber);

    /**
     * Find shop by license key
     */
    Optional<Shop> findByLicenseKey(String licenseKey);

    /**
     * Check if restaurant name exists
     */
    boolean existsByRestaurantName(String restaurantName);

    /**
     * Check if contact number exists
     */
    boolean existsByContactNumber(String contactNumber);

    /**
     * Check if license key exists
     */
    boolean existsByLicenseKey(String licenseKey);

    /**
     * Get the first shop (for single shop applications)
     */
    @Query("SELECT s FROM Shop s ORDER BY s.shopId ASC")
    Optional<Shop> findFirstShop();

    /**
     * Count total shops
     */
    @Query("SELECT COUNT(s) FROM Shop s")
    long countShops();
}

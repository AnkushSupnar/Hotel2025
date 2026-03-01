package com.frontend.config;

import com.frontend.entity.Shop;
import com.frontend.service.SessionService;
import com.frontend.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Initializes server-side session data when running in 'server' profile.
 *
 * On the desktop, the user selects a shop during login which sets SessionService.currentShop.
 * For the server/API profile, there is no interactive login, so the shop and application
 * settings must be loaded automatically at startup. Without this, BillPrint, BillPrintWithLogo,
 * and KOTOrderPrint would get null for restaurant name, address, contacts, etc.
 */
@Component
@Profile("server")
public class ServerInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(ServerInitializer.class);

    @Autowired
    private ShopService shopService;

    @Autowired
    private SessionService sessionService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeServerSession() {
        LOG.info("Server profile detected - auto-loading shop and application settings for API...");

        try {
            // Load the first shop from database (same as desktop shop selection)
            Optional<Shop> shopOpt = shopService.getFirstShop();
            if (shopOpt.isPresent()) {
                Shop shop = shopOpt.get();
                // Set session with null user (no desktop login) but with the shop
                // This populates currentShop so BillPrint/KOTOrderPrint get restaurant info
                sessionService.setServerSession(shop);
                LOG.info("Server session initialized with shop: '{}' (ID: {})",
                        shop.getRestaurantName(), shop.getShopId());
            } else {
                LOG.warn("No shop found in database! Bill/KOT PDFs will not show restaurant info. " +
                        "Please create a shop from the desktop application.");
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize server session: {}", e.getMessage(), e);
        }
    }
}

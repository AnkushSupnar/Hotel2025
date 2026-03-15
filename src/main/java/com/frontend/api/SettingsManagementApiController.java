package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.entity.MobileAppSetting;
import com.frontend.entity.MobileFeatureAccess;
import com.frontend.entity.Role;
import com.frontend.entity.Shop;
import com.frontend.enums.ScreenPermission;
import com.frontend.repository.TableMasterRepository;
import com.frontend.service.ApplicationSettingService;
import com.frontend.service.MobileAppSettingService;
import com.frontend.service.RoleService;
import com.frontend.service.ShopService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API Controller for Settings Management (Shops, Roles, Sections, Mobile Settings).
 * Mirrors desktop setting controllers for web UI usage.
 * Only active in 'server' profile.
 */
@RestController
@RequestMapping("/api/v1")
@Profile("server")
public class SettingsManagementApiController {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsManagementApiController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ShopService shopService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ApplicationSettingService applicationSettingService;

    @Autowired
    private MobileAppSettingService mobileAppSettingService;

    @Autowired
    private TableMasterRepository tableMasterRepository;

    // ==================== SHOP / RESTAURANT ====================

    @GetMapping("/shops")
    public ResponseEntity<ApiResponse> getAllShops() {
        try {
            List<Shop> shops = shopService.getAllShops();
            List<Map<String, Object>> shopList = new ArrayList<>();
            for (Shop s : shops) {
                shopList.add(shopToMap(s));
            }
            return ResponseEntity.ok(new ApiResponse("Shops retrieved", true, shopList));
        } catch (Exception e) {
            LOG.error("Error getting shops: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @GetMapping("/shops/{id}")
    public ResponseEntity<ApiResponse> getShopById(@PathVariable Long id) {
        try {
            Optional<Shop> shop = shopService.getShopById(id);
            if (shop.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Shop not found", false, null));
            }
            return ResponseEntity.ok(new ApiResponse("Shop retrieved", true, shopToMap(shop.get())));
        } catch (Exception e) {
            LOG.error("Error getting shop {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @PostMapping("/shops")
    public ResponseEntity<ApiResponse> createShop(@RequestBody Map<String, String> body) {
        try {
            Shop shop = new Shop();
            shop.setRestaurantName(body.get("restaurantName"));
            shop.setSubTitle(body.get("subTitle"));
            shop.setOwnerName(body.get("ownerName"));
            shop.setAddress(body.get("address"));
            shop.setContactNumber(body.get("contactNumber"));
            shop.setContactNumber2(body.get("contactNumber2"));
            shop.setGstinNumber(body.get("gstinNumber"));
            shop.setLicenseKey(body.get("licenseKey"));

            Shop saved = shopService.createShop(shop);
            LOG.info("Shop created: {}", saved.getRestaurantName());
            return ResponseEntity.ok(new ApiResponse("Restaurant saved successfully", true, shopToMap(saved)));
        } catch (Exception e) {
            LOG.error("Error creating shop: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(e.getMessage(), false, null));
        }
    }

    @PutMapping("/shops/{id}")
    public ResponseEntity<ApiResponse> updateShop(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Shop shop = new Shop();
            shop.setRestaurantName(body.get("restaurantName"));
            shop.setSubTitle(body.get("subTitle"));
            shop.setOwnerName(body.get("ownerName"));
            shop.setAddress(body.get("address"));
            shop.setContactNumber(body.get("contactNumber"));
            shop.setContactNumber2(body.get("contactNumber2"));
            shop.setGstinNumber(body.get("gstinNumber"));
            shop.setLicenseKey(body.get("licenseKey"));

            Shop updated = shopService.updateShop(id, shop);
            LOG.info("Shop updated: {}", updated.getRestaurantName());
            return ResponseEntity.ok(new ApiResponse("Restaurant updated successfully", true, shopToMap(updated)));
        } catch (Exception e) {
            LOG.error("Error updating shop {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(e.getMessage(), false, null));
        }
    }

    @DeleteMapping("/shops/{id}")
    public ResponseEntity<ApiResponse> deleteShop(@PathVariable Long id) {
        try {
            shopService.deleteShop(id);
            LOG.info("Shop deleted: {}", id);
            return ResponseEntity.ok(new ApiResponse("Restaurant deleted successfully", true, null));
        } catch (Exception e) {
            LOG.error("Error deleting shop {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(e.getMessage(), false, null));
        }
    }

    @GetMapping("/shops/search")
    public ResponseEntity<ApiResponse> searchShops(@RequestParam("q") String query) {
        try {
            List<Shop> all = shopService.getAllShops();
            String q = query.toLowerCase().trim();
            List<Map<String, Object>> results = all.stream()
                    .filter(s -> (s.getRestaurantName() != null && s.getRestaurantName().toLowerCase().contains(q))
                            || (s.getContactNumber() != null && s.getContactNumber().contains(q))
                            || (s.getOwnerName() != null && s.getOwnerName().toLowerCase().contains(q)))
                    .map(this::shopToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse("Search results", true, results));
        } catch (Exception e) {
            LOG.error("Error searching shops: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    private Map<String, Object> shopToMap(Shop s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shopId", s.getShopId());
        m.put("restaurantName", s.getRestaurantName());
        m.put("subTitle", s.getSubTitle());
        m.put("ownerName", s.getOwnerName());
        m.put("address", s.getAddress());
        m.put("contactNumber", s.getContactNumber());
        m.put("contactNumber2", s.getContactNumber2());
        m.put("gstinNumber", s.getGstinNumber());
        m.put("licenseKey", s.getLicenseKey());
        return m;
    }

    // ==================== ROLES / USER RIGHTS ====================

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse> getAllRoles() {
        try {
            List<Role> roles = roleService.getAllRoles();
            List<Map<String, Object>> roleList = new ArrayList<>();
            for (Role r : roles) {
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("roleId", r.getRoleId());
                rm.put("roleName", r.getRoleName());
                rm.put("rights", r.getRights());
                roleList.add(rm);
            }
            return ResponseEntity.ok(new ApiResponse("Roles retrieved", true, roleList));
        } catch (Exception e) {
            LOG.error("Error getting roles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @GetMapping("/screen-permissions")
    public ResponseEntity<ApiResponse> getScreenPermissions() {
        try {
            Map<String, List<Map<String, String>>> categorized = new LinkedHashMap<>();
            String[] categories = {"DASHBOARD", "SALES", "PURCHASE", "MASTER", "EMPLOYEE_SERVICE", "REPORTS", "SETTINGS"};
            for (String cat : categories) {
                ScreenPermission[] perms = ScreenPermission.getByCategory(cat);
                List<Map<String, String>> permList = new ArrayList<>();
                for (ScreenPermission sp : perms) {
                    Map<String, String> pm = new LinkedHashMap<>();
                    pm.put("name", sp.name());
                    pm.put("displayName", sp.getDisplayName());
                    pm.put("category", sp.getCategory());
                    pm.put("description", sp.getDescription());
                    permList.add(pm);
                }
                categorized.put(cat, permList);
            }
            return ResponseEntity.ok(new ApiResponse("Screen permissions retrieved", true, categorized));
        } catch (Exception e) {
            LOG.error("Error getting screen permissions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @GetMapping("/roles/{roleName}/permissions")
    public ResponseEntity<ApiResponse> getRolePermissions(@PathVariable String roleName) {
        try {
            Set<ScreenPermission> perms = roleService.getScreenPermissions(roleName);
            List<String> permNames = perms.stream().map(ScreenPermission::name).collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse("Role permissions retrieved", true, permNames));
        } catch (Exception e) {
            LOG.error("Error getting permissions for role {}: {}", roleName, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(e.getMessage(), false, null));
        }
    }

    @PutMapping("/roles/{roleName}/permissions")
    public ResponseEntity<ApiResponse> updateRolePermissions(@PathVariable String roleName, @RequestBody List<String> permissionNames) {
        try {
            Set<ScreenPermission> perms = new HashSet<>();
            for (String name : permissionNames) {
                try {
                    perms.add(ScreenPermission.valueOf(name));
                } catch (IllegalArgumentException ignored) {
                    LOG.warn("Unknown permission: {}", name);
                }
            }
            roleService.updateScreenPermissions(roleName, perms);
            LOG.info("Updated permissions for role {}: {} permissions", roleName, perms.size());
            return ResponseEntity.ok(new ApiResponse("Permissions saved successfully", true, permissionNames));
        } catch (Exception e) {
            LOG.error("Error updating permissions for role {}: {}", roleName, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(e.getMessage(), false, null));
        }
    }

    // ==================== TABLE SECTION SEQUENCE ====================

    @GetMapping("/sections")
    public ResponseEntity<ApiResponse> getDistinctSections() {
        try {
            List<String> sections = tableMasterRepository.findDistinctDescriptions();
            return ResponseEntity.ok(new ApiResponse("Sections retrieved", true, sections));
        } catch (Exception e) {
            LOG.error("Error getting sections: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @GetMapping("/settings/section-sequence")
    public ResponseEntity<ApiResponse> getSectionSequence() {
        try {
            Map<String, Object> result = new LinkedHashMap<>();

            String seqJson = applicationSettingService.getSettingByName("section_sequence")
                    .map(s -> s.getSettingValue()).orElse(null);
            String groupsJson = applicationSettingService.getSettingByName("section_row_groups_setting")
                    .map(s -> s.getSettingValue()).orElse(null);

            if (seqJson != null) {
                result.put("sectionSequence", objectMapper.readValue(seqJson, Map.class));
            } else {
                result.put("sectionSequence", new LinkedHashMap<>());
            }

            if (groupsJson != null) {
                result.put("rowGroups", objectMapper.readValue(groupsJson, List.class));
            } else {
                result.put("rowGroups", new ArrayList<>());
            }

            return ResponseEntity.ok(new ApiResponse("Section sequence retrieved", true, result));
        } catch (Exception e) {
            LOG.error("Error getting section sequence: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @PostMapping("/settings/section-sequence")
    public ResponseEntity<ApiResponse> saveSectionSequence(@RequestBody Map<String, Object> body) {
        try {
            Object sectionSequence = body.get("sectionSequence");
            Object rowGroups = body.get("rowGroups");

            if (sectionSequence != null) {
                String json = objectMapper.writeValueAsString(sectionSequence);
                applicationSettingService.saveSetting("section_sequence", json);
                LOG.info("Saved section_sequence");
            }
            if (rowGroups != null) {
                String json = objectMapper.writeValueAsString(rowGroups);
                applicationSettingService.saveSetting("section_row_groups_setting", json);
                LOG.info("Saved section_row_groups_setting");
            }

            return ResponseEntity.ok(new ApiResponse("Section sequence saved successfully", true, null));
        } catch (Exception e) {
            LOG.error("Error saving section sequence: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    // ==================== MOBILE APP SETTINGS ====================

    @GetMapping("/mobile-settings")
    public ResponseEntity<ApiResponse> getMobileSettings() {
        try {
            List<MobileAppSetting> settings = mobileAppSettingService.getAllSettings();
            List<Map<String, Object>> settingsList = new ArrayList<>();
            for (MobileAppSetting s : settings) {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("settingKey", s.getSettingKey());
                sm.put("settingValue", s.getSettingValue());
                sm.put("settingType", s.getSettingType());
                sm.put("description", s.getDescription());
                settingsList.add(sm);
            }
            return ResponseEntity.ok(new ApiResponse("Mobile settings retrieved", true, settingsList));
        } catch (Exception e) {
            LOG.error("Error getting mobile settings: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @PostMapping("/mobile-settings")
    public ResponseEntity<ApiResponse> saveMobileSettings(@RequestBody Map<String, String> settings) {
        try {
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String type = inferSettingType(key, value);
                mobileAppSettingService.saveSetting(key, value, type, null);
                LOG.info("Saved mobile setting: {} = {}", key, value);
            }
            return ResponseEntity.ok(new ApiResponse("Mobile settings saved", true, null));
        } catch (Exception e) {
            LOG.error("Error saving mobile settings: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @GetMapping("/mobile-settings/features/{role}")
    public ResponseEntity<ApiResponse> getFeatureAccess(@PathVariable String role) {
        try {
            List<MobileFeatureAccess> features = mobileAppSettingService.getFeatureAccessByRole(role);
            List<Map<String, Object>> featureList = new ArrayList<>();

            // If no features found, return all features with defaults
            if (features.isEmpty()) {
                Map<String, String> featureDefs = MobileAppSettingService.FEATURE_DEFINITIONS;
                for (Map.Entry<String, String> entry : featureDefs.entrySet()) {
                    Map<String, Object> fm = new LinkedHashMap<>();
                    fm.put("featureCode", entry.getKey());
                    fm.put("featureName", entry.getValue());
                    fm.put("isEnabled", "ADMIN".equals(role));
                    fm.put("category", MobileAppSettingService.FEATURE_CATEGORIES.getOrDefault(entry.getKey(), "Other"));
                    featureList.add(fm);
                }
            } else {
                for (MobileFeatureAccess fa : features) {
                    Map<String, Object> fm = new LinkedHashMap<>();
                    fm.put("featureCode", fa.getFeatureCode());
                    fm.put("featureName", fa.getFeatureName());
                    fm.put("isEnabled", fa.getIsEnabled());
                    fm.put("category", MobileAppSettingService.FEATURE_CATEGORIES.getOrDefault(fa.getFeatureCode(), "Other"));
                    featureList.add(fm);
                }
            }

            return ResponseEntity.ok(new ApiResponse("Feature access retrieved", true, featureList));
        } catch (Exception e) {
            LOG.error("Error getting feature access for {}: {}", role, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @PostMapping("/mobile-settings/features/{role}")
    public ResponseEntity<ApiResponse> saveFeatureAccess(@PathVariable String role, @RequestBody Map<String, Boolean> features) {
        try {
            Map<String, String> featureDefs = MobileAppSettingService.FEATURE_DEFINITIONS;
            for (Map.Entry<String, Boolean> entry : features.entrySet()) {
                String featureCode = entry.getKey();
                boolean enabled = entry.getValue();
                String featureName = featureDefs.getOrDefault(featureCode, featureCode);
                mobileAppSettingService.saveFeatureAccess(role, featureCode, featureName, enabled);
            }
            LOG.info("Saved feature access for role {}: {} features", role, features.size());
            return ResponseEntity.ok(new ApiResponse("Feature access saved", true, null));
        } catch (Exception e) {
            LOG.error("Error saving feature access for {}: {}", role, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    @PostMapping("/mobile-settings/regenerate-secret")
    public ResponseEntity<ApiResponse> regenerateSecret() {
        try {
            String newSecret = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
            mobileAppSettingService.saveSetting("JWT_SECRET_KEY", newSecret, "STRING", "JWT signing secret key");
            LOG.info("JWT secret regenerated");
            return ResponseEntity.ok(new ApiResponse("JWT secret regenerated", true, newSecret));
        } catch (Exception e) {
            LOG.error("Error regenerating JWT secret: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed: " + e.getMessage(), false, null));
        }
    }

    private String inferSettingType(String key, String value) {
        if (key.contains("ENABLED")) return "BOOLEAN";
        if (key.contains("DAYS") || key.contains("HOURS")) return "INTEGER";
        return "STRING";
    }
}

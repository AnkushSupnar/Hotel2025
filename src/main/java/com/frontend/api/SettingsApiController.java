package com.frontend.api;

import com.frontend.dto.ApiResponse;
import com.frontend.entity.ApplicationSetting;
import com.frontend.entity.Bank;
import com.frontend.service.ApplicationSettingService;
import com.frontend.service.BankService;
import com.frontend.service.SessionService;
import com.frontend.util.ApplicationSettingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * REST API Controller for Application Settings
 * Provides endpoints for managing application settings and configuration.
 *
 * Mirrors desktop ApplicationSettingController behavior:
 * - document_directory, input_font_path, bill_logo_image, use_bill_logo → saved to DB
 * - billing_printer, kot_printer, default_billing_bank → saved to local properties file
 *   (ApplicationSetting.properties in the document directory) for machine-specific config
 * - GET /all merges DB settings with local properties file values (local overrides DB)
 *
 * Only active in 'server' profile.
 */
@RestController
@RequestMapping("/api/v1/settings")
@Profile("server")
public class SettingsApiController {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsApiController.class);

    @Autowired
    private ApplicationSettingService applicationSettingService;

    @Autowired
    private BankService bankService;

    @Autowired
    private SessionService sessionService;

    /**
     * Get all application settings as a key-value map.
     * Returns only global (DB-stored) settings.
     * Machine-specific settings (printers, bank) are managed client-side in localStorage.
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllSettings() {
        try {
            List<ApplicationSetting> settings = applicationSettingService.getAllSettings();
            Map<String, String> settingsMap = new LinkedHashMap<>();
            for (ApplicationSetting setting : settings) {
                settingsMap.put(setting.getSettingName(), setting.getSettingValue());
            }

            LOG.info("Retrieved {} application settings from DB", settingsMap.size());
            return ResponseEntity.ok(new ApiResponse("Settings retrieved successfully", true, settingsMap));
        } catch (Exception e) {
            LOG.error("Error retrieving settings: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to retrieve settings: " + e.getMessage(), false, null));
        }
    }

    /**
     * Save application settings to database.
     * Only handles global settings (document_directory, input_font_path, bill_logo_image, use_bill_logo).
     * Machine-specific settings (printers, bank) are managed client-side in localStorage.
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse> saveSettings(@RequestBody Map<String, String> settings) {
        try {
            int savedCount = 0;

            for (Map.Entry<String, String> entry : settings.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null || value.trim().isEmpty()) {
                    LOG.debug("Skipping empty setting: {}", key);
                    continue;
                }

                applicationSettingService.saveSetting(key, value);
                LOG.info("Saved '{}' to database", key);
                savedCount++;
            }

            sessionService.reloadApplicationSettings();
            LOG.info("Saved {} application settings", savedCount);
            return ResponseEntity.ok(new ApiResponse("Settings saved successfully", true, savedCount));
        } catch (Exception e) {
            LOG.error("Error saving settings: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to save settings: " + e.getMessage(), false, null));
        }
    }

    /**
     * Get list of active banks for billing configuration
     */
    @GetMapping("/banks")
    public ResponseEntity<ApiResponse> getActiveBanks() {
        try {
            List<Bank> banks = bankService.getActiveBanks();
            List<Map<String, Object>> bankList = new ArrayList<>();
            for (Bank bank : banks) {
                Map<String, Object> bankMap = new LinkedHashMap<>();
                bankMap.put("id", bank.getId());
                bankMap.put("name", bank.getBankName());
                bankMap.put("upiId", bank.getUpiId());
                bankList.add(bankMap);
            }
            LOG.info("Retrieved {} active banks", bankList.size());
            return ResponseEntity.ok(new ApiResponse("Banks retrieved successfully", true, bankList));
        } catch (Exception e) {
            LOG.error("Error retrieving banks: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to retrieve banks: " + e.getMessage(), false, null));
        }
    }

    /**
     * Get list of available printers on the server machine.
     * Matches desktop ApplicationSettingController.loadAvailablePrinters behavior.
     */
    @GetMapping("/printers")
    public ResponseEntity<ApiResponse> getAvailablePrinters() {
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            List<String> printerNames = new ArrayList<>();
            for (PrintService ps : printServices) {
                printerNames.add(ps.getName());
            }
            LOG.info("Retrieved {} available printers", printerNames.size());
            return ResponseEntity.ok(new ApiResponse("Printers retrieved successfully", true, printerNames));
        } catch (Exception e) {
            LOG.error("Error retrieving printers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to retrieve printers: " + e.getMessage(), false, null));
        }
    }

    /**
     * Upload a bill logo image file
     */
    @PostMapping("/upload-logo")
    public ResponseEntity<ApiResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("No file provided", false, null));
            }

            // Validate that the uploaded file is an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("File must be an image (JPEG, PNG, etc.)", false, null));
            }

            // Get the document directory from settings
            Optional<ApplicationSetting> docDirSetting = applicationSettingService.getSettingByName("document_directory");
            if (docDirSetting.isEmpty() || docDirSetting.get().getSettingValue() == null
                    || docDirSetting.get().getSettingValue().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Document directory is not configured. Please set 'document_directory' in settings first.", false, null));
            }

            String documentDirectory = docDirSetting.get().getSettingValue().trim();
            Path dirPath = Paths.get(documentDirectory);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                LOG.info("Created document directory: {}", documentDirectory);
            }

            // Determine file extension from original filename
            String originalFilename = file.getOriginalFilename();
            String extension = "png"; // default
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            // Save the file as billlogo.{ext}
            String logoFilename = "billlogo." + extension;
            Path logoPath = dirPath.resolve(logoFilename);
            Files.copy(file.getInputStream(), logoPath, StandardCopyOption.REPLACE_EXISTING);

            // Save the logo path to settings (DB)
            String savedPath = logoPath.toString();
            applicationSettingService.saveSetting("bill_logo_image", savedPath);
            sessionService.reloadApplicationSettings();

            LOG.info("Bill logo uploaded successfully: {}", savedPath);
            return ResponseEntity.ok(new ApiResponse("Logo uploaded successfully", true, savedPath));
        } catch (IOException e) {
            LOG.error("Error uploading logo file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to upload logo: " + e.getMessage(), false, null));
        } catch (Exception e) {
            LOG.error("Unexpected error uploading logo: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse("Failed to upload logo: " + e.getMessage(), false, null));
        }
    }
}

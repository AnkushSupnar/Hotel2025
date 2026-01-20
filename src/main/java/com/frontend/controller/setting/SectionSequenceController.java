package com.frontend.controller.setting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frontend.config.SpringFXMLLoader;
import com.frontend.service.ApplicationSettingService;
import com.frontend.service.TableMasterService;
import com.frontend.view.AlertNotification;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for Table Section Sequence settings
 * Allows users to drag and drop to set display order for table sections in BillingFrame
 */
@Component
public class SectionSequenceController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SectionSequenceController.class);

    public static final String SECTION_SEQUENCE_SETTING = "section_sequence";

    @Autowired
    private SpringFXMLLoader loader;

    @Autowired
    private ApplicationSettingService applicationSettingService;

    @Autowired
    private TableMasterService tableMasterService;

    @Autowired
    private AlertNotification alertNotification;

    @FXML
    private Button btnBack;

    @FXML
    private VBox sectionsContainer;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnReset;

    private ObservableList<String> orderedSections = FXCollections.observableArrayList();
    private ObjectMapper objectMapper = new ObjectMapper();

    // Drag and drop state
    private HBox draggedRow = null;
    private int draggedIndex = -1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBackButton();
        setupEventHandlers();
        loadSections();
    }

    private void setupBackButton() {
        btnBack.setOnAction(e -> {
            try {
                LOG.info("Back button clicked - returning to Settings Menu");
                navigateToSettingMenu();
            } catch (Exception ex) {
                LOG.error("Error returning to Settings Menu: ", ex);
            }
        });
    }

    private void navigateToSettingMenu() {
        try {
            BorderPane mainPane = getMainPane();
            if (mainPane != null) {
                Pane pane = loader.getPage("/fxml/setting/SettingMenu.fxml");
                mainPane.setCenter(pane);
                LOG.info("Successfully navigated to Settings Menu");
            }
        } catch (Exception e) {
            LOG.error("Error navigating to Settings Menu: ", e);
        }
    }

    private BorderPane getMainPane() {
        try {
            return (BorderPane) sectionsContainer.getScene().lookup("#mainPane");
        } catch (Exception e) {
            LOG.warn("Could not find main pane, navigation might not work properly");
            return null;
        }
    }

    private void setupEventHandlers() {
        btnSave.setOnAction(e -> saveSequences());
        btnReset.setOnAction(e -> resetSequences());
    }

    /**
     * Load all sections and create UI rows for each with drag-drop support
     */
    private void loadSections() {
        try {
            LOG.info("Loading sections for sequence configuration");

            // Get all unique sections from tables
            List<String> allSections = tableMasterService.getUniqueDescriptions();

            if (allSections.isEmpty()) {
                LOG.warn("No sections found in database");
                showNoSectionsMessage();
                return;
            }

            // Load existing sequence settings and order sections
            Map<String, Integer> existingSequences = loadExistingSequences();

            // Order sections based on saved sequence
            orderedSections.clear();
            if (!existingSequences.isEmpty()) {
                // Sort by saved sequence
                List<String> sortedSections = new ArrayList<>(allSections);
                sortedSections.sort((a, b) -> {
                    int seqA = existingSequences.getOrDefault(a, Integer.MAX_VALUE);
                    int seqB = existingSequences.getOrDefault(b, Integer.MAX_VALUE);
                    return Integer.compare(seqA, seqB);
                });
                orderedSections.addAll(sortedSections);
            } else {
                orderedSections.addAll(allSections);
            }

            // Render the UI
            renderSectionRows();

            LOG.info("Loaded {} sections for sequence configuration", orderedSections.size());

        } catch (Exception e) {
            LOG.error("Error loading sections: ", e);
            alertNotification.showError("Error loading sections: " + e.getMessage());
        }
    }

    /**
     * Render all section rows with drag-drop support
     */
    private void renderSectionRows() {
        sectionsContainer.getChildren().clear();

        for (int i = 0; i < orderedSections.size(); i++) {
            String section = orderedSections.get(i);
            HBox row = createDraggableSectionRow(section, i);
            sectionsContainer.getChildren().add(row);

            // Add separator between rows (not after the last one)
            if (i < orderedSections.size() - 1) {
                Separator separator = new Separator();
                separator.getStyleClass().add("setting-separator");
                sectionsContainer.getChildren().add(separator);
            }
        }
    }

    /**
     * Create a draggable row for a section
     */
    private HBox createDraggableSectionRow(String sectionName, int index) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(15);
        row.getStyleClass().addAll("setting-row", "draggable-row");
        row.setUserData(sectionName);

        // Sequence number label
        Label seqLabel = new Label(String.valueOf(index + 1));
        seqLabel.setMinWidth(30);
        seqLabel.setMaxWidth(30);
        seqLabel.setAlignment(Pos.CENTER);
        seqLabel.getStyleClass().add("sequence-number-label");
        seqLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #667eea; " +
                "-fx-background-color: rgba(102, 126, 234, 0.1); -fx-background-radius: 15; " +
                "-fx-min-width: 30; -fx-min-height: 30; -fx-alignment: center;");

        // Drag handle icon
        FontAwesomeIcon dragIcon = new FontAwesomeIcon();
        dragIcon.setGlyphName("BARS");
        dragIcon.setSize("1.2em");
        dragIcon.setFill(Color.web("#9E9E9E"));
        dragIcon.setStyle("-fx-cursor: move;");

        // Section name icon
        FontAwesomeIcon icon = new FontAwesomeIcon();
        icon.setGlyphName("TH_LARGE");
        icon.setSize("1.3em");
        icon.setFill(Color.web("#26A69A"));

        // Section name label
        VBox labelBox = new VBox();
        labelBox.setSpacing(2);

        Label titleLabel = new Label(sectionName);
        titleLabel.getStyleClass().add("setting-row-title");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600;");

        Label countLabel = new Label(getTableCountText(sectionName));
        countLabel.getStyleClass().add("setting-row-value");

        labelBox.getChildren().addAll(titleLabel, countLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Drag hint
        Label dragHint = new Label("Drag to reorder");
        dragHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #BDBDBD;");

        row.getChildren().addAll(seqLabel, dragIcon, icon, labelBox, spacer, dragHint);
        row.setPadding(new Insets(16, 24, 16, 24));

        // Setup drag and drop handlers
        setupDragHandlers(row, index);

        return row;
    }

    /**
     * Setup drag and drop event handlers for a row
     */
    private void setupDragHandlers(HBox row, int index) {
        // Drag detected - start drag
        row.setOnDragDetected(event -> {
            LOG.debug("Drag detected on row: {}", row.getUserData());

            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(index));
            db.setContent(content);

            draggedRow = row;
            draggedIndex = index;

            // Visual feedback
            row.setStyle(row.getStyle() + "-fx-opacity: 0.5;");

            event.consume();
        });

        // Drag over - allow drop
        row.setOnDragOver(event -> {
            if (event.getGestureSource() != row && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);

                // Visual feedback for drop target
                row.setStyle(row.getStyle().replace("-fx-border-color: #667eea; -fx-border-width: 2 0 0 0;", "") +
                        "-fx-border-color: #667eea; -fx-border-width: 2 0 0 0;");
            }
            event.consume();
        });

        // Drag exited - remove visual feedback
        row.setOnDragExited(event -> {
            row.setStyle(row.getStyle().replace("-fx-border-color: #667eea; -fx-border-width: 2 0 0 0;", ""));
            event.consume();
        });

        // Drop - perform reorder
        row.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString()) {
                int sourceIndex = Integer.parseInt(db.getString());
                int targetIndex = orderedSections.indexOf(row.getUserData());

                if (sourceIndex != targetIndex && sourceIndex >= 0 && targetIndex >= 0) {
                    // Reorder the list
                    String movedSection = orderedSections.remove(sourceIndex);
                    orderedSections.add(targetIndex, movedSection);

                    LOG.info("Moved section '{}' from position {} to {}", movedSection, sourceIndex + 1, targetIndex + 1);

                    // Re-render UI
                    renderSectionRows();
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });

        // Drag done - cleanup
        row.setOnDragDone(event -> {
            if (draggedRow != null) {
                draggedRow.setStyle(draggedRow.getStyle().replace("-fx-opacity: 0.5;", ""));
            }
            draggedRow = null;
            draggedIndex = -1;
            event.consume();
        });
    }

    /**
     * Get table count text for a section
     */
    private String getTableCountText(String sectionName) {
        try {
            int count = tableMasterService.getTablesByDescription(sectionName).size();
            return count + " table" + (count != 1 ? "s" : "");
        } catch (Exception e) {
            return "Unknown tables";
        }
    }

    /**
     * Show message when no sections are found
     */
    private void showNoSectionsMessage() {
        sectionsContainer.getChildren().clear();

        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(40));

        Label messageLabel = new Label("No sections found. Please add tables with sections first.");
        messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #9E9E9E;");

        messageBox.getChildren().add(messageLabel);
        sectionsContainer.getChildren().add(messageBox);
    }

    /**
     * Load existing sequence settings from database
     */
    private Map<String, Integer> loadExistingSequences() {
        Map<String, Integer> sequences = new HashMap<>();

        try {
            var settingOpt = applicationSettingService.getSettingByName(SECTION_SEQUENCE_SETTING);

            if (settingOpt.isPresent()) {
                String jsonValue = settingOpt.get().getSettingValue();
                if (jsonValue != null && !jsonValue.trim().isEmpty()) {
                    sequences = objectMapper.readValue(jsonValue, new TypeReference<Map<String, Integer>>() {});
                    LOG.info("Loaded existing section sequences: {}", sequences);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error loading existing sequences, using defaults: {}", e.getMessage());
        }

        return sequences;
    }

    /**
     * Save sequence settings to database based on current order
     */
    private void saveSequences() {
        try {
            Map<String, Integer> sequences = new HashMap<>();

            // Build sequence map from current order
            for (int i = 0; i < orderedSections.size(); i++) {
                sequences.put(orderedSections.get(i), i + 1);
            }

            // Convert to JSON and save
            String jsonValue = objectMapper.writeValueAsString(sequences);
            applicationSettingService.saveSetting(SECTION_SEQUENCE_SETTING, jsonValue);

            LOG.info("Section sequences saved successfully: {}", jsonValue);
            alertNotification.showSuccess("Section order saved successfully!");

        } catch (Exception e) {
            LOG.error("Error saving section sequences: ", e);
            alertNotification.showError("Error saving order: " + e.getMessage());
        }
    }

    /**
     * Reset sequences to default alphabetical order
     */
    private void resetSequences() {
        try {
            // Get fresh list in alphabetical order
            List<String> sections = tableMasterService.getUniqueDescriptions();
            orderedSections.clear();
            orderedSections.addAll(sections);

            // Re-render UI
            renderSectionRows();

            LOG.info("Section sequences reset to default alphabetical order");
            alertNotification.showSuccess("Order reset to default (alphabetical)");

        } catch (Exception e) {
            LOG.error("Error resetting sequences: ", e);
            alertNotification.showError("Error resetting order: " + e.getMessage());
        }
    }
}

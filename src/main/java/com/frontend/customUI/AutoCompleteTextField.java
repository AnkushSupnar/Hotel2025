package com.frontend.customUI;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AutoCompleteTextField - Modern Material Design Autocomplete Component
 *
 * Features:
 * - Popup + ListView for reliable suggestions display
 * - startsWith() or contains() filtering (configurable)
 * - Custom font support for TextField and suggestions
 * - Smooth fade animations for popup
 * - Keyboard navigation (UP/DOWN/ENTER/TAB/ESCAPE)
 * - Mouse click selection
 * - Debounced typing (150ms)
 * - Selection callback support
 * - SPACE/ENTER on empty shows all suggestions
 * - Material Design styling
 *
 * @author Hotel2025 Team
 */
public class AutoCompleteTextField {

    // ==================== Fields ====================

    @Getter
    private final TextField textField;

    // Popup UI components
    private final Popup listPopup = new Popup();
    private final ListView<String> listView = new ListView<>();
    private final ObservableList<String> listItems = FXCollections.observableArrayList();
    private final StackPane popupContainer;

    // Data
    private List<String> suggestions = new ArrayList<>();
    private List<String> filteredSuggestions = new ArrayList<>();

    // State
    private boolean isSelectingSuggestion = false;
    private int hoveredIndex = -1;

    // Configuration
    private Font customFont;
    private double fontSize = 14.0;
    private boolean useContainsFilter = false; // false = startsWith, true = contains

    @Setter @Getter
    private Control nextFocusField;

    // Callbacks
    @Setter
    private Consumer<String> onSelectionCallback;

    // Debounce
    private final PauseTransition debounce = new PauseTransition(Duration.millis(150));

    // Selected value property for binding
    private final StringProperty selectedValue = new SimpleStringProperty("");

    // ==================== Material Design Colors ====================

    private static final String PRIMARY_COLOR = "#1976D2";
    private static final String PRIMARY_LIGHT = "#E3F2FD";
    private static final String ACCENT_COLOR = "#7B1FA2";
    private static final String TEXT_PRIMARY = "#212121";
    private static final String TEXT_SECONDARY = "#757575";
    private static final String SURFACE_COLOR = "#FFFFFF";
    private static final String DIVIDER_COLOR = "#E0E0E0";
    private static final String HOVER_COLOR = "#F5F5F5";
    private static final String SELECTED_COLOR = "#E3F2FD";

    // ==================== Layout Constants ====================

    private static final double ROW_HEIGHT = 36.0;
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final double MIN_POPUP_WIDTH = 250.0;
    private static final double POPUP_PADDING = 4.0;

    // ==================== Constructors ====================

    public AutoCompleteTextField(TextField textField, List<String> suggestions) {
        this.textField = textField;
        this.suggestions = new ArrayList<>(suggestions);
        this.customFont = null;
        this.popupContainer = createPopupContainer();
        initialize();
    }

    public AutoCompleteTextField(TextField textField, List<String> suggestions, Font customFont) {
        this.textField = textField;
        this.suggestions = new ArrayList<>(suggestions);
        this.customFont = customFont;
        this.fontSize = customFont != null ? customFont.getSize() : 14.0;
        this.popupContainer = createPopupContainer();
        initialize();
    }

    public AutoCompleteTextField(TextField textField) {
        this(textField, new ArrayList<>());
    }

    public AutoCompleteTextField(TextField textField, List<String> suggestions, Control nextFocusField) {
        this(textField, suggestions);
        this.nextFocusField = nextFocusField;
    }

    public AutoCompleteTextField(TextField textField, List<String> suggestions, Font customFont, Control nextFocusField) {
        this(textField, suggestions, customFont);
        this.nextFocusField = nextFocusField;
    }

    // ==================== Initialization ====================

    private void initialize() {
        initializeListView();
        initializePopup();
        styleTextField();
        attachListeners();
    }

    private StackPane createPopupContainer() {
        StackPane container = new StackPane();
        container.setStyle(
            "-fx-background-color: " + SURFACE_COLOR + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + DIVIDER_COLOR + ";" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1;"
        );

        // Add drop shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        shadow.setRadius(12);
        shadow.setOffsetY(4);
        container.setEffect(shadow);

        return container;
    }

    private void initializeListView() {
        listView.setItems(listItems);
        listView.setFocusTraversable(false);
        listView.setFixedCellSize(ROW_HEIGHT);
        listView.setCellFactory(lv -> new SuggestionCell());

        // Remove default ListView styling
        listView.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-padding: " + POPUP_PADDING + ";" +
            "-fx-background-insets: 0;"
        );

        // Track mouse hover for visual feedback
        listView.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            int index = getIndexAtY(event.getY());
            if (index != hoveredIndex && index >= 0 && index < listItems.size()) {
                hoveredIndex = index;
                listView.refresh();
            }
        });

        listView.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            hoveredIndex = -1;
            listView.refresh();
        });
    }

    private void initializePopup() {
        popupContainer.getChildren().add(listView);
        listPopup.getContent().add(popupContainer);
        listPopup.setAutoHide(true);
        listPopup.setAutoFix(true);

        // Set initial opacity for fade animation
        popupContainer.setOpacity(0);
    }

    private void styleTextField() {
        if (customFont != null) {
            textField.setFont(customFont);
        }

        // Only apply styles if not already styled
        if (textField.getStyle() == null || textField.getStyle().isEmpty()) {
            textField.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;" +
                "-fx-background-radius: 0;" +
                "-fx-padding: 8 6;" +
                "-fx-text-fill: " + TEXT_PRIMARY + ";" +
                "-fx-prompt-text-fill: " + TEXT_SECONDARY + ";"
            );
        }
    }

    // ==================== Event Listeners ====================

    private void attachListeners() {
        // Debounced text change listener
        textField.textProperty().addListener((obs, oldText, newText) -> {
            if (isSelectingSuggestion) return;

            debounce.stop();
            debounce.setOnFinished(e -> handleTextChange(newText));
            debounce.playFromStart();
        });

        // Keyboard navigation
        textField.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();

            switch (code) {
                case SPACE -> {
                    // SPACE on empty text shows all suggestions
                    if (textField.getText() == null || textField.getText().isEmpty()) {
                        showAllSuggestions();
                        event.consume();
                    }
                }
                case ESCAPE -> {
                    hidePopup();
                    event.consume();
                }
                case ENTER -> {
                    handleEnterKey();
                    event.consume();
                }
                case DOWN -> {
                    handleDownKey();
                    event.consume();
                }
                case UP -> {
                    handleUpKey();
                    event.consume();
                }
                case TAB -> {
                    if (listPopup.isShowing() && !listItems.isEmpty()) {
                        int sel = listView.getSelectionModel().getSelectedIndex();
                        if (sel >= 0) {
                            selectSuggestion(sel);
                        }
                        event.consume();
                    }
                }
                default -> { }
            }
        });

        // Mouse click selection
        listView.setOnMouseClicked(event -> {
            int index = listView.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                selectSuggestion(index);
            }
        });

        // Hide popup when TextField loses focus
        textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                Platform.runLater(() -> {
                    // Small delay to allow click events on ListView
                    PauseTransition delay = new PauseTransition(Duration.millis(150));
                    delay.setOnFinished(e -> {
                        if (!textField.isFocused()) {
                            hidePopup();
                        }
                    });
                    delay.play();
                });
            }
        });

        // Handle window movement - reposition popup
        textField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.xProperty().addListener((o, ov, nv) -> repositionPopup());
                        newWindow.yProperty().addListener((o, ov, nv) -> repositionPopup());
                    }
                });
            }
        });
    }

    // ==================== Keyboard Handlers ====================

    private void handleEnterKey() {
        if (listPopup.isShowing() && !listItems.isEmpty()) {
            int sel = listView.getSelectionModel().getSelectedIndex();
            if (sel < 0) sel = 0;
            selectSuggestion(sel);
        } else if (textField.getText() == null || textField.getText().isEmpty()) {
            showAllSuggestions();
        } else {
            // Try to filter and select first match
            handleTextChange(textField.getText());
            if (!filteredSuggestions.isEmpty()) {
                selectSuggestion(0);
            } else {
                moveFocusToNextField();
            }
        }
    }

    private void handleDownKey() {
        if (!listPopup.isShowing()) {
            if (textField.getText() == null || textField.getText().isEmpty()) {
                showAllSuggestions();
            } else {
                handleTextChange(textField.getText());
            }
        } else if (!listItems.isEmpty()) {
            int current = listView.getSelectionModel().getSelectedIndex();
            int next = Math.min(listItems.size() - 1, current + 1);
            listView.getSelectionModel().select(next);
            listView.scrollTo(next);
        }
    }

    private void handleUpKey() {
        if (listPopup.isShowing() && !listItems.isEmpty()) {
            int current = listView.getSelectionModel().getSelectedIndex();
            int prev = Math.max(0, current - 1);
            listView.getSelectionModel().select(prev);
            listView.scrollTo(prev);
        }
    }

    // ==================== Text Filtering ====================

    private void handleTextChange(String text) {
        if (text == null || text.trim().isEmpty()) {
            hidePopup();
            filteredSuggestions.clear();
            return;
        }

        String query = text.toLowerCase().trim();

        // Filter based on configuration
        if (useContainsFilter) {
            filteredSuggestions = suggestions.stream()
                .filter(s -> s.toLowerCase().contains(query))
                .sorted((a, b) -> {
                    // Prioritize startsWith matches
                    boolean aStarts = a.toLowerCase().startsWith(query);
                    boolean bStarts = b.toLowerCase().startsWith(query);
                    if (aStarts && !bStarts) return -1;
                    if (!aStarts && bStarts) return 1;
                    return String.CASE_INSENSITIVE_ORDER.compare(a, b);
                })
                .collect(Collectors.toList());
        } else {
            filteredSuggestions = suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(query))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        }

        if (filteredSuggestions.isEmpty()) {
            hidePopup();
            return;
        }

        updateListAndShow();
    }

    private void showAllSuggestions() {
        filteredSuggestions = new ArrayList<>(suggestions);
        filteredSuggestions.sort(String.CASE_INSENSITIVE_ORDER);

        if (filteredSuggestions.isEmpty()) {
            hidePopup();
            return;
        }

        updateListAndShow();
    }

    private void updateListAndShow() {
        listItems.setAll(filteredSuggestions);
        listView.getSelectionModel().select(0);
        listView.scrollTo(0);
        hoveredIndex = -1;
        showPopup();
    }

    // ==================== Selection ====================

    private void selectSuggestion(int index) {
        if (filteredSuggestions == null || filteredSuggestions.isEmpty()) return;
        if (index < 0 || index >= filteredSuggestions.size()) return;

        isSelectingSuggestion = true;

        try {
            String selected = filteredSuggestions.get(index);
            textField.setText(selected);
            textField.positionCaret(selected.length());
            selectedValue.set(selected);

            hidePopup();
            filteredSuggestions.clear();
            listItems.clear();

            // Notify callback
            if (onSelectionCallback != null) {
                onSelectionCallback.accept(selected);
            }

        } finally {
            Platform.runLater(() -> {
                isSelectingSuggestion = false;
                moveFocusToNextField();
            });
        }
    }

    private void moveFocusToNextField() {
        if (nextFocusField != null) {
            Platform.runLater(nextFocusField::requestFocus);
        }
    }

    // ==================== Popup Management ====================

    private void showPopup() {
        if (listItems.isEmpty()) return;

        // Calculate popup dimensions
        double popupWidth = Math.max(textField.getWidth(), MIN_POPUP_WIDTH);
        int visibleRows = Math.min(listItems.size(), MAX_VISIBLE_ROWS);
        double popupHeight = (visibleRows * ROW_HEIGHT) + (POPUP_PADDING * 2) + 2;

        listView.setPrefWidth(popupWidth - 2);
        listView.setPrefHeight(popupHeight - (POPUP_PADDING * 2));
        listView.setMaxHeight(popupHeight - (POPUP_PADDING * 2));

        if (!listPopup.isShowing()) {
            Point2D screenPos = textField.localToScreen(0, textField.getHeight());
            if (screenPos != null) {
                listPopup.show(textField, screenPos.getX(), screenPos.getY() + 2);

                // Fade in animation
                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), popupContainer);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }
        }
    }

    private void repositionPopup() {
        if (listPopup.isShowing()) {
            Point2D screenPos = textField.localToScreen(0, textField.getHeight());
            if (screenPos != null) {
                listPopup.setX(screenPos.getX());
                listPopup.setY(screenPos.getY() + 2);
            }
        }
    }

    private int getIndexAtY(double y) {
        return (int) ((y - POPUP_PADDING) / ROW_HEIGHT);
    }

    // ==================== Suggestion Cell ====================

    private class SuggestionCell extends ListCell<String> {

        private final Label label;

        public SuggestionCell() {
            label = new Label();
            label.setMaxWidth(Double.MAX_VALUE);
            label.setPadding(new Insets(8, 12, 8, 12));

            setGraphic(label);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setPadding(Insets.EMPTY);
            setStyle("-fx-background-color: transparent;");
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                label.setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
            } else {
                label.setText(item);
                setGraphic(label);

                // Apply custom font
                if (customFont != null) {
                    label.setFont(customFont);
                } else {
                    label.setFont(Font.font("Segoe UI", fontSize));
                }

                // Determine cell state
                boolean isSelected = isSelected();
                boolean isHovered = getIndex() == hoveredIndex;

                // Apply styling based on state
                if (isSelected) {
                    setStyle("-fx-background-color: " + SELECTED_COLOR + "; -fx-background-radius: 4;");
                    label.setTextFill(Color.web(PRIMARY_COLOR));
                    label.setFont(Font.font(label.getFont().getFamily(), FontWeight.BOLD, label.getFont().getSize()));
                } else if (isHovered) {
                    setStyle("-fx-background-color: " + HOVER_COLOR + "; -fx-background-radius: 4;");
                    label.setTextFill(Color.web(TEXT_PRIMARY));
                } else {
                    setStyle("-fx-background-color: transparent;");
                    label.setTextFill(Color.web(TEXT_PRIMARY));
                }
            }
        }
    }

    // ==================== Public API ====================

    /**
     * Set new suggestions list
     */
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
    }

    /**
     * Add suggestions to existing list
     */
    public void addSuggestions(List<String> newSuggestions) {
        this.suggestions.addAll(newSuggestions);
    }

    /**
     * Set custom font for TextField and suggestions
     */
    public void setCustomFont(Font customFont) {
        this.customFont = customFont;
        this.fontSize = customFont != null ? customFont.getSize() : 14.0;
        textField.setFont(customFont);
        listView.refresh();
    }

    /**
     * Set whether to use "contains" filter instead of "startsWith"
     */
    public void setUseContainsFilter(boolean useContains) {
        this.useContainsFilter = useContains;
    }

    /**
     * Clear the text field
     */
    public void clear() {
        textField.clear();
        hidePopup();
        filteredSuggestions.clear();
    }

    /**
     * Set text in the text field
     */
    public void setText(String text) {
        isSelectingSuggestion = true;
        textField.setText(text);
        Platform.runLater(() -> isSelectingSuggestion = false);
    }

    /**
     * Get current text
     */
    public String getText() {
        return textField.getText();
    }

    /**
     * Get the selected value property for binding
     */
    public StringProperty selectedValueProperty() {
        return selectedValue;
    }

    /**
     * Get the currently highlighted suggestion
     */
    public String getCurrentSuggestion() {
        if (filteredSuggestions == null || filteredSuggestions.isEmpty()) return null;
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0) idx = 0;
        return idx < filteredSuggestions.size() ? filteredSuggestions.get(idx) : null;
    }

    /**
     * Programmatically select current suggestion
     */
    public void selectCurrentSuggestion() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 && !filteredSuggestions.isEmpty()) idx = 0;
        if (idx >= 0 && idx < filteredSuggestions.size()) {
            selectSuggestion(idx);
        }
    }

    /**
     * Get list of current filtered suggestions
     */
    public List<String> getFilteredSuggestions() {
        return new ArrayList<>(filteredSuggestions);
    }

    /**
     * Check if popup is currently showing
     */
    public boolean isPopupShowing() {
        return listPopup.isShowing();
    }

    /**
     * Hide the popup
     */
    public void hidePopup() {
        if (listPopup.isShowing()) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(100), popupContainer);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> listPopup.hide());
            fadeOut.play();
        }
        hoveredIndex = -1;
    }

    /**
     * Request focus on the text field
     */
    public void requestFocus() {
        textField.requestFocus();
    }

    /**
     * Check if text field is focused
     */
    public boolean isFocused() {
        return textField.isFocused();
    }

    /**
     * Set prompt text
     */
    public void setPromptText(String promptText) {
        textField.setPromptText(promptText);
    }

    /**
     * Get prompt text
     */
    public String getPromptText() {
        return textField.getPromptText();
    }
}

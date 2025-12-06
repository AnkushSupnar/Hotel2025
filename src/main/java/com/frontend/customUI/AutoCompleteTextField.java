package com.frontend.customUI;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elegant JavaFX TextField with autocomplete suggestion popup.
 * Material Design styled with proper keyboard navigation.
 */
public class AutoCompleteTextField {

    @Getter
    private final TextField textField;
    private final ContextMenu suggestionsPopup;
    private List<String> suggestions = new ArrayList<>();
    private List<String> filteredSuggestions = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean isSelectingSuggestion = false;
    private Font customFont;

    @Setter
    @Getter
    private Control nextFocusField;

    private ChangeListener<String> textListener;

    // Material Design Color Palette
    private static final String PRIMARY_COLOR = "#1976D2";
    private static final String TEXT_PRIMARY = "#212121";
    private static final String TEXT_SECONDARY = "#757575";
    private static final String SURFACE_COLOR = "#FFFFFF";
    private static final String HOVER_COLOR = "#E3F2FD";

    // Styles for suggestion items
    private static final String HIGHLIGHT_LABEL_STYLE = "-fx-background-color: " + PRIMARY_COLOR + "; " +
            "-fx-background-radius: 4px;";

    private static final String NORMAL_LABEL_STYLE = "-fx-background-color: transparent;";

    private static final String HOVER_LABEL_STYLE = "-fx-background-color: " + HOVER_COLOR + "; " +
            "-fx-background-radius: 4px;";

    // Style to disable default menu item styling
    private static final String MENU_ITEM_STYLE = "-fx-padding: 0; " +
            "-fx-background-color: transparent; " +
            "-fx-border-color: transparent; " +
            "-fx-focus-color: transparent; " +
            "-fx-faint-focus-color: transparent;";

    // Row height and max visible rows for scrolling behavior
    private static final double ROW_HEIGHT = 40.0;
    private static final int MAX_VISIBLE_ROWS = 10;

    public AutoCompleteTextField(TextField textField, List<String> suggestions) {
        this.textField = textField;
        this.suggestionsPopup = new ContextMenu();
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>(suggestions);
        this.customFont = null;
        styleTextField();
        attachListeners();
        stylePopup();
    }

    public AutoCompleteTextField(TextField textField, List<String> suggestions, Font customFont) {
        this.textField = textField;
        this.suggestionsPopup = new ContextMenu();
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>(suggestions);
        this.customFont = customFont;
        styleTextField();
        attachListeners();
        stylePopup();
    }

    public AutoCompleteTextField(TextField textField) {
        this(textField, new ArrayList<>());
    }

    public AutoCompleteTextField(TextField textField, List<String> suggestions, Control nextFocusField) {
        this(textField, suggestions);
        this.nextFocusField = nextFocusField;
    }

    public AutoCompleteTextField(TextField textField, List<String> suggestions, Font customFont,
            Control nextFocusField) {
        this(textField, suggestions, customFont);
        this.nextFocusField = nextFocusField;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
    }

    public void setCustomFont(Font customFont) {
        this.customFont = customFont;
    }

    private void attachListeners() {
        textListener = (obs, oldText, newText) -> {
            if (isSelectingSuggestion) {
                return;
            }

            if (newText == null || newText.isEmpty()) {
                suggestionsPopup.hide();
                filteredSuggestions.clear();
                selectedIndex = 0;
            } else {
                String searchText = newText.toLowerCase().trim();

                List<String> matchedItems = suggestions.stream()
                        .filter(item -> item.toLowerCase().startsWith(searchText))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList());

                filteredSuggestions = matchedItems;

                if (filteredSuggestions.isEmpty()) {
                    suggestionsPopup.hide();
                    selectedIndex = 0;
                } else {
                    selectedIndex = 0;
                    populatePopup();
                    showPopupIfNeeded();
                }
            }
        };

        textField.textProperty().addListener(textListener);

        textField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case SPACE -> {
                    if (textField.getText() == null || textField.getText().isEmpty()) {
                        showAllSuggestions();
                        event.consume();
                    }
                }
                case ESCAPE -> {
                    suggestionsPopup.hide();
                    selectedIndex = 0;
                    event.consume();
                }
                case ENTER -> {
                    handleEnterKey(event);
                }
                case DOWN -> {
                    handleNavigation(true); // true = down
                    event.consume();
                }
                case UP -> {
                    handleNavigation(false); // false = up
                    event.consume();
                }
                case TAB -> {
                    if (!filteredSuggestions.isEmpty()) {
                        validateSelectedIndex();
                        selectSuggestion(selectedIndex);
                    }
                    event.consume();
                }
                default -> {
                    // Do nothing
                }
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                suggestionsPopup.hide();
                selectedIndex = 0;
            }
        });
    }

    /**
     * Centralized keyboard navigation handler - handles DOWN/UP arrow logic
     */
    private void handleNavigation(boolean moveDown) {
        if (filteredSuggestions.isEmpty()) {
            return;
        }

        // Ensure popup is showing
        if (!suggestionsPopup.isShowing()) {
            populatePopup();
            showPopupIfNeeded();
            selectedIndex = 0;
        } else {
            // Update selection index
            if (moveDown) {
                if (selectedIndex < filteredSuggestions.size() - 1) {
                    selectedIndex++;
                }
            } else {
                if (selectedIndex > 0) {
                    selectedIndex--;
                }
            }
        }

        // Always update UI safely
        Platform.runLater(() -> updatePopupHighlightSafely());
    }

    /**
     * Validate and fix selectedIndex bounds
     */
    private void validateSelectedIndex() {
        if (filteredSuggestions.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        if (selectedIndex < 0 || selectedIndex >= filteredSuggestions.size()) {
            selectedIndex = 0;
        }
    }

    private void showAllSuggestions() {
        filteredSuggestions = new ArrayList<>(suggestions);
        filteredSuggestions.sort(String.CASE_INSENSITIVE_ORDER);
        selectedIndex = 0;
        populatePopup();
        showPopupIfNeeded();
    }

    private void handleEnterKey(javafx.scene.input.KeyEvent event) {
        String currentText = textField.getText();

        if (currentText == null || currentText.isEmpty()) {
            if (suggestionsPopup.isShowing() && !filteredSuggestions.isEmpty()) {
                selectSuggestion(0);
            } else {
                showAllSuggestions();
            }
            event.consume();
            return;
        }

        validateSelectedIndex();

        if (suggestionsPopup.isShowing() && !filteredSuggestions.isEmpty()) {
            String firstSuggestion = filteredSuggestions.get(0);

            if (currentText.equalsIgnoreCase(firstSuggestion)) {
                suggestionsPopup.hide();
                moveFocusToNextField();
                event.consume();
                return;
            }

            // Use validated selectedIndex
            selectSuggestion(selectedIndex);
            event.consume();
            return;
        }

        if (!filteredSuggestions.isEmpty()) {
            String firstSuggestion = filteredSuggestions.get(0);
            if (currentText.equalsIgnoreCase(firstSuggestion)) {
                moveFocusToNextField();
                event.consume();
                return;
            }
        }

        boolean exactMatch = suggestions.stream()
                .anyMatch(s -> s.equalsIgnoreCase(currentText));

        if (exactMatch) {
            moveFocusToNextField();
            event.consume();
        }
    }

    private void moveFocusToNextField() {
        if (nextFocusField != null) {
            Platform.runLater(() -> {
                nextFocusField.requestFocus();
            });
        }
    }

    private void showPopupIfNeeded() {
        if (!suggestionsPopup.isShowing() && !filteredSuggestions.isEmpty()) {
            suggestionsPopup.show(textField,
                    textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                    textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
        }
    }

    private void selectSuggestion(int index) {
        validateSelectedIndex();
        index = selectedIndex; // Use validated index

        if (index < 0 || index >= filteredSuggestions.size()) {
            return;
        }

        isSelectingSuggestion = true;

        try {
            String suggestion = filteredSuggestions.get(index);
            textField.setText(suggestion);
            textField.positionCaret(suggestion.length());
            suggestionsPopup.hide();
            filteredSuggestions.clear();
            selectedIndex = 0;
        } catch (Exception e) {
            System.err.println("Error selecting suggestion: " + e.getMessage());
        } finally {
            Platform.runLater(() -> {
                isSelectingSuggestion = false;
            });
        }
    }

    private void populatePopup() {
        List<CustomMenuItem> menuItems = new ArrayList<>();

        for (int i = 0; i < filteredSuggestions.size(); i++) {
            String suggestion = filteredSuggestions.get(i);
            Label entryLabel = createSuggestionLabel(suggestion, i == selectedIndex);
            int index = i;

            CustomMenuItem item = new CustomMenuItem(entryLabel, true);
            item.setHideOnClick(false);
            item.setStyle(MENU_ITEM_STYLE);

            // Mouse click - update selectedIndex first
            entryLabel.setOnMouseClicked(evt -> {
                selectedIndex = index;
                updatePopupHighlightSafely();
                selectSuggestion(index);
                evt.consume();
            });

            final int currentIndex = i;
            entryLabel.setOnMouseEntered(evt -> {
                if (currentIndex != selectedIndex) {
                    entryLabel.setStyle(HOVER_LABEL_STYLE);
                    entryLabel.setTextFill(Color.web(TEXT_PRIMARY));
                }
            });

            entryLabel.setOnMouseExited(evt -> {
                if (currentIndex != selectedIndex) {
                    entryLabel.setStyle(NORMAL_LABEL_STYLE);
                    entryLabel.setTextFill(Color.web(TEXT_PRIMARY));
                } else {
                    entryLabel.setStyle(HIGHLIGHT_LABEL_STYLE);
                    entryLabel.setTextFill(Color.WHITE);
                }
            });

            menuItems.add(item);
        }

        suggestionsPopup.getItems().clear();
        suggestionsPopup.getItems().addAll(menuItems);

        suggestionsPopup.setPrefWidth(Region.USE_COMPUTED_SIZE);

        // Dynamic height + scroll behavior (max 10 items visible)
        int itemCount = filteredSuggestions.size();
        double visibleRows = Math.min(itemCount, MAX_VISIBLE_ROWS);
        double popupHeight = visibleRows * ROW_HEIGHT + 16; // + padding top/bottom

        suggestionsPopup.setPrefHeight(popupHeight);
        suggestionsPopup.setMaxHeight(popupHeight); // Enables built-in scrolling

        suggestionsPopup.setAutoFix(true);
        applyPopupSkinStyle();
    }

    /**
     * Apply custom styling to popup skin to override default ContextMenu styles
     */
    private void applyPopupSkinStyle() {
        suggestionsPopup.setOnShown(event -> {
            if (suggestionsPopup.getSkin() != null) {
                Region popupRegion = (Region) suggestionsPopup.getSkin().getNode();
                popupRegion.setStyle(
                        "-fx-background-color: " + SURFACE_COLOR + "; " +
                                "-fx-background-radius: 8px; " +
                                "-fx-border-color: #E0E0E0; " +
                                "-fx-border-radius: 8px; " +
                                "-fx-border-width: 1px; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3); " +
                                "-fx-padding: 8px 4px;");
            }
        });
    }

    /**
     * Safe highlight update with retry mechanism
     */
    private void updatePopupHighlightSafely() {
        // First try immediately
        if (tryUpdateHighlight()) {
            return;
        }

        // Retry after short delay
        Platform.runLater(() -> {
            if (tryUpdateHighlight()) {
                return;
            }
            // Final fallback - ensure popup is ready
            Platform.runLater(() -> populatePopup());
        });
    }

    private boolean tryUpdateHighlight() {
        if (!suggestionsPopup.isShowing() || suggestionsPopup.getItems().isEmpty()) {
            return false;
        }

        validateSelectedIndex();

        // Check if popup items match filteredSuggestions
        if (suggestionsPopup.getItems().size() != filteredSuggestions.size()) {
            return false;
        }

        updatePopupHighlight();
        return true;
    }

    private void updatePopupHighlight() {
        for (int i = 0; i < suggestionsPopup.getItems().size(); i++) {
            try {
                CustomMenuItem item = (CustomMenuItem) suggestionsPopup.getItems().get(i);
                Label label = (Label) item.getContent();
                boolean isHighlighted = (i == selectedIndex);

                applyFontToLabel(label, isHighlighted);

                if (isHighlighted) {
                    label.setStyle(HIGHLIGHT_LABEL_STYLE);
                    label.setTextFill(Color.WHITE);
                } else {
                    label.setStyle(NORMAL_LABEL_STYLE);
                    label.setTextFill(Color.web(TEXT_PRIMARY));
                }
            } catch (Exception e) {
                // Ignore individual item errors
                System.err.println("Error updating highlight for item " + i + ": " + e.getMessage());
            }
        }
    }

    private void applyFontToLabel(Label label, boolean highlighted) {
        if (customFont != null) {
            if (highlighted) {
                label.setFont(Font.font(
                        customFont.getFamily(),
                        FontWeight.MEDIUM,
                        customFont.getSize()));
            } else {
                label.setFont(customFont);
            }
        } else {
            if (highlighted) {
                label.setFont(Font.font("Segoe UI", FontWeight.MEDIUM, 14));
            } else {
                label.setFont(Font.font("Segoe UI", 14));
            }
        }
    }

    private Label createSuggestionLabel(String text, boolean highlighted) {
        Label label = new Label(text);
        label.setPrefWidth(300);
        label.setMaxWidth(400);
        label.setMinHeight(ROW_HEIGHT);
        label.setMaxHeight(ROW_HEIGHT);
        label.setPadding(new Insets(10, 16, 10, 16));

        applyFontToLabel(label, highlighted);

        label.setStyle(highlighted ? HIGHLIGHT_LABEL_STYLE : NORMAL_LABEL_STYLE);
        label.setWrapText(false);
        label.setTextFill(highlighted ? Color.WHITE : Color.web(TEXT_PRIMARY));
        label.setMouseTransparent(false);

        return label;
    }

    private void styleTextField() {
        if (textField.getStyle() == null || textField.getStyle().isEmpty()) {
            textField.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-border-color: transparent; " +
                            "-fx-background-radius: 0; " +
                            "-fx-padding: 14 0; " +
                            "-fx-text-fill: " + TEXT_PRIMARY + "; " +
                            "-fx-prompt-text-fill: " + TEXT_SECONDARY + ";");
        }
    }

    private void stylePopup() {
        suggestionsPopup.setStyle(
                "-fx-background-color: " + SURFACE_COLOR + "; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3); " +
                        "-fx-padding: 8px 4px;");

        applyPopupSkinStyle();
    }

    public String getCurrentSuggestion() {
        validateSelectedIndex();
        if (!filteredSuggestions.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
            return filteredSuggestions.get(selectedIndex);
        }
        return null;
    }

    public void selectCurrentSuggestion() {
        validateSelectedIndex();
        if (!filteredSuggestions.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
            selectSuggestion(selectedIndex);
        }
    }

    public List<String> getFilteredSuggestions() {
        return new ArrayList<>(filteredSuggestions);
    }

    public boolean isPopupShowing() {
        return suggestionsPopup.isShowing();
    }

    public void hidePopup() {
        suggestionsPopup.hide();
        selectedIndex = 0;
    }
}

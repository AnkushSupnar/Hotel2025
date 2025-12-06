package com.frontend.customUI;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Elegant JavaFX TextField with autocomplete suggestion popup.
 * Suggestions are passed via the constructor or setter.
 */
public class AutoCompleteTextField_old {

    @Getter
    private final TextField textField;
    private final ContextMenu suggestionsPopup;
    private List<String> suggestions = new ArrayList<>();
    private List<String> filteredSuggestions = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean isSelectingSuggestion = false;
    private Font customFont; // Custom font for suggestions

    // Reference to text listener for removal/re-adding
    private ChangeListener<String> textListener;

    public AutoCompleteTextField_old(TextField textField, List<String> suggestions) {
        this.textField = textField;
        this.suggestionsPopup = new ContextMenu();
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>(suggestions);
        this.customFont = null; // No custom font
        styleTextField();
        attachListeners();
        stylePopup();
    }

    public AutoCompleteTextField_old(TextField textField, List<String> suggestions, Font customFont) {
        this.textField = textField;
        this.suggestionsPopup = new ContextMenu();
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>(suggestions);
        this.customFont = customFont; // Use custom font for suggestions
        styleTextField();
        attachListeners();
        stylePopup();
    }

    public AutoCompleteTextField_old(TextField textField) {
        this(textField, new ArrayList<>());
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
    }

    private void attachListeners() {
        textListener = (obs, oldText, newText) -> {
            if (isSelectingSuggestion) {
                System.out.println("⏭ Skipping text processing (selection in progress)");
                return; // Skip processing during suggestion selection
            }

            System.out.println("📝 Text changed: '" + oldText + "' → '" + newText + "'");

            if (newText == null || newText.isEmpty()) {
                System.out.println("🔄 Text cleared, hiding popup");
                suggestionsPopup.hide();
                filteredSuggestions.clear(); // Clear filtered suggestions
                selectedIndex = 0; // Reset selected index
            } else if (newText.trim().isEmpty() && newText.startsWith(" ")) {
                // Show all suggestions when space is entered as first character
                System.out.println("📋 Showing all suggestions (space pressed)");
                filteredSuggestions = new ArrayList<>(suggestions);
                // Sort alphabetically
                filteredSuggestions.sort(String.CASE_INSENSITIVE_ORDER);
                selectedIndex = 0; // Select first item
                populatePopup();
                if (!suggestionsPopup.isShowing()) {
                    suggestionsPopup.show(textField,
                            textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                            textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
                }
            } else {
                // Filter and sort suggestions based on search text
                String searchText = newText.toLowerCase().trim();
                System.out.println("🔍 Filtering suggestions for: '" + searchText + "'");

                // Filter items that START with the search text (matching Swing behavior)
                // This is more restrictive and matches the original implementation
                List<String> matchedItems = suggestions.stream()
                        .filter(item -> item.toLowerCase().startsWith(searchText))
                        .collect(Collectors.toList());

                // Sort alphabetically
                matchedItems.sort(String.CASE_INSENSITIVE_ORDER);

                filteredSuggestions = matchedItems;

                if (filteredSuggestions.isEmpty()) {
                    System.out.println("❌ No matches found, hiding popup");
                    suggestionsPopup.hide();
                    selectedIndex = 0; // Reset selected index
                } else {
                    System.out.println("✅ Found " + filteredSuggestions.size() + " matches, showing popup");
                    selectedIndex = 0; // Always start with first suggestion
                    populatePopup();
                    if (!suggestionsPopup.isShowing()) {
                        suggestionsPopup.show(textField,
                                textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                                textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
                    }
                }
            }
        };

        textField.textProperty().addListener(textListener);

        textField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> {
                    suggestionsPopup.hide();
                    selectedIndex = 0;
                    event.consume();
                }
                case ENTER -> {
                    if (textField.getText().equals("")) {
                        filteredSuggestions = new ArrayList<>(suggestions);
                        if (suggestionsPopup.isShowing()) {
                            selectedIndex = 0;
                            textField.setText(filteredSuggestions.get(selectedIndex));
                            suggestionsPopup.hide();
                        }

                        System.out.println("Enter pressed with no text, showing popup");

                        // Sort alphabetically
                        filteredSuggestions.sort(String.CASE_INSENSITIVE_ORDER);
                        selectedIndex = 0; // Select first item
                        populatePopup();
                        if (!suggestionsPopup.isShowing()) {
                            suggestionsPopup.show(textField,
                                    textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                                    textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
                        }
                        return;
                    } else {

                        System.out.println("⏎ ENTER key pressed");
                        System.out.println("   - Filtered suggestions count: " + filteredSuggestions.size());
                        System.out.println("   - Selected index: " + selectedIndex);
                        System.out.println("   - Popup showing: " + suggestionsPopup.isShowing());

                        // Handle Enter key to select the first/selected suggestion
                        if (!filteredSuggestions.isEmpty()) {
                            // Always select the item at selectedIndex (default is 0, which is the first
                            // item)
                            if (selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
                                System.out.println("   ➜ Selecting suggestion at index " + selectedIndex);
                                selectSuggestion(selectedIndex);
                            } else {
                                // Fallback: if selectedIndex is out of range, select the first item
                                System.out.println("   ➜ Index out of range, selecting first item");
                                selectedIndex = 0;
                                selectSuggestion(0);
                            }
                            event.consume(); // Consume the event to prevent other handlers
                        } else {
                            System.out.println("   ✖ No suggestions available, letting Enter propagate");
                        }
                        // If no suggestions, let the Enter key propagate for other handlers
                    }

                }
                case DOWN -> {
                    if (!filteredSuggestions.isEmpty()) {
                        // Move down in the list
                        if (selectedIndex < filteredSuggestions.size() - 1) {
                            selectedIndex++;
                            // Show popup if not already showing
                            if (!suggestionsPopup.isShowing() && !textField.getText().trim().isEmpty()) {
                                populatePopup();
                                suggestionsPopup.show(textField,
                                        textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                                        textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
                            } else {
                                updatePopupHighlight();
                            }
                        }
                        event.consume();
                    }
                }
                case UP -> {
                    if (!filteredSuggestions.isEmpty()) {
                        // Move up in the list
                        if (selectedIndex > 0) {
                            selectedIndex--;
                            // Show popup if not already showing
                            if (!suggestionsPopup.isShowing() && !textField.getText().trim().isEmpty()) {
                                populatePopup();
                                suggestionsPopup.show(textField,
                                        textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                                        textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
                            } else {
                                updatePopupHighlight();
                            }
                        }
                        event.consume();
                    }
                }
                case TAB -> {
                    // Handle Tab key similar to Enter - auto-complete with selected suggestion
                    if (!filteredSuggestions.isEmpty() && suggestionsPopup.isShowing()) {
                        if (selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
                            selectSuggestion(selectedIndex);
                        } else {
                            selectedIndex = 0;
                            selectSuggestion(0);
                        }
                        event.consume(); // Consume the event to prevent tab navigation
                    }
                }
                default -> {
                    // Do nothing for other keys
                }
            }
        });

        // Add focus listener to hide popup when focus is lost
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                suggestionsPopup.hide();
                selectedIndex = 0;
            }
        });
    }

    private void selectSuggestion(int index) {
        if (index < 0 || index >= filteredSuggestions.size()) {
            System.err.println("Invalid suggestion index: " + index + " (size: " + filteredSuggestions.size() + ")");
            return; // Safety check
        }

        isSelectingSuggestion = true; // Set flag to prevent listener interference

        try {
            String suggestion = filteredSuggestions.get(index);

            System.out.println("✓ Selecting suggestion: " + suggestion);

            // Set the text in the text field
            textField.setText(suggestion);

            // Position cursor at the end
            textField.positionCaret(suggestion.length());

            // Hide the popup
            suggestionsPopup.hide();

            // Clear filtered suggestions to reset state for next use
            filteredSuggestions.clear();

            // Reset selected index
            selectedIndex = 0;

            System.out.println("✓ Selection complete. State reset for next use.");

        } catch (Exception e) {
            System.err.println("Error selecting suggestion: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Reset flag after a short delay to ensure setText() event is fully processed
            javafx.application.Platform.runLater(() -> {
                isSelectingSuggestion = false;
                System.out.println("✓ Flag reset. Ready for next input.");
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
            item.setOnAction(evt -> selectSuggestion(index));
            menuItems.add(item);
        }
        suggestionsPopup.getItems().clear();
        suggestionsPopup.getItems().addAll(menuItems);

        suggestionsPopup.setPrefWidth(Region.USE_COMPUTED_SIZE);

        // Always limit to 8 items height, enable scrolling for more items
        // Each item is 35px, so 8 items = 280px + 10px padding = 290px
        suggestionsPopup.setMaxHeight(290);
        suggestionsPopup.setPrefHeight(Region.USE_COMPUTED_SIZE);

        suggestionsPopup.setAutoFix(true);
    }

    private void updatePopupHighlight() {
        for (int i = 0; i < suggestionsPopup.getItems().size(); i++) {
            CustomMenuItem item = (CustomMenuItem) suggestionsPopup.getItems().get(i);
            Label label = (Label) item.getContent();
            if (i == selectedIndex) {
                label.setStyle(HIGHLIGHT_LABEL_STYLE);
                label.setTextFill(Color.WHITE);
            } else {
                label.setStyle(NORMAL_LABEL_STYLE);
                label.setTextFill(Color.web("#212121"));
            }
        }
    }

    private Label createSuggestionLabel(String text, boolean highlighted) {
        Label label = new Label(text);
        label.setPrefWidth(300);
        label.setMaxWidth(400);
        label.setMinHeight(35);
        label.setMaxHeight(35);
        // More compact padding for professional look
        label.setPadding(new Insets(8, 12, 8, 12));

        // Use custom font if provided, otherwise use default Segoe UI
        if (customFont != null) {
            label.setFont(customFont);
        } else {
            label.setFont(Font.font("Segoe UI", 14));
        }

        label.setStyle(highlighted ? HIGHLIGHT_LABEL_STYLE : NORMAL_LABEL_STYLE);
        label.setWrapText(false);
        label.setTextFill(highlighted ? Color.WHITE : Color.web("#212121"));
        return label;
    }

    private void styleTextField() {
        // Match Material Design input styling from login screen
        // Font is already set in FXML or controller, don't override it
        // textField.setFont(Font.font("Segoe UI", 15)); // Removed - preserves custom
        // fonts

        // Only apply non-font styles if no style is already set
        if (textField.getStyle() == null || textField.getStyle().isEmpty()) {
            textField.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-border-color: transparent; " +
                            "-fx-background-radius: 0; " +
                            "-fx-padding: 14 0; " +
                            "-fx-text-fill: #212121; " +
                            "-fx-prompt-text-fill: #9e9e9e;");
        }
    }

    private void stylePopup() {
        suggestionsPopup.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4); " +
                        "-fx-padding: 4px; " +
                        "-fx-max-height: 290px; " + // 8 items * 35px + 10px padding
                        "-fx-pref-height: 290px;");
    }

    private static final String HIGHLIGHT_LABEL_STYLE = "-fx-background-color: #5529b4ff; -fx-font-weight: 600; -fx-background-radius: 6px;";
    private static final String NORMAL_LABEL_STYLE = "-fx-background-color: transparent;";

    /**
     * Get the currently selected suggestion without selecting it
     * 
     * @return the suggestion text at the selected index, or null if no suggestions
     */
    public String getCurrentSuggestion() {
        if (!filteredSuggestions.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
            return filteredSuggestions.get(selectedIndex);
        }
        return null;
    }

    /**
     * Programmatically select the current suggestion
     * Useful for triggering selection from external code
     */
    public void selectCurrentSuggestion() {
        if (!filteredSuggestions.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
            selectSuggestion(selectedIndex);
        }
    }

    /**
     * Get the list of current filtered suggestions
     * 
     * @return list of filtered suggestions
     */
    public List<String> getFilteredSuggestions() {
        return new ArrayList<>(filteredSuggestions);
    }

    /**
     * Check if suggestions popup is currently showing
     * 
     * @return true if popup is showing
     */
    public boolean isPopupShowing() {
        return suggestionsPopup.isShowing();
    }

    /**
     * Hide the suggestions popup
     */
    public void hidePopup() {
        suggestionsPopup.hide();
        selectedIndex = 0;
    }
}

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
public class AutoCompleteTextField {

    @Getter
    private final TextField textField;
    private final ContextMenu suggestionsPopup;
    private List<String> suggestions = new ArrayList<>();
    private List<String> filteredSuggestions = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean isSelectingSuggestion = false;

    // Reference to text listener for removal/re-adding
    private ChangeListener<String> textListener;

    public AutoCompleteTextField(TextField textField, List<String> suggestions) {
        this.textField = textField;
        this.suggestionsPopup = new ContextMenu();
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>(suggestions);
        styleTextField();
        attachListeners();
        stylePopup();
    }

    public AutoCompleteTextField(TextField textField) {
        this(textField, new ArrayList<>());
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
    }

    private void attachListeners() {
        textListener = (obs, oldText, newText) -> {
            if (isSelectingSuggestion) {
                return; // Skip processing during suggestion selection
            }

            if (newText == null || newText.isEmpty()) {
                suggestionsPopup.hide();
                selectedIndex = 0; // Reset selected index
            } else if (newText.trim().isEmpty() && newText.startsWith(" ")) {
                // Show all suggestions when space is entered as first character
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

                // First, filter items that contain the search text
                List<String> matchedItems = suggestions.stream()
                        .filter(item -> item.toLowerCase().contains(searchText))
                        .collect(Collectors.toList());

                // Sort: items starting with search text come first, then others alphabetically
                matchedItems.sort((item1, item2) -> {
                    String lower1 = item1.toLowerCase();
                    String lower2 = item2.toLowerCase();

                    boolean starts1 = lower1.startsWith(searchText);
                    boolean starts2 = lower2.startsWith(searchText);

                    if (starts1 && !starts2) {
                        return -1; // item1 starts with search, comes first
                    } else if (!starts1 && starts2) {
                        return 1; // item2 starts with search, comes first
                    } else {
                        return lower1.compareTo(lower2); // Both same priority, sort alphabetically
                    }
                });

                filteredSuggestions = matchedItems;

                if (filteredSuggestions.isEmpty()) {
                    suggestionsPopup.hide();
                    selectedIndex = 0; // Reset selected index
                } else {
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
                }
                case ENTER -> {
                    System.out.println("Enter button pressed");
                    if (!filteredSuggestions.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredSuggestions.size()) {
                        selectSuggestion(selectedIndex);
                        event.consume(); // Consume the event to prevent other handlers
                    }
                }
                case DOWN -> {
                    if (suggestionsPopup.isShowing() && selectedIndex < filteredSuggestions.size() - 1) {
                        selectedIndex++;
                        updatePopupHighlight();
                    }
                    event.consume();
                }
                case UP -> {
                    if (suggestionsPopup.isShowing() && selectedIndex > 0) {
                        selectedIndex--;
                        updatePopupHighlight();
                    }
                    event.consume();
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
            return; // Safety check
        }

        isSelectingSuggestion = true; // Set flag to prevent listener interference

        String suggestion = filteredSuggestions.get(index);
        textField.setText(suggestion);
        textField.positionCaret(suggestion.length());

        suggestionsPopup.hide();
        selectedIndex = 0;

        isSelectingSuggestion = false; // Reset flag

        System.out.println("Selected suggestion: " + suggestion);
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
        label.setPadding(new Insets(10, 16, 10, 16));
        label.setFont(Font.font("Segoe UI", 14));
        label.setStyle(highlighted ? HIGHLIGHT_LABEL_STYLE : NORMAL_LABEL_STYLE);
        label.setWrapText(false);
        label.setTextFill(highlighted ? Color.WHITE : Color.web("#212121"));
        return label;
    }

    private void styleTextField() {
        // Match Material Design input styling from login screen
        textField.setFont(Font.font("Segoe UI", 15));
        textField.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: transparent; " +
                        "-fx-background-radius: 0; " +
                        "-fx-padding: 14 0; " +
                        "-fx-text-fill: #212121; " +
                        "-fx-prompt-text-fill: #9e9e9e;"
        );
    }

    private void stylePopup() {
        suggestionsPopup.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 4); " +
                        "-fx-padding: 4px;"
        );
    }

    private static final String HIGHLIGHT_LABEL_STYLE = "-fx-background-color: #5529b4ff; -fx-font-weight: 600; -fx-background-radius: 6px;";
    private static final String NORMAL_LABEL_STYLE = "-fx-background-color: transparent;";
}

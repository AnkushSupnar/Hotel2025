package com.frontend.customUI;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AutoCompleteTextField
 *
 * - Preserves original constructors
 * - Uses Popup + ListView for suggestions (reliable keyboard behavior)
 * - startsWith() filtering (case-insensitive)
 * - ENTER on empty text -> shows up to 10 items (scrollable)
 * - ENTER selects highlighted suggestion (Option A)
 * - Arrow-key navigation works
 * - Mouse click selection works
 * - Debounce typing: 150 ms
 * - Uses customFont (if provided) for TextField and list items
 */
public class AutoCompleteTextField {

    @Getter
    private final TextField textField;

    // kept for compatibility with your constructors (not used for selection)
    private final ContextMenu suggestionsPopup;

    // Real UI: Popup + ListView
    private final Popup listPopup = new Popup();
    private final ListView<String> listView = new ListView<>();
    private final ObservableList<String> listItems = FXCollections.observableArrayList();

    private List<String> suggestions = new ArrayList<>();
    private List<String> filteredSuggestions = new ArrayList<>();
    private int selectedIndex = 0;
    private boolean isSelectingSuggestion = false;
    private Font customFont;

    @Setter
    @Getter
    private Control nextFocusField;

    private ChangeListener<String> textListener;
    private final PauseTransition debounce = new PauseTransition(Duration.millis(150)); // 150ms debounce

    // Colors & styles
    private static final String PRIMARY_COLOR = "#1976D2";
    private static final String TEXT_PRIMARY = "#212121";
    private static final String TEXT_SECONDARY = "#757575";
    private static final String SURFACE_COLOR = "#FFFFFF";
    private static final String HOVER_COLOR = "#E3F2FD";

    private static final double ROW_HEIGHT = 40.0;
    private static final int MAX_VISIBLE_ROWS = 10;

    /* ---------------- Constructors (unchanged) ---------------- */

    public AutoCompleteTextField(TextField textField, List<String> suggestions) {
        this.textField = textField;
        this.suggestionsPopup = new ContextMenu(); // preserved for compatibility
        this.suggestionsPopup.setAutoHide(true);
        this.suggestions = new ArrayList<>(suggestions);
        this.customFont = null;
        initListPopup();
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
        initListPopup();
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

    /* ---------------- Public setters ---------------- */

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
    }

    public void setCustomFont(Font customFont) {
        this.customFont = customFont;
        styleTextField();
        // reapply cell factory so cells pick up the new font immediately
        listView.setCellFactory(lv -> new SuggestionCell());
    }

    /* ---------------- Initialization & styling ---------------- */

    private void initListPopup1() {
        listView.setItems(listItems);
        listView.setFocusTraversable(false);
        listView.setCellFactory(lv -> new SuggestionCell());
        listView.setPrefWidth(350);

        StackPane container = new StackPane(listView);
        container.setStyle("-fx-background-color: " + SURFACE_COLOR + "; -fx-padding: 4; -fx-background-radius: 8;");
        listPopup.getContent().add(container);
        listPopup.setAutoHide(true);
    }

    private void initListPopup() {
        listView.setItems(listItems);
        listView.setFocusTraversable(false);
        listView.setCellFactory(lv -> new SuggestionCell());

        StackPane container = new StackPane(listView);

        // Material-style styling
        container.setStyle(
                "-fx-background-color: " + SURFACE_COLOR + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #BDBDBD;" + // subtle gray border
                        "-fx-border-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);" +
                        "-fx-padding: 2;");

        listPopup.getContent().add(container);
        listPopup.setAutoHide(true);

        // Add hover effect for cells
        listView.setCellFactory(lv -> {
            ListCell<String> cell = new SuggestionCell();
            cell.hoverProperty().addListener((obs, wasHovered, isNowHovered) -> {
                if (isNowHovered) {
                    cell.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 4;");
                } else {
                    if (cell.isSelected()) {
                        cell.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-background-radius: 4;");
                    } else {
                        cell.setStyle("-fx-background-color: transparent;");
                    }
                }
            });
            return cell;
        });
    }

    private void stylePopup() {
        listView.setStyle("-fx-background-color: transparent; -fx-padding: 4;");
    }

    private void styleTextField() {
        if (customFont != null) {
            textField.setFont(customFont);
        }
        if (textField.getStyle() == null || textField.getStyle().isEmpty()) {
            textField.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-border-color: transparent; " +
                            "-fx-background-radius: 0; " +
                            "-fx-padding: 10 6; " +
                            "-fx-text-fill: " + TEXT_PRIMARY + "; " +
                            "-fx-prompt-text-fill: " + TEXT_SECONDARY + ";");
        }
    }

    /* ---------------- List cell ---------------- */

    private class SuggestionCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                setText(item);
                setPadding(new Insets(10, 16, 10, 16));
                if (customFont != null) {
                    setFont(customFont);
                } else {
                    setFont(Font.font("Segoe UI", 14));
                }

                if (isSelected()) {
                    setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");
                    setTextFill(Color.WHITE);
                } else {
                    setStyle("-fx-background-color: transparent;");
                    setTextFill(Color.web(TEXT_PRIMARY));
                }
            }
        }
    }

    /* ---------------- Popup helpers ---------------- */

    private void showListPopup() {
        if (listItems.isEmpty())
            return;

        if (!listPopup.isShowing()) {
            Point2D p = textField.localToScreen(0, textField.getHeight());
            double x = p.getX();
            double y = p.getY();
            listView.setPrefWidth(Math.max(textField.getWidth(), 300));
            int visible = Math.min(listItems.size(), MAX_VISIBLE_ROWS);
            listView.setPrefHeight(visible * ROW_HEIGHT);
            listPopup.show(textField, x, y);
        } else {
            int visible = Math.min(listItems.size(), MAX_VISIBLE_ROWS);
            listView.setPrefHeight(visible * ROW_HEIGHT);
        }
    }

    private void hideListPopup() {
        if (listPopup.isShowing())
            listPopup.hide();
    }

    private void populateListItems() {
        listItems.setAll(filteredSuggestions);
        if (!listItems.isEmpty()) {
            if (selectedIndex < 0 || selectedIndex >= listItems.size())
                selectedIndex = 0;
            listView.getSelectionModel().select(selectedIndex);
            listView.scrollTo(selectedIndex);
        }
    }

    /* ---------------- Events & listeners ---------------- */

    private void attachListeners() {
        // Debounced typing listener
        textListener = (obs, oldText, newText) -> {
            if (isSelectingSuggestion)
                return;

            debounce.stop();
            debounce.setOnFinished(e -> handleTypingFilter(newText));
            debounce.playFromStart();
        };
        textField.textProperty().addListener(textListener);

        // Key handling on textfield
        textField.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();

            // SPACE on empty -> show all suggestions
            if (code == KeyCode.SPACE) {
                if (textField.getText() == null || textField.getText().isEmpty()) {
                    showAllSuggestions();
                    event.consume();
                    return;
                }
            }

            if (code == KeyCode.ESCAPE) {
                hideListPopup();
                selectedIndex = 0;
                event.consume();
                return;
            }

            if (code == KeyCode.ENTER) {
                // ENTER selects highlighted suggestion (Option A)
                if (listPopup.isShowing() && !listItems.isEmpty()) {
                    int sel = listView.getSelectionModel().getSelectedIndex();
                    if (sel < 0)
                        sel = 0;
                    selectSuggestion(sel);
                } else {
                    if (textField.getText() == null || textField.getText().isEmpty()) {
                        showAllSuggestions();
                    } else {
                        handleTypingFilter(textField.getText());
                        if (!filteredSuggestions.isEmpty()) {
                            selectSuggestion(0);
                        } else {
                            moveFocusToNextField();
                        }
                    }
                }
                event.consume();
                return;
            }

            if (code == KeyCode.DOWN) {
                if (!listPopup.isShowing()) {
                    handleTypingFilter(textField.getText());
                } else {
                    int next = Math.min(listView.getItems().size() - 1,
                            listView.getSelectionModel().getSelectedIndex() + 1);
                    listView.getSelectionModel().select(next);
                    listView.scrollTo(next);
                }
                event.consume();
                return;
            }

            if (code == KeyCode.UP) {
                if (listPopup.isShowing()) {
                    int prev = Math.max(0, listView.getSelectionModel().getSelectedIndex() - 1);
                    listView.getSelectionModel().select(prev);
                    listView.scrollTo(prev);
                }
                event.consume();
                return;
            }

            if (code == KeyCode.TAB) {
                if (listPopup.isShowing() && !listItems.isEmpty()) {
                    int sel = listView.getSelectionModel().getSelectedIndex();
                    if (sel < 0)
                        sel = 0;
                    selectSuggestion(sel);
                    event.consume();
                }
            }
        });

        // Mouse click selection
        listView.setOnMouseClicked(evt -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                selectSuggestion(listView.getSelectionModel().getSelectedIndex());
            }
        });

        // Hide popup when textfield loses focus (but avoid hiding when clicking list)
        textField.focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                Platform.runLater(() -> {
                    Node focus = textField.getScene() != null ? textField.getScene().getFocusOwner() : null;
                    if (focus == null || (focus != listView && focus != textField)) {
                        hideListPopup();
                    }
                });
            }
        });
    }

    /* ---------------- Filtering & selection ---------------- */

    // startsWith filter (case-insensitive)
    private void handleTypingFilter(String newText) {
        if (newText == null || newText.isEmpty()) {
            hideListPopup();
            filteredSuggestions.clear();
            selectedIndex = 0;
            return;
        }

        String q = newText.toLowerCase().trim();

        filteredSuggestions = suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(q))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        if (filteredSuggestions.isEmpty()) {
            hideListPopup();
            selectedIndex = 0;
            return;
        }

        // update items and show popup
        listItems.setAll(filteredSuggestions);
        populateListItems();
        showListPopup();
        listView.getSelectionModel().select(0);
        listView.scrollTo(0);
    }

    private void showAllSuggestions() {
        filteredSuggestions = new ArrayList<>(suggestions);
        filteredSuggestions.sort(String.CASE_INSENSITIVE_ORDER);

        if (filteredSuggestions.isEmpty()) {
            hideListPopup();
            return;
        }

        listItems.setAll(filteredSuggestions);
        populateListItems();
        showListPopup();
        listView.getSelectionModel().select(0);
        listView.scrollTo(0);
    }

    private void selectSuggestion(int index) {
        if (filteredSuggestions == null || filteredSuggestions.isEmpty())
            return;
        if (index < 0 || index >= filteredSuggestions.size())
            return;

        isSelectingSuggestion = true;
        try {
            String selected = filteredSuggestions.get(index);
            textField.setText(selected);
            textField.positionCaret(selected.length());
            hideListPopup();
            filteredSuggestions.clear();
            listItems.clear();
            selectedIndex = 0;
        } finally {
            Platform.runLater(() -> isSelectingSuggestion = false);
            moveFocusToNextField();
        }
    }

    private void moveFocusToNextField() {
        if (nextFocusField != null)
            Platform.runLater(nextFocusField::requestFocus);
    }

    /* ---------------- Utilities ---------------- */

    public String getCurrentSuggestion() {
        if (filteredSuggestions == null || filteredSuggestions.isEmpty())
            return null;
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0)
            idx = 0;
        return filteredSuggestions.size() > idx ? filteredSuggestions.get(idx) : null;
    }

    public void selectCurrentSuggestion() {
        int idx = listView.getSelectionModel().getSelectedIndex();
        if (idx < 0 && !filteredSuggestions.isEmpty())
            idx = 0;
        if (idx >= 0 && idx < filteredSuggestions.size())
            selectSuggestion(idx);
    }

    public List<String> getFilteredSuggestions() {
        return new ArrayList<>(filteredSuggestions);
    }

    public boolean isPopupShowing() {
        return listPopup.isShowing();
    }

    public void hidePopup() {
        hideListPopup();
        selectedIndex = 0;
    }
}

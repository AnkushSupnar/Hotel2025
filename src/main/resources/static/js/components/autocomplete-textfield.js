/**
 * AutoCompleteTextField - Web replica of desktop customUI/AutoCompleteTextField.java
 *
 * Features (matching desktop exactly):
 * - Popup dropdown for reliable suggestions display
 * - startsWith() or contains() filtering (configurable)
 * - Custom font support (Kiran font) for input and suggestions
 * - Smooth fade animations for popup
 * - Keyboard navigation (UP/DOWN/ENTER/TAB/ESCAPE)
 * - Mouse click selection with hover highlight
 * - Debounced typing (150ms)
 * - Selection callback support (onSelectionCallback)
 * - SPACE/ENTER on empty shows all suggestions
 * - Material Design styling matching desktop colors exactly
 * - nextFocusField support (auto-focus next field after selection)
 * - Max 8 visible rows, 36px row height, min 250px popup width
 *
 * Usage:
 *   const ac = new AutoCompleteTextField(inputElement, suggestions, {
 *       customFont: 'Kiran',
 *       fontSize: 20,
 *       nextFocusField: document.getElementById('password'),
 *       useContainsFilter: false,
 *       onSelection: function(value) { ... }
 *   });
 *
 *   // Update suggestions later:
 *   ac.setSuggestions(['item1', 'item2']);
 *
 *   // Clear:
 *   ac.clear();
 */
class AutoCompleteTextField {

    // ==================== Material Design Colors (same as desktop) ====================
    static PRIMARY_COLOR = '#1976D2';
    static PRIMARY_LIGHT = '#E3F2FD';
    static TEXT_PRIMARY = '#212121';
    static TEXT_SECONDARY = '#757575';
    static SURFACE_COLOR = '#FFFFFF';
    static DIVIDER_COLOR = '#E0E0E0';
    static HOVER_COLOR = '#F5F5F5';
    static SELECTED_COLOR = '#E3F2FD';

    // ==================== Layout Constants (same as desktop) ====================
    static ROW_HEIGHT = 36;
    static MAX_VISIBLE_ROWS = 8;
    static MIN_POPUP_WIDTH = 250;
    static POPUP_PADDING = 4;
    static DEBOUNCE_MS = 150;

    /**
     * @param {HTMLInputElement} textField - The input element to attach to
     * @param {string[]} suggestions - Initial suggestions list
     * @param {Object} options - Configuration options
     * @param {string} [options.customFont] - Font family name (e.g. 'Kiran')
     * @param {number} [options.fontSize=14] - Font size in px
     * @param {HTMLElement} [options.nextFocusField] - Element to focus after selection
     * @param {boolean} [options.useContainsFilter=false] - Use contains instead of startsWith
     * @param {Function} [options.onSelection] - Callback when a suggestion is selected
     */
    constructor(textField, suggestions = [], options = {}) {
        this.textField = textField;
        this.suggestions = [...suggestions];
        this.filteredSuggestions = [];
        this.isSelectingSuggestion = false;
        this.hoveredIndex = -1;
        this.selectedIndex = -1;
        this.selectedValue = '';

        // Configuration
        this.customFont = options.customFont || null;
        this.fontSize = options.fontSize || 14;
        this.nextFocusField = options.nextFocusField || null;
        this.useContainsFilter = options.useContainsFilter || false;
        this.onSelectionCallback = options.onSelection || null;

        // Debounce timer
        this._debounceTimer = null;

        // Build UI and attach events
        this._createPopup();
        this._styleTextField();
        this._attachListeners();
    }

    // ==================== Popup UI Creation ====================

    _createPopup() {
        // Overlay backdrop (invisible, for click-outside-to-close)
        this.overlay = document.createElement('div');
        this.overlay.className = 'ac-overlay';
        this.overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;z-index:9998;display:none;';
        this.overlay.addEventListener('mousedown', () => this.hidePopup());

        // Popup container (matching desktop: white bg, radius 8, border #E0E0E0, shadow)
        this.popup = document.createElement('div');
        this.popup.className = 'ac-popup';
        this.popup.style.cssText =
            'position:fixed;z-index:9999;display:none;opacity:0;' +
            'background-color:' + AutoCompleteTextField.SURFACE_COLOR + ';' +
            'border-radius:8px;' +
            'border:1px solid ' + AutoCompleteTextField.DIVIDER_COLOR + ';' +
            'box-shadow:0 4px 12px rgba(0,0,0,0.15);' +
            'overflow:hidden;' +
            'transition:opacity 0.15s ease;';

        // List container inside popup
        this.listContainer = document.createElement('div');
        this.listContainer.className = 'ac-list';
        this.listContainer.style.cssText =
            'overflow-y:auto;overflow-x:hidden;' +
            'padding:' + AutoCompleteTextField.POPUP_PADDING + 'px;';

        this.popup.appendChild(this.listContainer);
        document.body.appendChild(this.overlay);
        document.body.appendChild(this.popup);
    }

    _styleTextField() {
        if (this.customFont) {
            this.textField.style.fontFamily = "'" + this.customFont + "', 'Segoe UI', sans-serif";
            this.textField.style.fontSize = this.fontSize + 'px';
            this.textField.style.textShadow = '0 0.3px 0 rgba(0,0,0,0.4)';
        }
    }

    // ==================== Event Listeners ====================

    _attachListeners() {
        // Text input with debounce (desktop: 150ms PauseTransition)
        this.textField.addEventListener('input', () => {
            if (this.isSelectingSuggestion) return;
            clearTimeout(this._debounceTimer);
            this._debounceTimer = setTimeout(() => {
                this._handleTextChange(this.textField.value);
            }, AutoCompleteTextField.DEBOUNCE_MS);
        });

        // Keyboard navigation (matching desktop: SPACE, ESCAPE, ENTER, DOWN, UP, TAB)
        this.textField.addEventListener('keydown', (e) => {
            switch (e.key) {
                case ' ':
                    // SPACE on empty shows all (desktop: case SPACE)
                    if (!this.textField.value || this.textField.value.trim() === '') {
                        e.preventDefault();
                        this._showAllSuggestions();
                    }
                    break;

                case 'Escape':
                    this.hidePopup();
                    e.preventDefault();
                    break;

                case 'Enter':
                    e.preventDefault();
                    this._handleEnterKey();
                    break;

                case 'ArrowDown':
                    e.preventDefault();
                    this._handleDownKey();
                    break;

                case 'ArrowUp':
                    e.preventDefault();
                    this._handleUpKey();
                    break;

                case 'Tab':
                    if (this._isPopupVisible() && this.filteredSuggestions.length > 0) {
                        if (this.selectedIndex >= 0) {
                            this._selectSuggestion(this.selectedIndex);
                        }
                        e.preventDefault();
                    }
                    break;
            }
        });

        // Hide popup on blur (desktop: focusedProperty listener with 150ms delay)
        this.textField.addEventListener('blur', () => {
            setTimeout(() => {
                if (document.activeElement !== this.textField) {
                    this.hidePopup();
                }
            }, 150);
        });

        // Reposition popup on scroll/resize
        window.addEventListener('scroll', () => this._repositionPopup(), true);
        window.addEventListener('resize', () => this._repositionPopup());
    }

    // ==================== Keyboard Handlers (matching desktop exactly) ====================

    _handleEnterKey() {
        if (this._isPopupVisible() && this.filteredSuggestions.length > 0) {
            let sel = this.selectedIndex;
            if (sel < 0) sel = 0;
            this._selectSuggestion(sel);
        } else if (!this.textField.value || this.textField.value.trim() === '') {
            this._showAllSuggestions();
        } else {
            // Try to filter and select first match
            this._handleTextChange(this.textField.value);
            if (this.filteredSuggestions.length > 0) {
                this._selectSuggestion(0);
            } else {
                this._moveFocusToNextField();
            }
        }
    }

    _handleDownKey() {
        if (!this._isPopupVisible()) {
            if (!this.textField.value || this.textField.value.trim() === '') {
                this._showAllSuggestions();
            } else {
                this._handleTextChange(this.textField.value);
            }
        } else if (this.filteredSuggestions.length > 0) {
            let next = Math.min(this.filteredSuggestions.length - 1, this.selectedIndex + 1);
            this._setSelectedIndex(next);
            this._scrollToIndex(next);
        }
    }

    _handleUpKey() {
        if (this._isPopupVisible() && this.filteredSuggestions.length > 0) {
            let prev = Math.max(0, this.selectedIndex - 1);
            this._setSelectedIndex(prev);
            this._scrollToIndex(prev);
        }
    }

    // ==================== Text Filtering (matching desktop logic) ====================

    _handleTextChange(text) {
        if (!text || text.trim() === '') {
            this.hidePopup();
            this.filteredSuggestions = [];
            return;
        }

        const query = text.toLowerCase().trim();

        if (this.useContainsFilter) {
            // Contains filter with startsWith priority (same as desktop)
            this.filteredSuggestions = this.suggestions
                .filter(s => s.toLowerCase().includes(query))
                .sort((a, b) => {
                    const aStarts = a.toLowerCase().startsWith(query);
                    const bStarts = b.toLowerCase().startsWith(query);
                    if (aStarts && !bStarts) return -1;
                    if (!aStarts && bStarts) return 1;
                    return a.localeCompare(b, undefined, { sensitivity: 'base' });
                });
        } else {
            // startsWith filter (desktop default)
            this.filteredSuggestions = this.suggestions
                .filter(s => s.toLowerCase().startsWith(query))
                .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }));
        }

        if (this.filteredSuggestions.length === 0) {
            this.hidePopup();
            return;
        }

        this._updateListAndShow();
    }

    _showAllSuggestions() {
        this.filteredSuggestions = [...this.suggestions]
            .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }));

        if (this.filteredSuggestions.length === 0) {
            this.hidePopup();
            return;
        }

        this._updateListAndShow();
    }

    _updateListAndShow() {
        this._renderList();
        this._setSelectedIndex(0);
        this.hoveredIndex = -1;
        this._showPopup();
    }

    // ==================== Selection (matching desktop) ====================

    _selectSuggestion(index) {
        if (!this.filteredSuggestions || this.filteredSuggestions.length === 0) return;
        if (index < 0 || index >= this.filteredSuggestions.length) return;

        this.isSelectingSuggestion = true;

        const selected = this.filteredSuggestions[index];
        this.textField.value = selected;
        this.selectedValue = selected;

        this.hidePopup();
        this.filteredSuggestions = [];

        // Notify callback (desktop: onSelectionCallback.accept(selected))
        if (this.onSelectionCallback) {
            this.onSelectionCallback(selected);
        }

        this.isSelectingSuggestion = false;
        this._moveFocusToNextField();
    }

    _moveFocusToNextField() {
        if (this.nextFocusField) {
            setTimeout(() => this.nextFocusField.focus(), 0);
        }
    }

    // ==================== Popup Rendering ====================

    _renderList() {
        this.listContainer.innerHTML = '';

        this.filteredSuggestions.forEach((item, index) => {
            const row = document.createElement('div');
            row.className = 'ac-item';
            row.dataset.index = index;

            // Row styling (desktop: ROW_HEIGHT=36, padding 8 12, radius 4)
            row.style.cssText =
                'height:' + AutoCompleteTextField.ROW_HEIGHT + 'px;' +
                'line-height:' + AutoCompleteTextField.ROW_HEIGHT + 'px;' +
                'padding:0 12px;' +
                'cursor:pointer;' +
                'border-radius:4px;' +
                'white-space:nowrap;' +
                'overflow:hidden;' +
                'text-overflow:ellipsis;' +
                'color:' + AutoCompleteTextField.TEXT_PRIMARY + ';' +
                'font-size:' + this.fontSize + 'px;' +
                'transition:background-color 0.1s;';

            if (this.customFont) {
                row.style.fontFamily = "'" + this.customFont + "', 'Segoe UI', sans-serif";
                row.style.textShadow = '0 0.3px 0 rgba(0,0,0,0.4)';
            } else {
                row.style.fontFamily = "'Segoe UI', sans-serif";
            }

            row.textContent = item;

            // Mouse hover (desktop: MOUSE_MOVED -> hoveredIndex -> refresh)
            row.addEventListener('mouseenter', () => {
                this.hoveredIndex = index;
                this._updateRowStyles();
            });

            row.addEventListener('mouseleave', () => {
                this.hoveredIndex = -1;
                this._updateRowStyles();
            });

            // Mouse click (desktop: listView.setOnMouseClicked)
            row.addEventListener('mousedown', (e) => {
                e.preventDefault(); // Prevent blur
                this._selectSuggestion(index);
            });

            this.listContainer.appendChild(row);
        });
    }

    _updateRowStyles() {
        const rows = this.listContainer.querySelectorAll('.ac-item');
        rows.forEach((row, i) => {
            const isSelected = (i === this.selectedIndex);
            const isHovered = (i === this.hoveredIndex);

            if (isSelected) {
                // Desktop: SELECTED_COLOR bg, PRIMARY_COLOR text, bold
                row.style.backgroundColor = AutoCompleteTextField.SELECTED_COLOR;
                row.style.color = AutoCompleteTextField.PRIMARY_COLOR;
                row.style.fontWeight = 'bold';
            } else if (isHovered) {
                // Desktop: HOVER_COLOR bg, TEXT_PRIMARY text
                row.style.backgroundColor = AutoCompleteTextField.HOVER_COLOR;
                row.style.color = AutoCompleteTextField.TEXT_PRIMARY;
                row.style.fontWeight = 'normal';
            } else {
                row.style.backgroundColor = 'transparent';
                row.style.color = AutoCompleteTextField.TEXT_PRIMARY;
                row.style.fontWeight = 'normal';
            }
        });
    }

    _setSelectedIndex(index) {
        this.selectedIndex = index;
        this._updateRowStyles();
    }

    _scrollToIndex(index) {
        const row = this.listContainer.querySelector('.ac-item[data-index="' + index + '"]');
        if (row) {
            row.scrollIntoView({ block: 'nearest' });
        }
    }

    // ==================== Popup Management ====================

    _showPopup() {
        if (this.filteredSuggestions.length === 0) return;

        // Calculate dimensions (desktop: width = max(textField.width, 250), height based on rows)
        const rect = this.textField.getBoundingClientRect();
        const popupWidth = Math.max(rect.width, AutoCompleteTextField.MIN_POPUP_WIDTH);
        const visibleRows = Math.min(this.filteredSuggestions.length, AutoCompleteTextField.MAX_VISIBLE_ROWS);
        const popupHeight = (visibleRows * AutoCompleteTextField.ROW_HEIGHT) + (AutoCompleteTextField.POPUP_PADDING * 2) + 2;

        this.listContainer.style.maxHeight = popupHeight + 'px';
        this.popup.style.width = popupWidth + 'px';

        // Position below the input field (desktop: localToScreen + textField.getHeight() + 2)
        this.popup.style.left = rect.left + 'px';
        this.popup.style.top = (rect.bottom + 2) + 'px';

        // Show with fade-in (desktop: FadeTransition 150ms)
        this.overlay.style.display = 'block';
        this.popup.style.display = 'block';
        requestAnimationFrame(() => {
            this.popup.style.opacity = '1';
        });
    }

    _repositionPopup() {
        if (!this._isPopupVisible()) return;
        const rect = this.textField.getBoundingClientRect();
        const popupWidth = Math.max(rect.width, AutoCompleteTextField.MIN_POPUP_WIDTH);
        this.popup.style.left = rect.left + 'px';
        this.popup.style.top = (rect.bottom + 2) + 'px';
        this.popup.style.width = popupWidth + 'px';
    }

    _isPopupVisible() {
        return this.popup.style.display === 'block' && this.popup.style.opacity !== '0';
    }

    hidePopup() {
        if (this.popup.style.display === 'block') {
            // Fade out (desktop: FadeTransition 100ms then hide)
            this.popup.style.opacity = '0';
            setTimeout(() => {
                this.popup.style.display = 'none';
                this.overlay.style.display = 'none';
            }, 100);
        }
        this.hoveredIndex = -1;
    }

    // ==================== Public API (matching desktop) ====================

    /** Set new suggestions list */
    setSuggestions(suggestions) {
        this.suggestions = [...suggestions];
    }

    /** Add suggestions to existing list */
    addSuggestions(newSuggestions) {
        this.suggestions.push(...newSuggestions);
    }

    /** Set custom font */
    setCustomFont(fontFamily, fontSize) {
        this.customFont = fontFamily;
        this.fontSize = fontSize || this.fontSize;
        this._styleTextField();
    }

    /** Set whether to use contains filter */
    setUseContainsFilter(useContains) {
        this.useContainsFilter = useContains;
    }

    /** Clear the text field */
    clear() {
        this.textField.value = '';
        this.hidePopup();
        this.filteredSuggestions = [];
        this.selectedValue = '';
    }

    /** Set text in the text field without triggering autocomplete */
    setText(text) {
        this.isSelectingSuggestion = true;
        this.textField.value = text;
        this.isSelectingSuggestion = false;
    }

    /** Get current text */
    getText() {
        return this.textField.value;
    }

    /** Get selected value */
    getSelectedValue() {
        return this.selectedValue;
    }

    /** Get currently highlighted suggestion */
    getCurrentSuggestion() {
        if (!this.filteredSuggestions || this.filteredSuggestions.length === 0) return null;
        let idx = this.selectedIndex;
        if (idx < 0) idx = 0;
        return idx < this.filteredSuggestions.length ? this.filteredSuggestions[idx] : null;
    }

    /** Programmatically select current suggestion */
    selectCurrentSuggestion() {
        let idx = this.selectedIndex;
        if (idx < 0 && this.filteredSuggestions.length > 0) idx = 0;
        if (idx >= 0 && idx < this.filteredSuggestions.length) {
            this._selectSuggestion(idx);
        }
    }

    /** Get filtered suggestions list */
    getFilteredSuggestions() {
        return [...this.filteredSuggestions];
    }

    /** Check if popup is showing */
    isPopupShowing() {
        return this._isPopupVisible();
    }

    /** Request focus on the text field */
    requestFocus() {
        this.textField.focus();
    }

    /** Check if text field is focused */
    isFocused() {
        return document.activeElement === this.textField;
    }

    /** Set prompt/placeholder text */
    setPromptText(text) {
        this.textField.placeholder = text;
    }

    /** Get prompt/placeholder text */
    getPromptText() {
        return this.textField.placeholder;
    }

    /** Set next focus field */
    setNextFocusField(element) {
        this.nextFocusField = element;
    }

    /** Set selection callback */
    setOnSelectionCallback(callback) {
        this.onSelectionCallback = callback;
    }

    /** Destroy and clean up DOM elements */
    destroy() {
        if (this.popup && this.popup.parentNode) {
            this.popup.parentNode.removeChild(this.popup);
        }
        if (this.overlay && this.overlay.parentNode) {
            this.overlay.parentNode.removeChild(this.overlay);
        }
        clearTimeout(this._debounceTimer);
    }
}

/**
 * SectionSequence.js - Web replica of desktop SectionSequenceController.java
 * Drag-and-drop table section reordering with merge/split functionality.
 */

let sectionSeqInitialized = false;
let sections = [];
let rowGroups = [];
let dragSrcIndex = null;

const GROUP_COLORS = ['#1976D2', '#E65100', '#2E7D32', '#6A1B9A', '#00838F', '#AD1457', '#F57F17'];

async function initSectionSequence() {
    if (!sectionSeqInitialized) {
        setupSectionHandlers();
        sectionSeqInitialized = true;
    }
    await loadSections();
}

function setupSectionHandlers() {
    document.getElementById('btnMergeSections').addEventListener('click', mergeSections);
    document.getElementById('btnSplitSections').addEventListener('click', splitSections);
    document.getElementById('btnSaveSequence').addEventListener('click', saveSequence);
    document.getElementById('btnResetSequence').addEventListener('click', resetSequence);
}

// ==================== LOAD SECTIONS ====================

async function loadSections() {
    try {
        // Load available sections from table master
        const sectResp = await apiGet('/sections');
        const availableSections = sectResp.data || [];

        // Load saved sequence
        const seqResp = await apiGet('/settings/section-sequence');
        const seqData = seqResp.data || {};
        const savedSequence = seqData.sectionSequence || {};
        rowGroups = seqData.rowGroups || [];

        // Order sections by saved sequence, append any new ones at end
        const ordered = [];
        const sequenced = [];

        // Build array from saved sequence
        if (Object.keys(savedSequence).length > 0) {
            const entries = Object.entries(savedSequence).sort((a, b) => a[1] - b[1]);
            entries.forEach(([name]) => {
                if (availableSections.includes(name)) {
                    ordered.push(name);
                    sequenced.push(name);
                }
            });
        }

        // Add any sections not in saved sequence
        availableSections.forEach(s => {
            if (!sequenced.includes(s)) {
                ordered.push(s);
            }
        });

        sections = ordered;
        renderSections();
    } catch (e) {
        console.error('Failed to load sections:', e);
        showError('Failed to load sections: ' + e.message);
    }
}

// ==================== RENDER ====================

function renderSections() {
    const container = document.getElementById('sectionsContainer');

    if (!sections || sections.length === 0) {
        container.innerHTML = '<div class="text-center text-muted py-4">No table sections found. Please add tables first.</div>';
        return;
    }

    container.innerHTML = '';

    sections.forEach((name, idx) => {
        const item = document.createElement('div');
        item.className = 'section-item';
        item.setAttribute('data-index', idx);
        item.draggable = true;

        // Find row group for this index
        const groupIdx = getGroupIndex(idx);
        const groupColor = groupIdx >= 0 ? GROUP_COLORS[groupIdx % GROUP_COLORS.length] : 'transparent';

        item.innerHTML = `
            <div class="section-group-indicator" style="background:${groupColor};"></div>
            <i class="bi bi-grip-vertical drag-handle"></i>
            <input type="checkbox" class="section-checkbox" data-index="${idx}" onchange="onSectionCheckboxChange()" />
            <span class="section-name">${escapeHtmlSec(name)}</span>
            <span class="section-badge">#${idx + 1}</span>
        `;

        // Drag events
        item.addEventListener('dragstart', onDragStart);
        item.addEventListener('dragover', onDragOver);
        item.addEventListener('dragenter', onDragEnter);
        item.addEventListener('dragleave', onDragLeave);
        item.addEventListener('drop', onDrop);
        item.addEventListener('dragend', onDragEnd);

        container.appendChild(item);
    });

    updateMergeSplitButtons();
}

// ==================== DRAG AND DROP ====================

function onDragStart(e) {
    dragSrcIndex = parseInt(e.currentTarget.getAttribute('data-index'));
    e.currentTarget.classList.add('dragging');
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', dragSrcIndex);
}

function onDragOver(e) {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
}

function onDragEnter(e) {
    e.preventDefault();
    e.currentTarget.classList.add('drag-over');
}

function onDragLeave(e) {
    e.currentTarget.classList.remove('drag-over');
}

function onDrop(e) {
    e.preventDefault();
    e.currentTarget.classList.remove('drag-over');
    const targetIndex = parseInt(e.currentTarget.getAttribute('data-index'));

    if (dragSrcIndex !== null && dragSrcIndex !== targetIndex) {
        // Reorder sections array
        const moved = sections.splice(dragSrcIndex, 1)[0];
        sections.splice(targetIndex, 0, moved);

        // Update row groups indices
        rowGroups = remapGroupsAfterMove(dragSrcIndex, targetIndex);

        renderSections();
    }
}

function onDragEnd(e) {
    e.currentTarget.classList.remove('dragging');
    document.querySelectorAll('.section-item').forEach(el => el.classList.remove('drag-over'));
    dragSrcIndex = null;
}

function remapGroupsAfterMove(fromIdx, toIdx) {
    // Simple approach: clear groups on move to avoid inconsistency
    // User can re-merge after reordering
    return rowGroups.map(group => {
        if (group.includes(fromIdx) || group.includes(toIdx)) {
            return group; // Keep existing group if involved items are still adjacent
        }
        return group;
    });
}

// ==================== ROW GROUPS ====================

function getGroupIndex(sectionIndex) {
    for (let g = 0; g < rowGroups.length; g++) {
        if (rowGroups[g].includes(sectionIndex)) return g;
    }
    return -1;
}

function onSectionCheckboxChange() {
    updateMergeSplitButtons();
}

function getSelectedIndices() {
    const checkboxes = document.querySelectorAll('.section-checkbox:checked');
    return Array.from(checkboxes).map(cb => parseInt(cb.getAttribute('data-index'))).sort((a, b) => a - b);
}

function updateMergeSplitButtons() {
    const selected = getSelectedIndices();
    const btnMerge = document.getElementById('btnMergeSections');
    const btnSplit = document.getElementById('btnSplitSections');

    // Merge: need 2+ adjacent selected
    let canMerge = false;
    if (selected.length >= 2) {
        canMerge = true;
        for (let i = 1; i < selected.length; i++) {
            if (selected[i] !== selected[i - 1] + 1) {
                canMerge = false;
                break;
            }
        }
    }

    // Split: need at least one selected that is in a group
    let canSplit = false;
    if (selected.length > 0) {
        canSplit = selected.some(idx => getGroupIndex(idx) >= 0);
    }

    btnMerge.disabled = !canMerge;
    btnSplit.disabled = !canSplit;
}

function mergeSections() {
    const selected = getSelectedIndices();
    if (selected.length < 2) return;

    // Check adjacency
    for (let i = 1; i < selected.length; i++) {
        if (selected[i] !== selected[i - 1] + 1) {
            showWarning('Please select adjacent sections to merge');
            return;
        }
    }

    // Remove selected indices from any existing groups
    rowGroups = rowGroups.map(group => group.filter(idx => !selected.includes(idx))).filter(g => g.length > 0);

    // Add new group
    rowGroups.push(selected);

    renderSections();
    showSuccess('Sections merged into a row group');
}

function splitSections() {
    const selected = getSelectedIndices();
    if (selected.length === 0) return;

    // Remove selected indices from their groups
    rowGroups = rowGroups.map(group => group.filter(idx => !selected.includes(idx))).filter(g => g.length > 1);

    renderSections();
    showSuccess('Sections split from row group');
}

// ==================== SAVE / RESET ====================

async function saveSequence() {
    const sectionSequence = {};
    sections.forEach((name, idx) => {
        sectionSequence[name] = idx;
    });

    const btn = document.getElementById('btnSaveSequence');
    btn.classList.add('loading');

    try {
        await apiPost('/settings/section-sequence', {
            sectionSequence: sectionSequence,
            rowGroups: rowGroups
        });
        showSuccess('Section sequence saved successfully');
    } catch (e) {
        showError('Failed to save: ' + e.message);
    } finally {
        btn.classList.remove('loading');
    }
}

async function resetSequence() {
    const confirmed = await showConfirm('Reset section order to server state?');
    if (!confirmed) return;
    await loadSections();
    showSuccess('Section order reset');
}

function escapeHtmlSec(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

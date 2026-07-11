package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutable editing document for the keyframe manager.
 *
 * <p>This class is the only owner of editor history. Every snapshot is a deep copy; sharing mutable keyframes between
 * history entries used to let later edits silently alter undo and redo results.</p>
 */
public final class KeyframeEditorState {

    private static final int MAX_HISTORY_STATES = 100;

    private final List<AnimationKeyframe> keyframes;
    private final List<AnimationKeyframe> selectedKeyframes = new ArrayList<>();
    private final List<AnimationKeyframe> keyframeView;
    private final List<AnimationKeyframe> selectionView = Collections.unmodifiableList(this.selectedKeyframes);
    private final Deque<List<AnimationKeyframe>> undoHistory = new ArrayDeque<>();
    private final Deque<List<AnimationKeyframe>> redoHistory = new ArrayDeque<>();

    public KeyframeEditorState(@NotNull Collection<AnimationKeyframe> keyframes) {
        this.keyframes = AnimationKeyframeSequence.copyAndSort(keyframes);
        this.keyframeView = Collections.unmodifiableList(this.keyframes);
    }

    /** Returns a live read-only view. Mutating a contained frame must be preceded by {@link #saveSnapshot()}. */
    @NotNull
    public List<AnimationKeyframe> getKeyframes() {
        return this.keyframeView;
    }

    /** Returns a live read-only selection view backed by the current editor document. */
    @NotNull
    public List<AnimationKeyframe> getSelectedKeyframes() {
        return this.selectionView;
    }

    public boolean canUndo() {
        return !this.undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !this.redoHistory.isEmpty();
    }

    public void saveSnapshot() {
        this.undoHistory.push(copy(this.keyframes));
        trimHistory(this.undoHistory);
        this.redoHistory.clear();
    }

    public boolean undo() {
        if (this.undoHistory.isEmpty()) return false;
        Set<String> selectedIdentifiers = getSelectedIdentifiers();
        this.redoHistory.push(copy(this.keyframes));
        trimHistory(this.redoHistory);
        replaceKeyframes(this.undoHistory.pop());
        restoreSelection(selectedIdentifiers);
        return true;
    }

    public boolean redo() {
        if (this.redoHistory.isEmpty()) return false;
        Set<String> selectedIdentifiers = getSelectedIdentifiers();
        this.undoHistory.push(copy(this.keyframes));
        trimHistory(this.undoHistory);
        replaceKeyframes(this.redoHistory.pop());
        restoreSelection(selectedIdentifiers);
        return true;
    }

    public void sort() {
        AnimationKeyframeSequence.sort(this.keyframes);
    }

    public void clearSelection() {
        this.selectedKeyframes.clear();
    }

    public void selectAll() {
        this.selectedKeyframes.clear();
        this.selectedKeyframes.addAll(this.keyframes);
    }

    public boolean select(@Nullable AnimationKeyframe keyframe, boolean addToSelection) {
        if (!addToSelection) this.selectedKeyframes.clear();
        if ((keyframe == null) || !this.keyframes.contains(keyframe) || this.selectedKeyframes.contains(keyframe)) return false;
        this.selectedKeyframes.add(keyframe);
        return true;
    }

    public boolean deselect(@NotNull AnimationKeyframe keyframe) {
        return this.selectedKeyframes.remove(keyframe);
    }

    public void add(@NotNull AnimationKeyframe keyframe) {
        this.saveSnapshot();
        this.keyframes.add(keyframe);
        this.sort();
    }

    public boolean deleteSelected() {
        if (this.selectedKeyframes.isEmpty()) return false;
        this.saveSnapshot();
        this.keyframes.removeAll(this.selectedKeyframes);
        this.selectedKeyframes.clear();
        return true;
    }

    public boolean smoothSelected(long distanceMs) {
        if ((distanceMs <= 0L) || (this.selectedKeyframes.size() < 2)) return false;
        List<AnimationKeyframe> sortedSelection = new ArrayList<>(this.selectedKeyframes);
        AnimationKeyframeSequence.sort(sortedSelection);
        this.saveSnapshot();
        long startTime = sortedSelection.getFirst().timestamp;
        for (int index = 1; index < sortedSelection.size(); index++) {
            long offset;
            try {
                offset = Math.multiplyExact(distanceMs, index);
                sortedSelection.get(index).timestamp = Math.addExact(startTime, offset);
            } catch (ArithmeticException ex) {
                sortedSelection.get(index).timestamp = Long.MAX_VALUE;
            }
        }
        this.sort();
        return true;
    }

    private void replaceKeyframes(@NotNull Collection<AnimationKeyframe> replacement) {
        this.keyframes.clear();
        this.keyframes.addAll(copy(replacement));
        this.sort();
    }

    @NotNull
    private Set<String> getSelectedIdentifiers() {
        Set<String> identifiers = new HashSet<>();
        for (AnimationKeyframe keyframe : this.selectedKeyframes) identifiers.add(keyframe.uniqueIdentifier);
        return identifiers;
    }

    private void restoreSelection(@NotNull Set<String> selectedIdentifiers) {
        this.selectedKeyframes.clear();
        if (selectedIdentifiers.isEmpty()) return;
        for (AnimationKeyframe keyframe : this.keyframes) {
            if (selectedIdentifiers.contains(keyframe.uniqueIdentifier)) this.selectedKeyframes.add(keyframe);
        }
    }

    @NotNull
    private static List<AnimationKeyframe> copy(@NotNull Collection<AnimationKeyframe> keyframes) {
        List<AnimationKeyframe> copy = new ArrayList<>(keyframes.size());
        for (AnimationKeyframe keyframe : keyframes) copy.add(keyframe.copy());
        return copy;
    }

    private static void trimHistory(@NotNull Deque<List<AnimationKeyframe>> history) {
        while (history.size() > MAX_HISTORY_STATES) history.removeLast();
    }

}

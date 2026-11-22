package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * The working copy of the entries of a list variable while the user edits them in the {@link ListVariableEditorWindowBody}, with undo and
 * redo. It never touches the variable itself: the editor applies the entries once the user is done, so all edits of one editing session
 * become a single change of the variable.
 *
 * <p>Every edit remembers the selected index of the editor from right before the edit, so undoing it selects what was selected then.</p>
 */
public final class ListEntriesDraft {

    private static final int HISTORY_LIMIT = 100;

    @NotNull
    private final List<String> initialEntries;
    @NotNull
    private final ArrayList<String> entries;
    @NotNull
    private final List<String> entriesView;
    private final Deque<Snapshot> undoHistory = new ArrayDeque<>();
    private final Deque<Snapshot> redoHistory = new ArrayDeque<>();

    public ListEntriesDraft(@NotNull List<String> entries) {
        this.initialEntries = List.copyOf(entries);
        this.entries = new ArrayList<>(this.initialEntries);
        this.entriesView = Collections.unmodifiableList(this.entries);
    }

    /**
     * @return A read-only view of the entries, which follows all later edits.
     */
    @NotNull
    public List<String> getEntries() {
        return this.entriesView;
    }

    public int size() {
        return this.entries.size();
    }

    @NotNull
    public String get(int index) {
        return this.entries.get(index);
    }

    /**
     * @return If the entries differ from the ones the draft started with.
     */
    public boolean isChanged() {
        return !this.entries.equals(this.initialEntries);
    }

    /**
     * Inserts the entry, so it has the given index afterward.
     */
    public void insert(int index, @NotNull String entry, int selectedIndexBefore) {
        Objects.requireNonNull(entry);
        Objects.checkIndex(index, this.entries.size() + 1);
        this.pushUndo(selectedIndexBefore);
        this.entries.add(index, entry);
    }

    /**
     * @return If the entry changed.
     */
    public boolean set(int index, @NotNull String entry, int selectedIndexBefore) {
        Objects.requireNonNull(entry);
        if (this.entries.get(index).equals(entry)) return false;
        this.pushUndo(selectedIndexBefore);
        this.entries.set(index, entry);
        return true;
    }

    public void remove(int index, int selectedIndexBefore) {
        Objects.checkIndex(index, this.entries.size());
        this.pushUndo(selectedIndexBefore);
        this.entries.remove(index);
    }

    /**
     * Inserts a copy of the entry right below it.
     *
     * @return The index of the copy.
     */
    public int duplicate(int index, int selectedIndexBefore) {
        String entry = this.entries.get(index);
        this.insert(index + 1, entry, selectedIndexBefore);
        return index + 1;
    }

    /**
     * Moves the entry, so it has the index {@code to} afterward.
     *
     * @return If the entry moved.
     */
    public boolean move(int from, int to, int selectedIndexBefore) {
        Objects.checkIndex(from, this.entries.size());
        Objects.checkIndex(to, this.entries.size());
        if (from == to) return false;
        this.pushUndo(selectedIndexBefore);
        this.entries.add(to, this.entries.remove(from));
        return true;
    }

    /**
     * @return If the entries changed.
     */
    public boolean replaceAll(@NotNull List<String> newEntries, int selectedIndexBefore) {
        for (String entry : newEntries) Objects.requireNonNull(entry);
        if (this.entries.equals(newEntries)) return false;
        this.pushUndo(selectedIndexBefore);
        this.entries.clear();
        this.entries.addAll(newEntries);
        return true;
    }

    public boolean canUndo() {
        return !this.undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !this.redoHistory.isEmpty();
    }

    /**
     * Undoes the latest edit.
     *
     * @param selectedIndexNow The selected index of the editor, which redoing the edit selects again.
     * @return The state the entries went back to, with the index to select, or {@code null} if there is nothing to undo.
     */
    @Nullable
    public Snapshot undo(int selectedIndexNow) {
        return this.restore(this.undoHistory, this.redoHistory, selectedIndexNow);
    }

    /**
     * Redoes the latest undone edit.
     *
     * @param selectedIndexNow The selected index of the editor, which undoing the edit again selects.
     * @return The state the entries went to, with the index to select, or {@code null} if there is nothing to redo.
     */
    @Nullable
    public Snapshot redo(int selectedIndexNow) {
        return this.restore(this.redoHistory, this.undoHistory, selectedIndexNow);
    }

    @Nullable
    private Snapshot restore(@NotNull Deque<Snapshot> from, @NotNull Deque<Snapshot> to, int selectedIndexNow) {
        Snapshot snapshot = from.poll();
        if (snapshot == null) return null;
        to.push(new Snapshot(List.copyOf(this.entries), selectedIndexNow));
        trimHistory(to);
        this.entries.clear();
        this.entries.addAll(snapshot.entries());
        return snapshot;
    }

    private void pushUndo(int selectedIndexBefore) {
        this.undoHistory.push(new Snapshot(List.copyOf(this.entries), selectedIndexBefore));
        trimHistory(this.undoHistory);
        this.redoHistory.clear();
    }

    private static void trimHistory(@NotNull Deque<Snapshot> history) {
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    /**
     * @return If the entries can get edited as text with one entry per line, which only works when no entry contains a line break.
     */
    public static boolean canEditAsText(@NotNull List<String> entries) {
        for (String entry : entries) {
            if (VariableTextUtils.containsLineBreak(entry)) return false;
        }
        return true;
    }

    /**
     * @return The entries as text with one entry per line, see {@link #canEditAsText(List)}.
     */
    @NotNull
    public static String toText(@NotNull List<String> entries) {
        return String.join("\n", entries);
    }

    /**
     * Reads text with one entry per line. Empty lines are empty entries, and empty text is an empty list.
     *
     * @return The entries as a new mutable list.
     */
    @NotNull
    public static List<String> fromText(@NotNull String text) {
        return VariableLists.split(text, "", false, false);
    }

    /**
     * A state of the entries in the history.
     *
     * @param selectedIndex The index the editor had selected in that state, or -1 if nothing was selected.
     */
    public record Snapshot(@NotNull List<String> entries, int selectedIndex) {
    }

}

package de.keksuccino.fancymenu.customization.variables;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListEntriesDraftTest {

    @Test
    void editsChangeTheDraftButNeverTheListItStartedWith() {
        List<String> original = new ArrayList<>(List.of("apple", "banana"));
        ListEntriesDraft draft = new ListEntriesDraft(original);

        draft.insert(2, "cherry", -1);
        draft.set(0, "apricot", 0);
        original.add("changed later");

        assertAll(() -> assertEquals(List.of("apricot", "banana", "cherry"), draft.getEntries()), () -> assertEquals(List.of("apple", "banana", "changed later"), original),
                () -> assertTrue(draft.isChanged()));
    }

    @Test
    void entriesViewIsReadOnlyAndFollowsEdits() {
        ListEntriesDraft draft = new ListEntriesDraft(List.of("a"));
        List<String> view = draft.getEntries();

        draft.insert(0, "b", 0);

        assertAll(() -> assertEquals(List.of("b", "a"), view), () -> assertThrows(UnsupportedOperationException.class, () -> view.add("c")));
    }

    @Test
    void insertDuplicateRemoveAndMoveWorkAtTheGivenIndexes() {
        ListEntriesDraft draft = new ListEntriesDraft(List.of("a", "b", "c"));

        draft.insert(0, "first", -1);
        int copyIndex = draft.duplicate(2, 2);
        draft.remove(1, 1);

        assertAll(() -> assertEquals(3, copyIndex), () -> assertEquals(List.of("first", "b", "b", "c"), draft.getEntries()));

        assertTrue(draft.move(0, 3, 0));
        assertEquals(List.of("b", "b", "c", "first"), draft.getEntries());
        assertTrue(draft.move(3, 1, 3));
        assertEquals(List.of("b", "first", "b", "c"), draft.getEntries());
    }

    @Test
    void editsThatChangeNothingCreateNoUndoStep() {
        ListEntriesDraft draft = new ListEntriesDraft(List.of("a", "b"));

        assertAll(() -> assertFalse(draft.set(0, "a", 0)), () -> assertFalse(draft.move(1, 1, 1)), () -> assertFalse(draft.replaceAll(List.of("a", "b"), 0)),
                () -> assertFalse(draft.canUndo()), () -> assertFalse(draft.isChanged()));
    }

    @Test
    void invalidIndexesFailWithoutChangingAnything() {
        ListEntriesDraft draft = new ListEntriesDraft(List.of("a"));

        assertAll(() -> assertThrows(IndexOutOfBoundsException.class, () -> draft.insert(2, "x", 0)), () -> assertThrows(IndexOutOfBoundsException.class, () -> draft.remove(1, 0)),
                () -> assertThrows(IndexOutOfBoundsException.class, () -> draft.move(0, 1, 0)), () -> assertThrows(NullPointerException.class, () -> draft.insert(0, null, 0)));
        assertAll(() -> assertEquals(List.of("a"), draft.getEntries()), () -> assertFalse(draft.canUndo()));
    }

    @Test
    void undoAndRedoRestoreEntriesAndTheSelectionOfTheirTime() {
        ListEntriesDraft draft = new ListEntriesDraft(List.of("a", "b"));
        draft.insert(2, "c", 1);
        draft.remove(0, 2);

        ListEntriesDraft.Snapshot undone = draft.undo(1);
        assertNotNull(undone);
        assertAll(() -> assertEquals(List.of("a", "b", "c"), draft.getEntries()), () -> assertEquals(2, undone.selectedIndex()));

        ListEntriesDraft.Snapshot undoneAgain = draft.undo(2);
        assertNotNull(undoneAgain);
        assertAll(() -> assertEquals(List.of("a", "b"), draft.getEntries()), () -> assertEquals(1, undoneAgain.selectedIndex()), () -> assertNull(draft.undo(0)),
                () -> assertFalse(draft.isChanged()));

        ListEntriesDraft.Snapshot redone = draft.redo(1);
        assertNotNull(redone);
        assertAll(() -> assertEquals(List.of("a", "b", "c"), draft.getEntries()), () -> assertEquals(2, redone.selectedIndex()));
    }

    @Test
    void newEditsClearTheRedoHistory() {
        ListEntriesDraft draft = new ListEntriesDraft(List.of("a"));
        draft.insert(1, "b", 0);
        draft.undo(1);

        draft.insert(0, "x", 0);

        assertAll(() -> assertFalse(draft.canRedo()), () -> assertNull(draft.redo(0)), () -> assertEquals(List.of("x", "a"), draft.getEntries()));
    }

    @Test
    void historyKeepsOnlyTheLatestSteps() {
        ListEntriesDraft draft = new ListEntriesDraft(List.of());
        for (int i = 0; i < 150; i++) {
            draft.insert(i, "entry " + i, i - 1);
        }

        int undoSteps = 0;
        while (draft.undo(-1) != null) {
            undoSteps++;
        }

        assertAll(() -> assertEquals(50, draft.size()), () -> assertTrue(draft.isChanged()));
        assertEquals(100, undoSteps);
    }

    @Test
    void textWithOneEntryPerLineRoundTripsEntriesWithoutLineBreaks() {
        List<String> entries = List.of("apple", "", " spaced ", "[\"json\"]", "$$name");

        String text = ListEntriesDraft.toText(entries);

        assertAll(() -> assertEquals("apple\n\n spaced \n[\"json\"]\n$$name", text), () -> assertEquals(entries, ListEntriesDraft.fromText(text)),
                () -> assertTrue(ListEntriesDraft.canEditAsText(entries)));
    }

    @Test
    void textReadsEveryKindOfLineBreakAndEmptyTextIsAnEmptyList() {
        assertAll(() -> assertEquals(List.of("a", "b", "c", ""), ListEntriesDraft.fromText("a\r\nb\rc\n")), () -> assertEquals(List.of(), ListEntriesDraft.fromText("")),
                () -> assertEquals(List.of("", ""), ListEntriesDraft.fromText("\n")));
    }

    @Test
    void entriesWithLineBreaksCannotBeEditedAsText() {
        assertAll(() -> assertFalse(ListEntriesDraft.canEditAsText(List.of("a", "two\nlines"))), () -> assertFalse(ListEntriesDraft.canEditAsText(List.of("carriage\rreturn"))),
                () -> assertTrue(ListEntriesDraft.canEditAsText(List.of())));
    }

}

package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CursorTickSelectionTest {

    @Test
    void selectedCustomCursorIsAppliedThenNormalCursorIsRestored() {
        CursorTickSelection<FakeCursor> selection = new CursorTickSelection<>();
        FakeCursor cursor = new FakeCursor(7L);
        selection.setCustomCursor(cursor, FakeCursor::isUsable);

        assertEquals(7L, take(selection));
        assertEquals(100L, take(selection));
        assertEquals(CursorTickSelection.NO_CURSOR_CHANGE, take(selection));
    }

    @Test
    void staleRetirementCannotClearNewAllocationWithReusedNativeHandle() {
        CursorTickSelection<FakeCursor> selection = new CursorTickSelection<>();
        FakeCursor oldAllocation = new FakeCursor(42L);
        FakeCursor newAllocation = new FakeCursor(42L);
        selection.setCustomCursor(newAllocation, FakeCursor::isUsable);

        selection.retireCustomCursor(oldAllocation);

        assertEquals(42L, take(selection));
    }

    @Test
    void retiringSelectedAllocationCancelsPendingSelectionByIdentity() {
        CursorTickSelection<FakeCursor> selection = new CursorTickSelection<>();
        FakeCursor cursor = new FakeCursor(42L);
        selection.setCustomCursor(cursor, FakeCursor::isUsable);

        selection.retireCustomCursor(cursor);

        assertEquals(CursorTickSelection.NO_CURSOR_CHANGE, take(selection));
    }

    @Test
    void unusableAllocationIsNeverApplied() {
        CursorTickSelection<FakeCursor> selection = new CursorTickSelection<>();
        FakeCursor cursor = new FakeCursor(5L);
        selection.setCustomCursor(cursor, FakeCursor::isUsable);
        cursor.usable.set(false);

        assertEquals(CursorTickSelection.NO_CURSOR_CHANGE, take(selection));
    }

    @Test
    void rawStandardCursorPreservesExistingTickResetBehavior() {
        CursorTickSelection<FakeCursor> selection = new CursorTickSelection<>();
        selection.setRawCursor(9L);

        assertEquals(9L, take(selection));
        assertEquals(100L, take(selection));
        assertEquals(CursorTickSelection.NO_CURSOR_CHANGE, take(selection));
    }

    private static long take(CursorTickSelection<FakeCursor> selection) {
        return selection.takeCursorForTick(100L, FakeCursor::isUsable, cursor -> cursor.nativeHandle);
    }

    private static final class FakeCursor {

        private final long nativeHandle;
        private final AtomicBoolean usable = new AtomicBoolean(true);

        private FakeCursor(long nativeHandle) {
            this.nativeHandle = nativeHandle;
        }

        private boolean isUsable() {
            return this.usable.get();
        }

    }

}

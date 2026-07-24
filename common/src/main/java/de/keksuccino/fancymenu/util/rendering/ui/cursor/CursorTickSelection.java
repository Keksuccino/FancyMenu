package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Stores FancyMenu's next tick cursor request while retaining allocation identity for custom cursors.
 * Numeric GLFW handles are not identities because GLFW may reuse their values after destruction.
 */
final class CursorTickSelection<T> {

    static final long NO_CURSOR_CHANGE = -2L;

    private long rawCursor = NO_CURSOR_CHANGE;
    @Nullable
    private T customCursor;

    synchronized void setRawCursor(long cursor) {
        this.customCursor = null;
        this.rawCursor = cursor;
    }

    synchronized void setCustomCursor(@NotNull T cursor, @NotNull Predicate<T> usability) {
        if (!usability.test(cursor)) return;
        this.customCursor = cursor;
        this.rawCursor = NO_CURSOR_CHANGE;
    }

    synchronized void retireCustomCursor(@NotNull T cursor) {
        if (this.customCursor != cursor) return;
        this.customCursor = null;
        this.rawCursor = NO_CURSOR_CHANGE;
    }

    synchronized long takeCursorForTick(long normalCursor, @NotNull Predicate<T> usability, @NotNull ToLongFunction<T> nativeHandle) {
        if (this.customCursor != null) {
            T selectedCursor = this.customCursor;
            this.customCursor = null;
            if (usability.test(selectedCursor)) {
                this.rawCursor = -1L;
                return nativeHandle.applyAsLong(selectedCursor);
            }
            this.rawCursor = NO_CURSOR_CHANGE;
            return NO_CURSOR_CHANGE;
        }
        if (this.rawCursor != -1L && this.rawCursor != NO_CURSOR_CHANGE) {
            long selectedCursor = this.rawCursor;
            this.rawCursor = -1L;
            return selectedCursor;
        }
        if (this.rawCursor == -1L) {
            this.rawCursor = NO_CURSOR_CHANGE;
            return normalCursor;
        }
        return NO_CURSOR_CHANGE;
    }

    synchronized void clear() {
        this.customCursor = null;
        this.rawCursor = NO_CURSOR_CHANGE;
    }

}

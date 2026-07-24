package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Thread-safe identity registry that retires replaced and removed cursor allocations exactly once at the owner level. */
final class CursorRegistry<T> {

    private final Map<String, T> entries = new HashMap<>();
    private final Retirement<T> retirement;
    private boolean closed;

    CursorRegistry(@NotNull Retirement<T> retirement) {
        this.retirement = retirement;
    }

    boolean register(@NotNull String name, @NotNull T cursor, @NotNull Predicate<T> acceptanceCondition) {
        T cursorToRetire = null;
        boolean executeRetirement = false;
        boolean accepted;
        synchronized (this) {
            accepted = !this.closed && acceptanceCondition.test(cursor);
            if (!accepted) {
                cursorToRetire = cursor;
                executeRetirement = this.retirement.markRetired(cursor);
            } else {
                T replaced = this.entries.put(name, cursor);
                if (replaced != null && replaced != cursor) {
                    this.removeIdentityLocked(replaced);
                    // The replacement is visible before the old allocation is marked retired.
                    cursorToRetire = replaced;
                    executeRetirement = this.retirement.markRetired(replaced);
                }
            }
        }
        if (executeRetirement) this.retirement.executeRetirement(cursorToRetire);
        return accepted;
    }

    @Nullable
    synchronized T get(@NotNull String name) {
        return this.entries.get(name);
    }

    boolean unregister(@NotNull String name) {
        T removed;
        boolean executeRetirement = false;
        synchronized (this) {
            removed = this.entries.remove(name);
            if (removed != null) {
                this.removeIdentityLocked(removed);
                executeRetirement = this.retirement.markRetired(removed);
            }
        }
        if (executeRetirement) this.retirement.executeRetirement(removed);
        return removed != null;
    }

    boolean unregister(@NotNull String name, @NotNull T expectedCursor) {
        boolean removed;
        boolean executeRetirement;
        synchronized (this) {
            removed = this.entries.get(name) == expectedCursor;
            this.removeIdentityLocked(expectedCursor);
            // The stale owner still owns this allocation even when a newer cursor now occupies the same name.
            executeRetirement = this.retirement.markRetired(expectedCursor);
        }
        if (executeRetirement) this.retirement.executeRetirement(expectedCursor);
        return removed;
    }

    void retire(@NotNull T cursor) {
        boolean executeRetirement;
        synchronized (this) {
            this.removeIdentityLocked(cursor);
            executeRetirement = this.retirement.markRetired(cursor);
        }
        if (executeRetirement) this.retirement.executeRetirement(cursor);
    }

    void close() {
        Set<T> retirementsToExecute = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        synchronized (this) {
            if (this.closed) return;
            this.closed = true;
            Set<T> uniqueCursors = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            uniqueCursors.addAll(this.entries.values());
            this.entries.clear();
            uniqueCursors.forEach(cursor -> {
                if (this.retirement.markRetired(cursor)) retirementsToExecute.add(cursor);
            });
        }
        retirementsToExecute.forEach(this.retirement::executeRetirement);
    }

    private void removeIdentityLocked(@NotNull T cursor) {
        this.entries.entrySet().removeIf(entry -> entry.getValue() == cursor);
    }

    interface Retirement<T> {

        boolean markRetired(@NotNull T cursor);

        void executeRetirement(@NotNull T cursor);

    }

}

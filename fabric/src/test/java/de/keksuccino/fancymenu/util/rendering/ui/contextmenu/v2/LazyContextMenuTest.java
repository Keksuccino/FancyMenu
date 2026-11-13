package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Menus and entries in this test skip their constructors, because their field initializers read the game's font and FancyMenu's UI scale,
 * which don't exist in unit tests. Opening and clearing a menu need its search entry, which only the constructor builds, so they are left to tests in the game.
 */
class LazyContextMenuTest {

    private static final Unsafe UNSAFE = loadUnsafe();

    @Test
    void contentWaitsUntilSomethingNeedsIt() {
        AtomicInteger builds = new AtomicInteger();
        LazyContextMenu menu = allocateMenu(lazy -> builds.incrementAndGet());

        assertTrue(menu.isWaitingForContent());
        assertEquals(0, builds.get());
    }

    @Test
    void askingForTheEntriesBuildsTheContentOnce() {
        AtomicInteger builds = new AtomicInteger();
        ContextMenu.ContextMenuEntry<?> first = allocateEntry("first", false);
        ContextMenu.ContextMenuEntry<?> second = allocateEntry("second", false);
        LazyContextMenu menu = allocateMenu(lazy -> {
            builds.incrementAndGet();
            lazy.addEntry(first);
            lazy.addEntry(second);
        });

        assertEquals(List.of(first, second), menu.getEntries());
        assertEquals(List.of(first, second), menu.getEntries());
        assertEquals(1, builds.get());
        assertFalse(menu.isWaitingForContent());
    }

    @Test
    void askingForOneEntryBuildsTheContent() {
        ContextMenu.ContextMenuEntry<?> first = allocateEntry("first", false);
        ContextMenu.ContextMenuEntry<?> second = allocateEntry("second", false);

        assertSame(second, allocateMenu(lazy -> addAll(lazy, first, second)).getEntry("second"));
        assertTrue(allocateMenu(lazy -> addAll(lazy, first, second)).hasEntry("first"));
        assertEquals(1, allocateMenu(lazy -> addAll(lazy, first, second)).getEntryIndex("second"));
        assertNull(allocateMenu(lazy -> addAll(lazy, first, second)).getEntry("third"));
    }

    @Test
    void stackingBuildsTheContent() {
        ContextMenu.ContextMenuEntry<?> stackable = allocateEntry("stackable", true);
        ContextMenu.ContextMenuEntry<?> single = allocateEntry("single", false);
        LazyContextMenu menu = allocateMenu(lazy -> addAll(lazy, single, stackable));

        assertEquals(List.of(stackable), menu.getStackableEntries());
        assertFalse(menu.isWaitingForContent());
    }

    @Test
    void entriesAddedFromOutsideComeAfterTheContent() {
        ContextMenu.ContextMenuEntry<?> first = allocateEntry("first", false);
        ContextMenu.ContextMenuEntry<?> second = allocateEntry("second", false);
        ContextMenu.ContextMenuEntry<?> addedAtTheEnd = allocateEntry("added_at_the_end", false);
        ContextMenu.ContextMenuEntry<?> addedAfterFirst = allocateEntry("added_after_first", false);
        AtomicInteger builds = new AtomicInteger();
        LazyContextMenu menu = allocateMenu(lazy -> {
            builds.incrementAndGet();
            addAll(lazy, first, second);
        });

        menu.addEntry(addedAtTheEnd);
        menu.addEntryAfter("first", addedAfterFirst);

        assertEquals(List.of(first, addedAfterFirst, second, addedAtTheEnd), menu.getEntries());
        assertEquals(1, builds.get());
    }

    @Test
    void removingAnEntryOfTheContentBuildsItFirst() {
        ContextMenu.ContextMenuEntry<?> first = allocateEntry("first", false);
        ContextMenu.ContextMenuEntry<?> second = allocateEntry("second", false);
        LazyContextMenu menu = allocateMenu(lazy -> addAll(lazy, first, second));

        menu.removeEntry("first");

        assertEquals(List.of(second), menu.getEntries());
    }

    private static void addAll(@NotNull ContextMenu menu, @NotNull ContextMenu.ContextMenuEntry<?>... entries) {
        for (ContextMenu.ContextMenuEntry<?> entry : entries) menu.addEntry(entry);
    }

    @NotNull
    private static LazyContextMenu allocateMenu(@NotNull Consumer<ContextMenu> contentBuilder) {
        try {
            LazyContextMenu menu = (LazyContextMenu) UNSAFE.allocateInstance(LazyContextMenu.class);
            Field entries = ContextMenu.class.getDeclaredField("entries");
            entries.setAccessible(true);
            entries.set(menu, new ArrayList<>());
            menu.contentBuilder = contentBuilder;
            return menu;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @NotNull
    private static ContextMenu.ContextMenuEntry<?> allocateEntry(@NotNull String identifier, boolean stackable) {
        try {
            ContextMenu.SeparatorContextMenuEntry entry = (ContextMenu.SeparatorContextMenuEntry) UNSAFE.allocateInstance(ContextMenu.SeparatorContextMenuEntry.class);
            entry.identifier = identifier;
            entry.stackMeta = new ContextMenu.ContextMenuStackMeta();
            entry.visibleStateSuppliers = new ArrayList<>();
            entry.setStackable(stackable);
            return entry;
        } catch (InstantiationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Unsafe loadUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

}

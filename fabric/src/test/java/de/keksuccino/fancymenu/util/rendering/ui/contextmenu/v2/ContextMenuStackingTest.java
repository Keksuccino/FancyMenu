package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entries and menus in this test skip their constructors, because their field initializers read the game's font and FancyMenu's UI scale,
 * which don't exist in unit tests.
 */
class ContextMenuStackingTest {

    private static final Unsafe UNSAFE = loadUnsafe();

    @Test
    void stackAppliersReachTheSubMenuEntriesOfEveryStackedMenu() {
        List<String> applied = new ArrayList<>();
        List<ContextMenu.ContextMenuEntry<?>> copies = new ArrayList<>();
        for (String holder : List.of("first", "second", "third")) {
            ContextMenu.SubMenuContextMenuEntry copy = allocateEntry(ContextMenu.SubMenuContextMenuEntry.class);
            copy.setStackApplier((stackEntry, value) -> applied.add(holder + "=" + value));
            copies.add(copy);
        }

        ContextMenu.ContextMenuEntry<?> first = ContextMenu.linkStackCopies(copies, allocate(ContextMenu.class));
        new TestBuilder().applyStackAppliers(first, true);

        assertSame(copies.get(0), first);
        assertEquals(List.of("first=true", "second=true", "third=true"), applied);
    }

    @Test
    void stackValuesOfSubMenuEntriesCoverEveryStackedMenu() {
        TestBuilder builder = new TestBuilder();

        ContextMenu.ContextMenuEntry<?> sameValues = linkSubMenuCopiesWithValues("enabled", "enabled", "enabled");
        ContextMenu.ContextMenuEntry<?> lastValueDiffers = linkSubMenuCopiesWithValues("enabled", "enabled", "disabled");

        ContextMenuBuilder.StackValue<String> common = builder.resolveStackValue(sameValues);
        ContextMenuBuilder.StackValue<String> mixed = builder.resolveStackValue(lastValueDiffers);
        assertTrue(common.hasCommonValue());
        assertEquals("enabled", common.getValue());
        assertTrue(mixed.isMixed());
        assertFalse(mixed.hasCommonValue());
    }

    @Test
    void linkedCopiesFormOneChainThatSharesItsStackProperties() {
        ContextMenu stacked = allocate(ContextMenu.class);
        List<ContextMenu.ContextMenuEntry<?>> copies = List.of(allocateEntry(ContextMenu.SubMenuContextMenuEntry.class), allocateEntry(ContextMenu.SubMenuContextMenuEntry.class), allocateEntry(ContextMenu.SubMenuContextMenuEntry.class));

        ContextMenu.linkStackCopies(copies, stacked);

        for (int i = 0; i < copies.size(); i++) {
            ContextMenu.ContextMenuStackMeta meta = copies.get(i).getStackMeta();
            assertTrue(meta.isPartOfStack());
            assertEquals(i == 0, meta.isFirstInStack());
            assertEquals(i == copies.size() - 1, meta.isLastInStack());
            assertSame((i < copies.size() - 1) ? copies.get(i + 1) : null, meta.getNextInStack());
            assertSame(copies.get(0).getStackMeta().getProperties(), meta.getProperties());
            assertSame(stacked, copies.get(i).getParent());
        }
    }

    @Test
    void singleCopyIsBothFirstAndLastInItsStack() {
        ContextMenu.SubMenuContextMenuEntry copy = allocateEntry(ContextMenu.SubMenuContextMenuEntry.class);

        ContextMenu.linkStackCopies(List.of(copy), allocate(ContextMenu.class));

        assertTrue(copy.getStackMeta().isFirstInStack());
        assertTrue(copy.getStackMeta().isLastInStack());
        assertNull(copy.getStackMeta().getNextInStack());
    }

    @NotNull
    private static ContextMenu.ContextMenuEntry<?> linkSubMenuCopiesWithValues(@NotNull String... values) {
        List<ContextMenu.ContextMenuEntry<?>> copies = new ArrayList<>();
        for (String value : values) {
            ContextMenu.SubMenuContextMenuEntry copy = allocateEntry(ContextMenu.SubMenuContextMenuEntry.class);
            copy.setStackValueSupplier(stackEntry -> value);
            copies.add(copy);
        }
        return ContextMenu.linkStackCopies(copies, allocate(ContextMenu.class));
    }

    @NotNull
    private static <T extends ContextMenu.ContextMenuEntry<?>> T allocateEntry(@NotNull Class<T> type) {
        T entry = allocate(type);
        entry.stackMeta = new ContextMenu.ContextMenuStackMeta();
        return entry;
    }

    @NotNull
    private static <T> T allocate(@NotNull Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @NotNull
    private static Unsafe loadUnsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class TestBuilder implements ContextMenuBuilder<TestBuilder> {

        private final List<ContextMenuScreenOpenProcessor> processors = new ArrayList<>();

        @Override
        public @Nullable Screen getContextMenuCallbackScreen() {
            return null;
        }

        @Override
        public @NotNull TestBuilder self() {
            return this;
        }

        @Override
        public void saveSnapshot() {
        }

        @Override
        public void saveSnapshot(@NotNull Object snapshot) {
        }

        @Override
        public @Nullable Object createSnapshot() {
            return null;
        }

        @Override
        public @NotNull List<TestBuilder> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<TestBuilder, Boolean> filter) {
            return List.of(this);
        }

        @Override
        public @NotNull List<ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessorList() {
            return this.processors;
        }

    }

}

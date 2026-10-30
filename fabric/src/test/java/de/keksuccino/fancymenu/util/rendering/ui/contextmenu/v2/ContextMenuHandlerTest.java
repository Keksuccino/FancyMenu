package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextMenuHandlerTest {

    private static final List<String> ENTRY_PATH = List.of("first_sub_menu", "second_sub_menu");

    @AfterEach
    void clearHandler() {
        ContextMenuHandler.INSTANCE.removeCurrent();
    }

    @Test
    void setAndOpenForwardsEntryPath() throws ReflectiveOperationException {
        RecordingContextMenu menu = RecordingContextMenu.create();

        ContextMenuHandler.INSTANCE.setAndOpen(menu, 12.0F, 34.0F, ENTRY_PATH);

        assertEquals(1, menu.openCalls);
        assertFalse(menu.openedAtMouse);
        assertEquals(12.0F, menu.openX);
        assertEquals(34.0F, menu.openY);
        assertEquals(ENTRY_PATH, menu.openEntryPath);
        assertSame(menu, ContextMenuHandler.INSTANCE.getCurrent());
    }

    @Test
    void setAndOpenAtMouseForwardsEntryPath() throws ReflectiveOperationException {
        RecordingContextMenu menu = RecordingContextMenu.create();

        ContextMenuHandler.INSTANCE.setAndOpenAtMouse(menu, ENTRY_PATH);

        assertEquals(1, menu.openCalls);
        assertTrue(menu.openedAtMouse);
        assertEquals(ENTRY_PATH, menu.openEntryPath);
        assertSame(menu, ContextMenuHandler.INSTANCE.getCurrent());
    }

    @Test
    void overloadsWithoutEntryPathOpenOnlyTheRootMenu() throws ReflectiveOperationException {
        RecordingContextMenu atPosition = RecordingContextMenu.create();
        RecordingContextMenu atMouse = RecordingContextMenu.create();

        ContextMenuHandler.INSTANCE.setAndOpen(atPosition, 1.0F, 2.0F);
        ContextMenuHandler.INSTANCE.setAndOpenAtMouse(atMouse);

        assertEquals(1, atPosition.openCalls);
        assertNull(atPosition.openEntryPath);
        assertEquals(1, atMouse.openCalls);
        assertNull(atMouse.openEntryPath);
    }

    @Test
    void openingAnotherMenuClosesThePreviousMenuChain() throws ReflectiveOperationException {
        RecordingContextMenu previous = RecordingContextMenu.create();
        RecordingContextMenu next = RecordingContextMenu.create();

        ContextMenuHandler.INSTANCE.setAndOpen(previous, 0.0F, 0.0F, ENTRY_PATH);
        ContextMenuHandler.INSTANCE.setAndOpenAtMouse(next, ENTRY_PATH);

        assertEquals(1, previous.closeChainCalls);
        assertEquals(0, next.closeChainCalls);
        assertSame(next, ContextMenuHandler.INSTANCE.getCurrent());
        assertEquals(1, ContextMenuHandler.INSTANCE.children().size());
    }

    /**
     * Records how the handler opens and closes it. Instances skip the {@link ContextMenu} constructor, because its field
     * initializers read the UI scale from FancyMenu's options and the game window, which don't exist in unit tests.
     */
    private static class RecordingContextMenu extends ContextMenu {

        private int openCalls;
        private boolean openedAtMouse;
        private float openX;
        private float openY;
        @Nullable
        private List<String> openEntryPath;
        private int closeChainCalls;

        private static RecordingContextMenu create() throws ReflectiveOperationException {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (RecordingContextMenu) ((Unsafe) unsafeField.get(null)).allocateInstance(RecordingContextMenu.class);
        }

        @Override
        public ContextMenu openMenuAt(float x, float y, @Nullable List<String> entryPath) {
            this.openCalls++;
            this.openX = x;
            this.openY = y;
            this.openEntryPath = entryPath;
            return this;
        }

        @Override
        public ContextMenu openMenuAtMouse(@Nullable List<String> entryPath) {
            this.openCalls++;
            this.openedAtMouse = true;
            this.openEntryPath = entryPath;
            return this;
        }

        @Override
        public ContextMenu closeMenuChain() {
            this.closeChainCalls++;
            return this;
        }

    }

}

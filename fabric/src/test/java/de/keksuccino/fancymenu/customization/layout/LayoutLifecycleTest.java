package de.keksuccino.fancymenu.customization.layout;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutLifecycleTest {

    @Test
    void destroyTerminatesEveryBackgroundExactlyOnce() {
        Layout layout = new Layout("test-screen");
        TrackingBackground first = new TrackingBackground();
        TrackingBackground second = new TrackingBackground();
        layout.menuBackgrounds.add(first);
        layout.menuBackgrounds.add(second);

        layout.destroy();
        layout.destroy();

        assertEquals(1, first.destroyCalls);
        assertEquals(1, second.destroyCalls);
    }

    private static final class TrackingBackground extends MenuBackground<TrackingBackground> {

        private int destroyCalls;

        private TrackingBackground() {
            super(null);
        }

        @Override
        protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

        @Override
        public void onDestroyBackground() {
            this.destroyCalls++;
        }
    }
}

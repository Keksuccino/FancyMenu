package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * {@link ContainerEventHandler}, but fires all events for every child instead of just focused/hovered ones in some cases.
 */
public interface FocuslessEventHandler extends ContainerEventHandler {

    @Override
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.setDragging(false);
        for(GuiEventListener child : this.children()) {
            if (child.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    default boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {
        if (this.isDragging() && (button == 0)) {
            for (GuiEventListener child : this.children()) {
                if (child.mouseDragged(mouseX, mouseY, button, $$3, $$4)) return true;
            }
        }
        return false;
    }

    @Override
    default boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        for(GuiEventListener child : this.children()) {
            if (child.mouseScrolled(mouseX, mouseY, scrollDelta)) return true;
        }
        return false;
    }

    @Override
    default boolean keyPressed(int keycode, int scancode, int modifiers) {
        for(GuiEventListener child : this.children()) {
            if (child.keyPressed(keycode, scancode, modifiers)) return true;
        }
        return false;
    }

    @Override
    default boolean keyReleased(int keycode, int scancode, int modifiers) {
        for(GuiEventListener child : this.children()) {
            if (child.keyReleased(keycode, scancode, modifiers)) return true;
        }
        return false;
    }

    @Override
    default boolean charTyped(char c, int $$1) {
        for(GuiEventListener child : this.children()) {
            if (child.charTyped(c, $$1)) return true;
        }
        return false;
    }

    @Nullable
    @Override
    default GuiEventListener getFocused() {
        return null;
    }

    @Override
    default void setFocused(@Nullable GuiEventListener var1) {
    }

    @Override
    default void setFocused(boolean $$0) {
    }

    @Override
    @NotNull
    List<GuiEventListener> children();

}

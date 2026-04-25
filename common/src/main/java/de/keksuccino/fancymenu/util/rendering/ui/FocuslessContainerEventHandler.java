package de.keksuccino.fancymenu.util.rendering.ui;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.jetbrains.annotations.Nullable;

/**
 * {@link ContainerEventHandler}, but fires all events for every child instead of just focused/hovered ones in some cases.
 */
public interface FocuslessContainerEventHandler extends ContainerEventHandler, FancyMenuUiComponent {

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), dragX, dragY);
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
    }

    default boolean charTyped(char codePoint, int modifiers) {
        return this.charTyped(new CharacterEvent(codePoint, modifiers));
    }

    @Override
    default boolean mouseReleased(MouseButtonEvent event) {
        this.setDragging(false);
        for(GuiEventListener child : this.children()) {
            if (child.mouseReleased(event)) return true;
        }
        return false;
    }

    @Override
    default boolean mouseDragged(MouseButtonEvent event, double $$3, double $$4) {
        if (this.isDragging() && (event.button() == 0)) {
            for (GuiEventListener child : this.children()) {
                if (child.mouseDragged(event, $$3, $$4)) return true;
            }
        }
        return false;
    }

    @Override
    default boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        for(GuiEventListener child : this.children()) {
            if (child.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        }
        return false;
    }

    @Override
    default boolean keyPressed(KeyEvent event) {
        for(GuiEventListener child : this.children()) {
            if (child.keyPressed(event)) return true;
        }
        return false;
    }

    @Override
    default boolean keyReleased(KeyEvent event) {
        for(GuiEventListener child : this.children()) {
            if (child.keyReleased(event)) return true;
        }
        return false;
    }

    @Override
    default boolean charTyped(CharacterEvent event) {
        for(GuiEventListener child : this.children()) {
            if (child.charTyped(event)) return true;
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

}

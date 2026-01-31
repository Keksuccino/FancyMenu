package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import de.keksuccino.fancymenu.util.rendering.ui.Tickable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContextMenuHandler extends AbstractContainerEventHandler implements Renderable, Tickable {

    public static final ContextMenuHandler INSTANCE = new ContextMenuHandler();

    private final List<GuiEventListener> children = new ArrayList<>();

    private ContextMenuHandler() {
    }

    public void setAndOpen(@NotNull ContextMenu menu, float x, float y) {
        this.setAndOpen(menu, x, y, null);
    }

    public void setAndOpen(@NotNull ContextMenu menu, float x, float y, @Nullable List<String> entryPath) {
        removeCurrent();
        this.children.add(menu);
        menu.openMenuAt(x, y);
    }

    public void setAndOpenAtMouse(@NotNull ContextMenu menu) {
        this.setAndOpenAtMouse(menu, null);
    }

    public void setAndOpenAtMouse(@NotNull ContextMenu menu, @Nullable List<String> entryPath) {
        removeCurrent();
        this.children.add(menu);
        menu.openMenuAtMouse();
    }

    public void removeCurrent() {
        ContextMenu current = this.getCurrent();
        if (current != null) current.closeMenuChain();
        this.children.clear();
    }

    @Nullable
    public ContextMenu getCurrent() {
        if (!this.children.isEmpty()) return (ContextMenu) this.children.get(0);
        return null;
    }

    @Override
    public void tick() {
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            current.render(graphics, mouseX, mouseY, partial);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            if (current.getMenuUnderCursor(mouseX, mouseY) == null) {
                removeCurrent();
                return false;
            }
            return current.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        ContextMenu current = this.getCurrent();
        if (current != null) {
            return current.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        ContextMenu current = this.getCurrent();
        if (current != null && current.isMouseOverMenu(mouseX, mouseY)) {
            return Optional.of(current);
        }
        return Optional.empty();
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

}

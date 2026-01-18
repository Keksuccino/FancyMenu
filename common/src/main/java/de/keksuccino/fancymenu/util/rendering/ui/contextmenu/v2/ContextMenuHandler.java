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
    public @NotNull List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public boolean isFocused() {
        return false;
    }

}

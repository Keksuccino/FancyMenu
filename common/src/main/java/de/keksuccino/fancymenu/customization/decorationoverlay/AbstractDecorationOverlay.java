package de.keksuccino.fancymenu.customization.decorationoverlay;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractDecorationOverlay implements Renderable, ContainerEventHandler, NarratableEntry {

    @NotNull
    private String instanceIdentifier = ScreenCustomization.generateUniqueIdentifier();
    @ApiStatus.Internal
    public boolean showOverlay = false;
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<Renderable> renderables = new ArrayList<>();

    @NotNull
    public String getInstanceIdentifier() {
        return this.instanceIdentifier;
    }

    public void setInstanceIdentifier(@NotNull String instanceIdentifier) {
        this.instanceIdentifier = instanceIdentifier;
    }

    @Override
    public abstract void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    public final void _render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.render(graphics, mouseX, mouseY, partial);

        this.renderables.forEach(renderable -> renderable.render(graphics, mouseX, mouseY, partial));

    }

    /**
     * Gets called after the current {@link Screen} got initialized or resized.<br>
     * At this point all its widgets should be available.
     */
    public void onScreenInitializedOrResized(@NotNull Screen screen) {
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.children;
    }

    protected void addChild(@NotNull GuiEventListener child) {
        this.children.add(Objects.requireNonNull(child));
    }

    protected <C extends Renderable & GuiEventListener> void addRenderableChild(@NotNull C child) {
        this.children.add(Objects.requireNonNull(child));
        this.renderables.add(child);
    }

    protected void removeChild(@NotNull GuiEventListener child) {
        this.children.remove(child);
        if (child instanceof Renderable r) this.renderables.remove(r);
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    @Override
    public void setDragging(boolean isDragging) {
        // do nothing
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return null;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        // do nothing
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        // do nothing
    }

    @Nullable
    protected static Screen getScreen() {
        return Minecraft.getInstance().screen;
    }

    protected static int getScreenWidth() {
        var s = getScreen();
        return (s != null) ? s.width : 1;
    }

    protected static int getScreenHeight() {
        var s = getScreen();
        return (s != null) ? s.height : 1;
    }

    protected static boolean isEditor() {
        return (getScreen() instanceof LayoutEditorScreen);
    }

}

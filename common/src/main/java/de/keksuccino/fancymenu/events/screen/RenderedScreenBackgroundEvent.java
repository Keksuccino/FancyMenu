package de.keksuccino.fancymenu.events.screen;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * Fired after rendering the screen background.<br>
 * This event only gets fired when the screen calls its background render method.
 **/
public class RenderedScreenBackgroundEvent extends EventBase {

    private final Screen screen;
    private final GuiGraphics graphics;
    private final int mouseX;
    private final int mouseY;
    private final float partial;

    public RenderedScreenBackgroundEvent(@NotNull Screen screen, @NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.screen = Objects.requireNonNull(screen);
        this.graphics = Objects.requireNonNull(graphics);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partial = partial;
    }

    @NotNull
    public Screen getScreen() {
        return this.screen;
    }

    @NotNull
    public GuiGraphics getGraphics() {
        return graphics;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public float getPartial() {
        return partial;
    }

    public <T extends GuiEventListener & NarratableEntry> void addWidget(T widget) {
        this.getWidgets().add(widget);
        this.getNarratables().add(widget);
    }

    public <T extends GuiEventListener & NarratableEntry & Renderable> void addRenderableWidget(T widget) {
        this.addWidget(widget);
        this.getRenderables().add(widget);
    }

    public List<GuiEventListener> getWidgets() {
        return ((IMixinScreen)this.getScreen()).getChildrenFancyMenu();
    }

    public List<Renderable> getRenderables() {
        return ((IMixinScreen)this.getScreen()).getRenderablesFancyMenu();
    }

    public List<NarratableEntry> getNarratables() {
        return ((IMixinScreen)this.getScreen()).getNarratablesFancyMenu();
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

}

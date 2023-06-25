package de.keksuccino.fancymenu.events.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * Fired after rendering the screen background.<br>
 * This event only gets fired when the screen calls its background render method.
 **/
public class RenderedScreenBackgroundEvent extends EventBase {

    private final Screen screen;
    private final PoseStack poseStack;

    public RenderedScreenBackgroundEvent(Screen screen, PoseStack poseStack) {
        this.screen = screen;
        this.poseStack = poseStack;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
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

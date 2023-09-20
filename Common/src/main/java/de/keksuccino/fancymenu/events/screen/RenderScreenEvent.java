package de.keksuccino.fancymenu.events.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinScreen;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public class RenderScreenEvent extends EventBase {

    private final Screen screen;
    private final PoseStack poseStack;
    private final int mouseX;
    private final int mouseY;
    private final float partial;

    protected RenderScreenEvent(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partial) {
        this.screen = screen;
        this.poseStack = poseStack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partial = partial;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
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

    public static class Pre extends RenderScreenEvent {

        public Pre(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partial) {
            super(screen, poseStack, mouseX, mouseY, partial);
        }

    }

    public static class Post extends RenderScreenEvent {

        public Post(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partial) {
            super(screen, poseStack, mouseX, mouseY, partial);
        }

    }

}

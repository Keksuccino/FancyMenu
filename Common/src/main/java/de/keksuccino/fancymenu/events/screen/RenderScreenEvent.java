package de.keksuccino.fancymenu.events.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.events.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

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

    @Override
    public boolean isCancelable() {
        return false;
    }

    public static class Pre extends RenderScreenEvent {

        public Pre(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partial) {
            super(screen, poseStack, mouseX, mouseY, partial);
        }

        @Override
        public boolean isCancelable() {
            return true;
        }

    }

    public static class Post extends RenderScreenEvent {

        public Post(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partial) {
            super(screen, poseStack, mouseX, mouseY, partial);
        }

    }

}

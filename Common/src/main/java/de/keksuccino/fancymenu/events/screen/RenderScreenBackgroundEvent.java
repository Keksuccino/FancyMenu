package de.keksuccino.fancymenu.events.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.events.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

public class RenderScreenBackgroundEvent extends EventBase {

    private final Screen screen;
    private final PoseStack poseStack;

    protected RenderScreenBackgroundEvent(Screen screen, PoseStack poseStack) {
        this.screen = screen;
        this.poseStack = poseStack;
    }

    public Screen getScreen() {
        return this.screen;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    @Override
    public boolean isCancelable() {
        return false;
    }

    public static class Post extends RenderScreenBackgroundEvent {

        public Post(Screen screen, PoseStack poseStack) {
            super(screen, poseStack);
        }

    }

}

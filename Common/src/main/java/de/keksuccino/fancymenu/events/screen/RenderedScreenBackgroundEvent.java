package de.keksuccino.fancymenu.events.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.screens.Screen;

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

    @Override
    public boolean isCancelable() {
        return false;
    }

}

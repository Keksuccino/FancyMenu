package de.keksuccino.fancymenu.utils;

import de.keksuccino.fancymenu.mixin.mixins.client.IMixinMinecraft;
import net.minecraft.client.Minecraft;

public class RenderingUtils {

    public static float getPartialTick() {
        return Minecraft.getInstance().isPaused() ? ((IMixinMinecraft)Minecraft.getInstance()).getPausePartialTickFancyMenu() : Minecraft.getInstance().getFrameTime();
    }

}

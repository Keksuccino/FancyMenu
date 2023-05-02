package de.keksuccino.fancymenu.utils;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinMinecraft;
import net.minecraft.client.Minecraft;

public class RenderUtils {

    public static float getPartialTick() {
        return Minecraft.getInstance().isPaused() ? ((IMixinMinecraft)Minecraft.getInstance()).getPausePartialTickFancyMenu() : Minecraft.getInstance().getFrameTime();
    }

    public static void resetGuiScale() {
        Window m = Minecraft.getInstance().getWindow();
        m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
    }

}

package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMixinMinecraft {

    @Accessor("fps") static int getFpsFancyMenu() {
        return 0;
    }

    @Invoker("openChatScreen") void openChatScreenFancyMenu(String msg);

    @Accessor("pausePartialTick") float getPausePartialTickFancyMenu();

    @Accessor("reloadStateTracker") ResourceLoadStateTracker getReloadStateTrackerFancyMenu();

}

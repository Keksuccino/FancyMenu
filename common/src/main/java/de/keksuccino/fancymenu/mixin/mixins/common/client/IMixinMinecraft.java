package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface IMixinMinecraft {

    @Accessor("reloadStateTracker") ResourceLoadStateTracker getReloadStateTrackerFancyMenu();

}

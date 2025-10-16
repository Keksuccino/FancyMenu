package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMixinMinecraft {

    @Invoker("openChatScreen") void openChatScreenFancyMenu(ChatComponent.ChatMethod chatMethod);

    @Accessor("reloadStateTracker") ResourceLoadStateTracker getReloadStateTrackerFancyMenu();

}

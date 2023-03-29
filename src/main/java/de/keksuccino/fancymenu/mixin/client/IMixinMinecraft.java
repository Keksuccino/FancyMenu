package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMixinMinecraft {

    @Invoker("openChatScreen") public void openChatScreenFancyMenu(String msg);

}

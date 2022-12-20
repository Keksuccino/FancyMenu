
package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMixinMinecraft {

    @Invoker("openChatScreen") public void openChatScreenFancyMenu(String msg);

    @Accessor("musicManager") public MusicTicker getMusicManagerFancyMenu();

    @Accessor("musicManager") public void setMusicManagerFancyMenu(MusicTicker manager);

}

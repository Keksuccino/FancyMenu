package de.keksuccino.fancymenu.mixin.client;

import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TitleScreen.class)
public interface IMixinTitleScreen {

    @Accessor("fading") boolean getFadingFancyMenu();

    @Accessor("realmsNotificationsScreen") RealmsNotificationsScreen getRealmsNotificationsScreenFancyMenu();

}

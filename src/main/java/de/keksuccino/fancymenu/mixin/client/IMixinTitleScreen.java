package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TitleScreen.class)
public interface IMixinTitleScreen {

    @Accessor("realmsNotificationsScreen") public Screen getRealmsNotificationScreenFancyMenu();

}

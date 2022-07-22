package de.keksuccino.fancymenu.mixin.client;

import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MainMenuScreen.class)
public interface IMixinMainMenuScreen {

    @Accessor("realmsNotificationsScreen") public Screen getRealmsNotificationsScreenFancyMenu();

    @Accessor("realmsNotificationsScreen") public void setRealmsNotificationsScreenFancyMenu(Screen s);

    @Accessor("copyrightX") public int getCopyrightXFancyMenu();

    @Accessor("copyrightX") public void setCopyrightXFancyMenu(int i);

    @Accessor("fading") public boolean getFadingFancyMenu();

    @Accessor("fading") public void setFadingFancyMenu(boolean b);

}

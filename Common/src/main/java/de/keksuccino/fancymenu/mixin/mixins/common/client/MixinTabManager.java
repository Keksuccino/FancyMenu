package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabManager.class)
public class MixinTabManager {

    @Inject(method = "setCurrentTab", at = @At("RETURN"))
    private void onSetCurrentTabFancyMenu(Tab tab, boolean playPressSound, CallbackInfo info) {
        ScreenCustomization.reInitCurrentScreen();
    }

}

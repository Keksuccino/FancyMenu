package de.keksuccino.drippyloadingscreen.mixin.mixins.client;

import de.keksuccino.drippyloadingscreen.customization.DrippyOverlayScreen;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MenuCustomization.class)
public class MixinMenuCustomization {

    @Inject(at = @At("HEAD"), method = "isValidScreen", cancellable = true, remap = false)
    private static void onIsValidScreen(Screen screen, CallbackInfoReturnable<Boolean> info) {
        if ((screen != null) && (screen instanceof DrippyOverlayScreen)) {
            info.setReturnValue(true);
        }
    }

}

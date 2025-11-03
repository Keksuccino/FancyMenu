package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.helpers.WorldSessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReceivingLevelScreen.class)
public abstract class MixinLevelLoadStatusManager {

    @Unique private boolean worldEnteredNotified_FancyMenu;

    @Inject(method = "onClose", at = @At("RETURN"))
    private void after_onClose_FancyMenu(CallbackInfo info) {
        if (this.worldEnteredNotified_FancyMenu) {
            return;
        }
        if (WorldSessionTracker.hasPendingEntry()) {
            this.worldEnteredNotified_FancyMenu = true;
            WorldSessionTracker.handleWorldEntered(Minecraft.getInstance());
        }
    }

}
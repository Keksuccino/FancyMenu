package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.WorldSessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ReceivingLevelScreen.class)
public abstract class MixinLevelLoadStatusManager {

    @Unique private boolean worldEnteredNotified_FancyMenu;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ReceivingLevelScreen;onClose()V"))
    private void wrap_onClose_in_tick_FancyMenu(ReceivingLevelScreen instance, Operation<Void> original) {
        original.call(instance);
        if (this.worldEnteredNotified_FancyMenu) {
            return;
        }
        if (WorldSessionTracker.hasPendingEntry()) {
            this.worldEnteredNotified_FancyMenu = true;
            WorldSessionTracker.handleWorldEntered(Minecraft.getInstance());
        }
    }

}

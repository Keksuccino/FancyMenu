package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.helpers.WorldSessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.LevelLoadStatusManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadStatusManager.class)
public abstract class MixinLevelLoadStatusManager {

    @Unique private boolean worldEnteredNotified_FancyMenu;

    @Shadow public abstract boolean levelReady();

    @Inject(method = "tick", at = @At("RETURN"))
    private void afterTick_FancyMenu(CallbackInfo info) {
        if (this.worldEnteredNotified_FancyMenu) {
            return;
        }
        if (this.levelReady() && WorldSessionTracker.hasPendingEntry()) {
            this.worldEnteredNotified_FancyMenu = true;
            WorldSessionTracker.handleWorldEntered(Minecraft.getInstance());
        }
    }

}
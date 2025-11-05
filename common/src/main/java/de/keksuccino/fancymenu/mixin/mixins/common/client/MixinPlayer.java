package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayer {

    /** @reason Fire FancyMenu listener when the local player jumps. */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void after_jumpFromGround_FancyMenu(CallbackInfo info) {
        if ((Object)this instanceof LocalPlayer) {
            Listeners.ON_JUMP.onJump();
        }
    }

}

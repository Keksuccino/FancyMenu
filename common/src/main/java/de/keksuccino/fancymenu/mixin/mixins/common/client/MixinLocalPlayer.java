package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        LocalPlayer self = (LocalPlayer)(Object)this;
        if (!(self.level() instanceof ClientLevel clientLevel)) {
            Listeners.ON_ENTER_BIOME.onBiomeChanged(null);
            return;
        }

        if (!clientLevel.hasChunkAt(self.getBlockX(), self.getBlockZ())) {
            Listeners.ON_ENTER_BIOME.onBiomeChanged(null);
            return;
        }

        Holder<Biome> biomeHolder = clientLevel.getBiome(self.blockPosition());
        ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);
        Listeners.ON_ENTER_BIOME.onBiomeChanged(biomeKey);
    }

}

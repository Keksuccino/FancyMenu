package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Unique
    private ResourceKey<Biome> lastBiomeKey_FancyMenu;

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        LocalPlayer self = (LocalPlayer)(Object)this;

        ResourceKey<Biome> currentBiomeKey = null;
        if (self.level() instanceof ClientLevel clientLevel && clientLevel.hasChunkAt(self.getBlockX(), self.getBlockZ())) {
            Holder<Biome> biomeHolder = clientLevel.getBiome(self.blockPosition());
            currentBiomeKey = biomeHolder.unwrapKey().orElse(null);
        }

        if (Objects.equals(this.lastBiomeKey_FancyMenu, currentBiomeKey)) {
            if (currentBiomeKey == null) {
                Listeners.ON_ENTER_BIOME.onBiomeChanged(null);
            }
            return;
        }

        if (this.lastBiomeKey_FancyMenu != null) {
            Listeners.ON_LEAVE_BIOME.onBiomeLeft(this.lastBiomeKey_FancyMenu);
        }

        this.lastBiomeKey_FancyMenu = currentBiomeKey;
        Listeners.ON_ENTER_BIOME.onBiomeChanged(currentBiomeKey);
    }

}

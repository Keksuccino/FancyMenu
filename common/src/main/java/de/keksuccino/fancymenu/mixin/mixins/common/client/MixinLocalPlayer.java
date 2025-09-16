package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FluidState;
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

    @Unique
    private boolean lastSwimmingState_FancyMenu;

    @Unique
    private boolean swimmingStateInitialized_FancyMenu;

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        LocalPlayer self = (LocalPlayer)(Object)this;

        this.updateStartSwimmingListener_FancyMenu(self);

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

    @Unique
    private void updateStartSwimmingListener_FancyMenu(LocalPlayer self) {
        boolean isSwimming = self.isSwimming();

        if (!this.swimmingStateInitialized_FancyMenu) {
            this.swimmingStateInitialized_FancyMenu = true;
            this.lastSwimmingState_FancyMenu = isSwimming;
            return;
        }

        if (!this.lastSwimmingState_FancyMenu && isSwimming) {
            String fluidKey = null;
            if (self.level() instanceof ClientLevel clientLevel && clientLevel.hasChunkAt(self.getBlockX(), self.getBlockZ())) {
                FluidState fluidState = clientLevel.getFluidState(self.blockPosition());
                if (!fluidState.isEmpty()) {
                    ResourceLocation fluidLocation = BuiltInRegistries.FLUID.getKey(fluidState.getType());
                    if (fluidLocation != null) {
                        fluidKey = fluidLocation.toString();
                    }
                }
            }
            Listeners.ON_START_SWIMMING.onStartSwimming(fluidKey);
        }

        this.lastSwimmingState_FancyMenu = isSwimming;
    }

}

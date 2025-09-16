package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.listener.listeners.helpers.FluidContactInfo;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
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

    @Unique
    private String lastSwimmingFluidKey_FancyMenu;

    @Unique
    private boolean lastTouchingFluidState_FancyMenu;

    @Unique
    private boolean touchingStateInitialized_FancyMenu;

    @Unique
    private String lastTouchingFluidKey_FancyMenu;

    @Unique
    private static final FluidContactInfo NO_FLUID_FancyMenu = new FluidContactInfo(false, null);

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        LocalPlayer self = (LocalPlayer)(Object)this;

        this.updateFluidListeners_FancyMenu(self);

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
    private void updateFluidListeners_FancyMenu(LocalPlayer self) {
        FluidContactInfo contactInfo = this.detectFluidContact_FancyMenu(self);
        boolean isTouchingFluid = contactInfo.touching();
        String currentFluidKey = contactInfo.fluidKey();

        if (!this.touchingStateInitialized_FancyMenu) {
            this.touchingStateInitialized_FancyMenu = true;
            this.lastTouchingFluidState_FancyMenu = isTouchingFluid;
            this.lastTouchingFluidKey_FancyMenu = isTouchingFluid ? currentFluidKey : null;
        } else {
            if (!this.lastTouchingFluidState_FancyMenu && isTouchingFluid) {
                this.lastTouchingFluidKey_FancyMenu = currentFluidKey;
                Listeners.ON_START_TOUCHING_FLUID.onStartTouchingFluid(currentFluidKey);
            } else if (this.lastTouchingFluidState_FancyMenu && !isTouchingFluid) {
                Listeners.ON_STOP_TOUCHING_FLUID.onStopTouchingFluid(this.lastTouchingFluidKey_FancyMenu);
                this.lastTouchingFluidKey_FancyMenu = null;
            } else if (isTouchingFluid) {
                this.lastTouchingFluidKey_FancyMenu = currentFluidKey;
            }
            this.lastTouchingFluidState_FancyMenu = isTouchingFluid;
        }

        boolean isSwimming = self.isSwimming();
        String swimmingFluidKey = isSwimming ? currentFluidKey : null;

        if (!this.swimmingStateInitialized_FancyMenu) {
            this.swimmingStateInitialized_FancyMenu = true;
            this.lastSwimmingState_FancyMenu = isSwimming;
            this.lastSwimmingFluidKey_FancyMenu = isSwimming ? swimmingFluidKey : null;
            return;
        }

        if (!this.lastSwimmingState_FancyMenu && isSwimming) {
            this.lastSwimmingFluidKey_FancyMenu = swimmingFluidKey;
            Listeners.ON_START_SWIMMING.onStartSwimming(swimmingFluidKey);
        } else if (this.lastSwimmingState_FancyMenu && !isSwimming) {
            Listeners.ON_STOP_SWIMMING.onStopSwimming(this.lastSwimmingFluidKey_FancyMenu);
            this.lastSwimmingFluidKey_FancyMenu = null;
        } else if (isSwimming) {
            this.lastSwimmingFluidKey_FancyMenu = swimmingFluidKey;
        }

        this.lastSwimmingState_FancyMenu = isSwimming;
    }

    @Unique
    private FluidContactInfo detectFluidContact_FancyMenu(LocalPlayer self) {
        if (!(self.level() instanceof ClientLevel clientLevel)) {
            return NO_FLUID_FancyMenu;
        }

        AABB box = self.getBoundingBox().deflate(0.001D);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.ceil(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.ceil(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.ceil(box.maxZ);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (!clientLevel.hasChunkAt(x, z)) {
                    continue;
                }
                for (int y = minY; y < maxY; y++) {
                    mutablePos.set(x, y, z);
                    FluidState fluidState = clientLevel.getFluidState(mutablePos);
                    if (fluidState.isEmpty()) {
                        continue;
                    }

                    double fluidSurface = (double)y + fluidState.getHeight(clientLevel, mutablePos);
                    if (fluidSurface >= box.minY) {
                        ResourceLocation fluidLocation = BuiltInRegistries.FLUID.getKey(fluidState.getType());
                        String key = (fluidLocation != null) ? fluidLocation.toString() : null;
                        return new FluidContactInfo(true, key);
                    }
                }
            }
        }

        return NO_FLUID_FancyMenu;
    }

}

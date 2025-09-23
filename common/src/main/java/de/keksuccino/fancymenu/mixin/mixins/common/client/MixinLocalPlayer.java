package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.FluidContactInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
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
    private boolean positionChangeInitialized_FancyMenu;

    @Unique
    private BlockPos lastKnownBlockPosition_FancyMenu;

    @Unique
    private BlockPos lastSteppedBlockPos_FancyMenu;

    @Unique
    private boolean steppingStateInitialized_FancyMenu;

    @Unique
    private boolean ridingStateInitialized_FancyMenu;

    @Unique
    private boolean lastRidingState_FancyMenu;

    @Unique
    private Entity lastMountedEntity_FancyMenu;

    @Unique
    private static final FluidContactInfo NO_FLUID_FANCYMENU = new FluidContactInfo(false, null);

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        LocalPlayer self = (LocalPlayer)(Object)this;

        this.updateFluidListeners_FancyMenu(self);
        this.updatePositionChangedListener_FancyMenu(self);
        this.updateSteppingListener_FancyMenu(self);
        this.updateRidingListeners_FancyMenu(self);

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
            this.lastTouchingFluidKey_FancyMenu = isTouchingFluid ? currentFluidKey : null;
            if (isTouchingFluid) {
                Listeners.ON_START_TOUCHING_FLUID.onStartTouchingFluid(currentFluidKey);
            }
        } else {
            if (!this.lastTouchingFluidState_FancyMenu && isTouchingFluid) {
                this.lastTouchingFluidKey_FancyMenu = currentFluidKey;
                Listeners.ON_START_TOUCHING_FLUID.onStartTouchingFluid(currentFluidKey);
            } else if (this.lastTouchingFluidState_FancyMenu && !isTouchingFluid) {
                Listeners.ON_STOP_TOUCHING_FLUID.onStopTouchingFluid(this.lastTouchingFluidKey_FancyMenu);
                this.lastTouchingFluidKey_FancyMenu = null;
            } else if (isTouchingFluid) {
                this.lastTouchingFluidKey_FancyMenu = currentFluidKey;
            } else {
                this.lastTouchingFluidKey_FancyMenu = null;
            }
        }
        this.lastTouchingFluidState_FancyMenu = isTouchingFluid;

        boolean isSwimming = self.isSwimming();
        String swimmingFluidKey = isSwimming ? currentFluidKey : null;

        if (!this.swimmingStateInitialized_FancyMenu) {
            this.swimmingStateInitialized_FancyMenu = true;
            this.lastSwimmingFluidKey_FancyMenu = swimmingFluidKey;
            if (isSwimming) {
                Listeners.ON_START_SWIMMING.onStartSwimming(swimmingFluidKey);
            }
        } else {
            if (!this.lastSwimmingState_FancyMenu && isSwimming) {
                this.lastSwimmingFluidKey_FancyMenu = swimmingFluidKey;
                Listeners.ON_START_SWIMMING.onStartSwimming(swimmingFluidKey);
            } else if (this.lastSwimmingState_FancyMenu && !isSwimming) {
                Listeners.ON_STOP_SWIMMING.onStopSwimming(this.lastSwimmingFluidKey_FancyMenu);
                this.lastSwimmingFluidKey_FancyMenu = null;
            } else if (isSwimming) {
                this.lastSwimmingFluidKey_FancyMenu = swimmingFluidKey;
            } else {
                this.lastSwimmingFluidKey_FancyMenu = null;
            }
        }

        this.lastSwimmingState_FancyMenu = isSwimming;
    }

    @Unique
    private void updatePositionChangedListener_FancyMenu(LocalPlayer self) {
        if (!(self.level() instanceof ClientLevel clientLevel)) {
            return;
        }

        BlockPos currentPos = self.blockPosition();
        if (!clientLevel.hasChunkAt(currentPos.getX(), currentPos.getZ())) {
            return;
        }

        BlockPos immutablePos = currentPos.immutable();
        if (!this.positionChangeInitialized_FancyMenu) {
            this.positionChangeInitialized_FancyMenu = true;
            this.lastKnownBlockPosition_FancyMenu = immutablePos;
            return;
        }

        if (!immutablePos.equals(this.lastKnownBlockPosition_FancyMenu)) {
            BlockPos previousPos = this.lastKnownBlockPosition_FancyMenu;
            this.lastKnownBlockPosition_FancyMenu = immutablePos;
            Listeners.ON_POSITION_CHANGED.onPositionChanged(previousPos, immutablePos);
        }
    }

    @Unique
    private void updateSteppingListener_FancyMenu(LocalPlayer self) {
        if (!(self.level() instanceof ClientLevel clientLevel)) {
            this.resetSteppingState_FancyMenu();
            return;
        }

        if (!self.onGround() || self.isSpectator()) {
            this.resetSteppingState_FancyMenu();
            return;
        }

        BlockPos onPos = self.getOnPos();
        if (!clientLevel.hasChunkAt(onPos)) {
            return;
        }

        BlockState blockState = clientLevel.getBlockState(onPos);
        if (blockState.isAir()) {
            return;
        }

        BlockPos immutablePos = onPos.immutable();

        if (!this.steppingStateInitialized_FancyMenu || !immutablePos.equals(this.lastSteppedBlockPos_FancyMenu)) {
            this.steppingStateInitialized_FancyMenu = true;
            this.lastSteppedBlockPos_FancyMenu = immutablePos;
            Listeners.ON_STEPPING_ON_BLOCK.onSteppedOnBlock(immutablePos, blockState);
        }
    }

    @Unique
    private void updateRidingListeners_FancyMenu(LocalPlayer self) {
        Entity vehicle = self.getVehicle();
        boolean isRiding = vehicle != null;

        if (!this.ridingStateInitialized_FancyMenu) {
            this.ridingStateInitialized_FancyMenu = true;
            if (isRiding) {
                this.updateMountedEntityCache_FancyMenu(vehicle);
                Listeners.ON_ENTITY_MOUNTED.onEntityMounted(vehicle);
            }
        } else {
            if (!this.lastRidingState_FancyMenu && isRiding) {
                this.updateMountedEntityCache_FancyMenu(vehicle);
                Listeners.ON_ENTITY_MOUNTED.onEntityMounted(vehicle);
            } else if (this.lastRidingState_FancyMenu && !isRiding) {
                this.fireEntityUnmountedListener_FancyMenu();
            } else if (isRiding) {
                this.updateMountedEntityCache_FancyMenu(vehicle);
            }
        }

        this.lastRidingState_FancyMenu = isRiding;
        if (!isRiding) {
            this.clearMountedEntityCache_FancyMenu();
        }
    }

    @Unique
    private void fireEntityUnmountedListener_FancyMenu() {
        if (this.lastMountedEntity_FancyMenu != null) {
            Listeners.ON_ENTITY_UNMOUNTED.onEntityUnmounted(this.lastMountedEntity_FancyMenu);
        } else {
            Listeners.ON_ENTITY_UNMOUNTED.onEntityUnmounted(null);
        }
    }

    @Unique
    private void updateMountedEntityCache_FancyMenu(Entity vehicle) {
        this.lastMountedEntity_FancyMenu = vehicle;
    }

    @Unique
    private void clearMountedEntityCache_FancyMenu() {
        this.lastMountedEntity_FancyMenu = null;
    }

    @Unique
    private void resetSteppingState_FancyMenu() {
        this.steppingStateInitialized_FancyMenu = false;
        this.lastSteppedBlockPos_FancyMenu = null;
    }

    @Unique
    private FluidContactInfo detectFluidContact_FancyMenu(LocalPlayer self) {
        if (!(self.level() instanceof ClientLevel clientLevel)) {
            return NO_FLUID_FANCYMENU;
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

        return NO_FLUID_FANCYMENU;
    }

    /** @reason Fire FancyMenu listener when the local player drops an item. */
    @WrapOperation(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack wrap_removeFromSelected_FancyMenu(Inventory inventory, boolean fullStack, Operation<ItemStack> operation) {
        ItemStack removed = operation.call(inventory, fullStack);
        if (!removed.isEmpty()) {
            ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(removed.getItem());
            String itemKey = itemLocation != null ? itemLocation.toString() : null;
            Listeners.ON_ITEM_DROPPED.onItemDropped(itemKey);
        }
        return removed;
    }
}


package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.FluidContactInfo;
import de.keksuccino.fancymenu.mixin.interfaces.LocalPlayerDrowningTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Objects;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer implements LocalPlayerDrowningTracker {

    @Unique
    private ResourceKey<Biome> lastBiomeKey_FancyMenu;

    @Unique
    private boolean biomeStateInitialized_FancyMenu;

    @Unique private boolean biomeTrackingWasDormant_FancyMenu;

    @Unique
    private boolean lastSwimmingState_FancyMenu;

    @Unique private boolean swimmingTrackingWasDormant_FancyMenu;

    @Unique
    private boolean runningStateInitialized_FancyMenu;

    @Unique
    private boolean lastRunningState_FancyMenu;

    @Unique private boolean runningTrackingWasDormant_FancyMenu;

    @Unique
    private boolean swimmingStateInitialized_FancyMenu;

    @Unique
    private String lastSwimmingFluidKey_FancyMenu;

    @Unique
    private boolean lastTouchingFluidState_FancyMenu;

    @Unique private boolean touchingTrackingWasDormant_FancyMenu;

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

    @Unique private boolean steppingTrackingWasDormant_FancyMenu;

    @Unique
    private boolean ridingStateInitialized_FancyMenu;

    @Unique
    private boolean lastRidingState_FancyMenu;

    @Unique private boolean ridingTrackingWasDormant_FancyMenu;

    @Unique
    private Entity lastMountedEntity_FancyMenu;

    @Unique
    private ResourceKey<Level> lastDimensionKey_FancyMenu;

    @Unique
    private boolean dimensionInitialized_FancyMenu;

    @Unique private boolean dimensionTrackingWasDormant_FancyMenu;

    @Unique
    private boolean burningStateInitialized_FancyMenu;

    @Unique
    private boolean lastBurningState_FancyMenu;

    @Unique private boolean burningTrackingWasDormant_FancyMenu;

    @Unique
    private boolean drowningActive_FancyMenu;

    @Unique private boolean drowningTrackingWasDormant_FancyMenu;

    @Unique
    private boolean freezingStateInitialized_FancyMenu;

    @Unique
    private boolean lastFreezingState_FancyMenu;

    @Unique private boolean freezingTrackingWasDormant_FancyMenu;

    @Unique
    private boolean fullyFrozenStateInitialized_FancyMenu;

    @Unique
    private boolean lastFullyFrozenState_FancyMenu;

    @Unique private boolean fullyFrozenTrackingWasDormant_FancyMenu;

    @Unique
    private boolean experienceInitialized_FancyMenu;

    @Unique
    private boolean shouldEmitExperienceChange_FancyMenu;

    @Unique
    private int previousTotalExperience_FancyMenu;

    @Unique
    private int previousExperienceLevel_FancyMenu;

    @Unique
    private boolean healthInitialized_FancyMenu;

    @Unique
    private float lastKnownHealth_FancyMenu;

    @Unique
    private static final FluidContactInfo NO_FLUID_FANCYMENU = new FluidContactInfo(false, null);

    @Unique
    private boolean weatherStateInitialized_FancyMenu;

    @Unique
    private String lastWeatherType_FancyMenu;

    @Unique
    private boolean lastWeatherCanSnow_FancyMenu;

    @Unique
    private boolean lastWeatherCanRain_FancyMenu;

    @Unique private boolean weatherTrackingWasDormant_FancyMenu;

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        LocalPlayer self = (LocalPlayer)(Object)this;

        this.updateFluidListeners_FancyMenu(self);
        this.updatePositionChangedListener_FancyMenu(self);
        this.updateSteppingListener_FancyMenu(self);
        this.updateDimensionListener_FancyMenu(self);
        this.updateBurningListeners_FancyMenu(self);
        this.updateDrowningListenerState_FancyMenu(self);
        this.updateFreezingListeners_FancyMenu(self);
        this.updateRunningListeners_FancyMenu(self);
        this.updateRidingListeners_FancyMenu(self);
        this.updateBiomeListeners_FancyMenu(self);
        this.updateWeatherListener_FancyMenu(self, self.level() instanceof ClientLevel clientLevel ? clientLevel : null);
        this.updateDamageListener_FancyMenu(self);
    }

    @Unique
    private void updateDimensionListener_FancyMenu(LocalPlayer self) {
        if (!Listeners.ON_DIMENSION_ENTERED.hasInstancesListening()) {
            this.dimensionTrackingWasDormant_FancyMenu = true;
            this.dimensionInitialized_FancyMenu = false;
            this.lastDimensionKey_FancyMenu = null;
            return;
        }
        if (self.level() == null) return;
        ResourceKey<Level> currentDimensionKey = self.level().dimension();
        if (!this.dimensionInitialized_FancyMenu) {
            this.dimensionInitialized_FancyMenu = true;
            this.lastDimensionKey_FancyMenu = currentDimensionKey;
            if (!this.dimensionTrackingWasDormant_FancyMenu && currentDimensionKey != null) Listeners.ON_DIMENSION_ENTERED.onDimensionEntered(currentDimensionKey);
            this.dimensionTrackingWasDormant_FancyMenu = false;
        } else if (!Objects.equals(this.lastDimensionKey_FancyMenu, currentDimensionKey)) {
            this.lastDimensionKey_FancyMenu = currentDimensionKey;
            if (currentDimensionKey != null) Listeners.ON_DIMENSION_ENTERED.onDimensionEntered(currentDimensionKey);
        }
    }

    @Unique
    private void updateBurningListeners_FancyMenu(LocalPlayer self) {
        boolean tracking = Listeners.ON_STARTED_BURNING.hasInstancesListening() || Listeners.ON_STOPPED_BURNING.hasInstancesListening();
        if (!tracking) {
            this.burningTrackingWasDormant_FancyMenu = true;
            this.burningStateInitialized_FancyMenu = false;
            return;
        }
        boolean isBurning = self.isOnFire();
        if (!this.burningStateInitialized_FancyMenu) {
            this.burningStateInitialized_FancyMenu = true;
            this.lastBurningState_FancyMenu = isBurning;
            if (!this.burningTrackingWasDormant_FancyMenu && isBurning) Listeners.ON_STARTED_BURNING.onStartedBurning();
            this.burningTrackingWasDormant_FancyMenu = false;
            return;
        }
        if (!this.lastBurningState_FancyMenu && isBurning) {
            Listeners.ON_STARTED_BURNING.onStartedBurning();
        } else if (this.lastBurningState_FancyMenu && !isBurning) {
            Listeners.ON_STOPPED_BURNING.onStoppedBurning();
        }
        this.lastBurningState_FancyMenu = isBurning;
    }

    @Unique
    private void updateDrowningListenerState_FancyMenu(LocalPlayer self) {
        if (!Listeners.ON_STARTED_DROWNING.hasInstancesListening()) {
            this.drowningActive_FancyMenu = false;
            this.drowningTrackingWasDormant_FancyMenu = true;
            return;
        }
        this.fancymenu$prepareDrowningTracking();
        if (self.getAirSupply() >= self.getMaxAirSupply()) {
            this.drowningActive_FancyMenu = false;
        }
    }

    @Unique
    private void updateFreezingListeners_FancyMenu(LocalPlayer self) {
        boolean trackFreezing = Listeners.ON_STARTED_FREEZING.hasInstancesListening() || Listeners.ON_STOPPED_FREEZING.hasInstancesListening();
        boolean trackFullyFrozen = Listeners.ON_FULLY_FROZEN.hasInstancesListening();
        if (!trackFreezing && !trackFullyFrozen) {
            this.freezingTrackingWasDormant_FancyMenu = true;
            this.fullyFrozenTrackingWasDormant_FancyMenu = true;
            this.freezingStateInitialized_FancyMenu = false;
            this.fullyFrozenStateInitialized_FancyMenu = false;
            return;
        }

        int ticksFrozen = self.getTicksFrozen();
        int ticksRequiredToFreeze = self.getTicksRequiredToFreeze();
        if (trackFreezing) {
            boolean isFreezing = ticksFrozen > 0;
            float freezingIntensity = ticksRequiredToFreeze > 0 ? Mth.clamp((float)ticksFrozen / (float)ticksRequiredToFreeze, 0.0F, 1.0F) : 0.0F;
            if (!this.freezingStateInitialized_FancyMenu) {
                this.freezingStateInitialized_FancyMenu = true;
                this.lastFreezingState_FancyMenu = isFreezing;
                if (!this.freezingTrackingWasDormant_FancyMenu && isFreezing) Listeners.ON_STARTED_FREEZING.onStartedFreezing(freezingIntensity);
                this.freezingTrackingWasDormant_FancyMenu = false;
            } else {
                if (!this.lastFreezingState_FancyMenu && isFreezing) {
                    Listeners.ON_STARTED_FREEZING.onStartedFreezing(freezingIntensity);
                } else if (this.lastFreezingState_FancyMenu && !isFreezing) {
                    Listeners.ON_STOPPED_FREEZING.onStoppedFreezing();
                }
                this.lastFreezingState_FancyMenu = isFreezing;
            }
        } else {
            this.freezingTrackingWasDormant_FancyMenu = true;
            this.freezingStateInitialized_FancyMenu = false;
        }

        if (trackFullyFrozen) {
            boolean isFullyFrozen = ticksRequiredToFreeze > 0 && ticksFrozen >= ticksRequiredToFreeze;
            if (!this.fullyFrozenStateInitialized_FancyMenu) {
                this.fullyFrozenStateInitialized_FancyMenu = true;
                this.lastFullyFrozenState_FancyMenu = isFullyFrozen;
                if (!this.fullyFrozenTrackingWasDormant_FancyMenu && isFullyFrozen) Listeners.ON_FULLY_FROZEN.onFullyFrozen();
                this.fullyFrozenTrackingWasDormant_FancyMenu = false;
            } else {
                if (!this.lastFullyFrozenState_FancyMenu && isFullyFrozen) Listeners.ON_FULLY_FROZEN.onFullyFrozen();
                this.lastFullyFrozenState_FancyMenu = isFullyFrozen;
            }
        } else {
            this.fullyFrozenTrackingWasDormant_FancyMenu = true;
            this.fullyFrozenStateInitialized_FancyMenu = false;
        }
    }

    @Unique
    private void updateRunningListeners_FancyMenu(LocalPlayer self) {
        boolean tracking = Listeners.ON_STARTED_RUNNING.hasInstancesListening() || Listeners.ON_STOPPED_RUNNING.hasInstancesListening();
        if (!tracking) {
            this.runningTrackingWasDormant_FancyMenu = true;
            this.runningStateInitialized_FancyMenu = false;
            return;
        }
        boolean isRunning = self.isSprinting();
        if (!this.runningStateInitialized_FancyMenu) {
            this.runningStateInitialized_FancyMenu = true;
            this.lastRunningState_FancyMenu = isRunning;
            if (!this.runningTrackingWasDormant_FancyMenu && isRunning) Listeners.ON_STARTED_RUNNING.onStartedRunning();
            this.runningTrackingWasDormant_FancyMenu = false;
            return;
        }
        if (!this.lastRunningState_FancyMenu && isRunning) {
            Listeners.ON_STARTED_RUNNING.onStartedRunning();
        } else if (this.lastRunningState_FancyMenu && !isRunning) {
            Listeners.ON_STOPPED_RUNNING.onStoppedRunning();
        }
        this.lastRunningState_FancyMenu = isRunning;
    }

    @Unique
    private void updateBiomeListeners_FancyMenu(LocalPlayer self) {
        boolean trackEntering = Listeners.ON_ENTER_BIOME.hasInstancesListening();
        boolean trackLeaving = Listeners.ON_LEAVE_BIOME.hasInstancesListening();
        if (!trackEntering && !trackLeaving) {
            this.biomeTrackingWasDormant_FancyMenu = true;
            this.biomeStateInitialized_FancyMenu = false;
            this.lastBiomeKey_FancyMenu = null;
            return;
        }

        ResourceKey<Biome> currentBiomeKey = null;
        if (self.level() instanceof ClientLevel clientLevel && clientLevel.hasChunkAt(self.getBlockX(), self.getBlockZ())) {
            Holder<Biome> biomeHolder = clientLevel.getBiome(self.blockPosition());
            currentBiomeKey = biomeHolder.unwrapKey().orElse(null);
        }
        if (!this.biomeStateInitialized_FancyMenu) {
            this.biomeStateInitialized_FancyMenu = true;
            this.lastBiomeKey_FancyMenu = currentBiomeKey;
            if (!this.biomeTrackingWasDormant_FancyMenu && trackEntering) Listeners.ON_ENTER_BIOME.onBiomeChanged(currentBiomeKey);
            this.biomeTrackingWasDormant_FancyMenu = false;
            return;
        }
        if (Objects.equals(this.lastBiomeKey_FancyMenu, currentBiomeKey)) return;
        if (trackLeaving && this.lastBiomeKey_FancyMenu != null) Listeners.ON_LEAVE_BIOME.onBiomeLeft(this.lastBiomeKey_FancyMenu);
        this.lastBiomeKey_FancyMenu = currentBiomeKey;
        if (trackEntering) Listeners.ON_ENTER_BIOME.onBiomeChanged(currentBiomeKey);
    }

    @Unique
    private void updateDamageListener_FancyMenu(LocalPlayer self) {
        if (!Listeners.ON_DAMAGE_TAKEN.hasInstancesListening()) {
            this.healthInitialized_FancyMenu = false;
            return;
        }
        float currentHealth = self.getHealth();
        if (!this.healthInitialized_FancyMenu) {
            this.healthInitialized_FancyMenu = true;
        } else if (currentHealth < this.lastKnownHealth_FancyMenu - 1.0E-4F) {
            float damageTaken = this.lastKnownHealth_FancyMenu - currentHealth;
            DamageSource lastDamageSource = self.getLastDamageSource();
            String damageTypeKey = this.resolveDamageTypeKey_FancyMenu(lastDamageSource);
            String damageSourceKey = this.resolveDamageSourceKey_FancyMenu(lastDamageSource);
            boolean fatalDamage = currentHealth <= 0.0F;
            Listeners.ON_DAMAGE_TAKEN.onDamageTaken(damageTaken, damageTypeKey, fatalDamage, damageSourceKey);
        }
        this.lastKnownHealth_FancyMenu = currentHealth;
    }

    @Unique
    private void updateFluidListeners_FancyMenu(LocalPlayer self) {
        boolean trackTouching = Listeners.ON_START_TOUCHING_FLUID.hasInstancesListening() || Listeners.ON_STOP_TOUCHING_FLUID.hasInstancesListening();
        boolean trackSwimming = Listeners.ON_START_SWIMMING.hasInstancesListening() || Listeners.ON_STOP_SWIMMING.hasInstancesListening();
        if (!trackTouching && !trackSwimming) {
            this.touchingTrackingWasDormant_FancyMenu = true;
            this.swimmingTrackingWasDormant_FancyMenu = true;
            this.touchingStateInitialized_FancyMenu = false;
            this.swimmingStateInitialized_FancyMenu = false;
            this.lastTouchingFluidKey_FancyMenu = null;
            this.lastSwimmingFluidKey_FancyMenu = null;
            return;
        }

        FluidContactInfo contactInfo = this.detectFluidContact_FancyMenu(self);
        boolean isTouchingFluid = contactInfo.touching();
        String currentFluidKey = contactInfo.fluidKey();

        if (trackTouching) {
            if (!this.touchingStateInitialized_FancyMenu) {
                this.touchingStateInitialized_FancyMenu = true;
                this.lastTouchingFluidKey_FancyMenu = isTouchingFluid ? currentFluidKey : null;
                if (!this.touchingTrackingWasDormant_FancyMenu && isTouchingFluid) Listeners.ON_START_TOUCHING_FLUID.onStartTouchingFluid(currentFluidKey);
                this.touchingTrackingWasDormant_FancyMenu = false;
            } else if (!this.lastTouchingFluidState_FancyMenu && isTouchingFluid) {
                this.lastTouchingFluidKey_FancyMenu = currentFluidKey;
                Listeners.ON_START_TOUCHING_FLUID.onStartTouchingFluid(currentFluidKey);
            } else if (this.lastTouchingFluidState_FancyMenu && !isTouchingFluid) {
                Listeners.ON_STOP_TOUCHING_FLUID.onStopTouchingFluid(this.lastTouchingFluidKey_FancyMenu);
                this.lastTouchingFluidKey_FancyMenu = null;
            } else {
                this.lastTouchingFluidKey_FancyMenu = isTouchingFluid ? currentFluidKey : null;
            }
            this.lastTouchingFluidState_FancyMenu = isTouchingFluid;
        } else {
            this.touchingTrackingWasDormant_FancyMenu = true;
            this.touchingStateInitialized_FancyMenu = false;
            this.lastTouchingFluidKey_FancyMenu = null;
        }

        if (trackSwimming) {
            boolean isSwimming = self.isSwimming();
            String swimmingFluidKey = isSwimming ? currentFluidKey : null;
            if (!this.swimmingStateInitialized_FancyMenu) {
                this.swimmingStateInitialized_FancyMenu = true;
                this.lastSwimmingFluidKey_FancyMenu = swimmingFluidKey;
                if (!this.swimmingTrackingWasDormant_FancyMenu && isSwimming) Listeners.ON_START_SWIMMING.onStartSwimming(swimmingFluidKey);
                this.swimmingTrackingWasDormant_FancyMenu = false;
            } else if (!this.lastSwimmingState_FancyMenu && isSwimming) {
                this.lastSwimmingFluidKey_FancyMenu = swimmingFluidKey;
                Listeners.ON_START_SWIMMING.onStartSwimming(swimmingFluidKey);
            } else if (this.lastSwimmingState_FancyMenu && !isSwimming) {
                Listeners.ON_STOP_SWIMMING.onStopSwimming(this.lastSwimmingFluidKey_FancyMenu);
                this.lastSwimmingFluidKey_FancyMenu = null;
            } else {
                this.lastSwimmingFluidKey_FancyMenu = swimmingFluidKey;
            }
            this.lastSwimmingState_FancyMenu = isSwimming;
        } else {
            this.swimmingTrackingWasDormant_FancyMenu = true;
            this.swimmingStateInitialized_FancyMenu = false;
            this.lastSwimmingFluidKey_FancyMenu = null;
        }
    }

    @Unique
    private void updatePositionChangedListener_FancyMenu(LocalPlayer self) {
        if (!Listeners.ON_POSITION_CHANGED.hasInstancesListening()) {
            this.positionChangeInitialized_FancyMenu = false;
            this.lastKnownBlockPosition_FancyMenu = null;
            return;
        }
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
        if (!Listeners.ON_STEPPING_ON_BLOCK.hasInstancesListening()) {
            this.steppingTrackingWasDormant_FancyMenu = true;
            this.resetSteppingState_FancyMenu();
            return;
        }
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

        if (!this.steppingStateInitialized_FancyMenu) {
            this.steppingStateInitialized_FancyMenu = true;
            this.lastSteppedBlockPos_FancyMenu = immutablePos;
            if (!this.steppingTrackingWasDormant_FancyMenu) Listeners.ON_STEPPING_ON_BLOCK.onSteppedOnBlock(immutablePos, blockState);
            this.steppingTrackingWasDormant_FancyMenu = false;
            return;
        }
        if (!immutablePos.equals(this.lastSteppedBlockPos_FancyMenu)) {
            this.lastSteppedBlockPos_FancyMenu = immutablePos;
            Listeners.ON_STEPPING_ON_BLOCK.onSteppedOnBlock(immutablePos, blockState);
        }

    }

    @Unique
    private void updateRidingListeners_FancyMenu(LocalPlayer self) {
        boolean tracking = Listeners.ON_ENTITY_MOUNTED.hasInstancesListening() || Listeners.ON_ENTITY_UNMOUNTED.hasInstancesListening();
        if (!tracking) {
            this.ridingTrackingWasDormant_FancyMenu = true;
            this.ridingStateInitialized_FancyMenu = false;
            this.lastRidingState_FancyMenu = false;
            this.clearMountedEntityCache_FancyMenu();
            return;
        }
        Entity vehicle = self.getVehicle();
        boolean isRiding = vehicle != null;

        if (!this.ridingStateInitialized_FancyMenu) {
            this.ridingStateInitialized_FancyMenu = true;
            if (isRiding) {
                this.updateMountedEntityCache_FancyMenu(vehicle);
                if (!this.ridingTrackingWasDormant_FancyMenu) Listeners.ON_ENTITY_MOUNTED.onEntityMounted(vehicle);
            }
            this.ridingTrackingWasDormant_FancyMenu = false;
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
                        Identifier fluidLocation = BuiltInRegistries.FLUID.getKey(fluidState.getType());
                        String key = (fluidLocation != null) ? fluidLocation.toString() : null;
                        return new FluidContactInfo(true, key);
                    }
                }
            }
        }

        return NO_FLUID_FANCYMENU;

    }

    @Inject(method = "setExperienceValues", at = @At("HEAD"))
    private void before_setExperienceValues_FancyMenu(float currentXP, int totalExperience, int level, CallbackInfo ci) {
        if (!Listeners.ON_EXPERIENCE_CHANGED.hasInstancesListening()) {
            this.experienceInitialized_FancyMenu = false;
            this.shouldEmitExperienceChange_FancyMenu = false;
            return;
        }
        LocalPlayer self = (LocalPlayer)(Object)this;
        this.shouldEmitExperienceChange_FancyMenu = this.experienceInitialized_FancyMenu;
        this.previousTotalExperience_FancyMenu = self.totalExperience;
        this.previousExperienceLevel_FancyMenu = self.experienceLevel;

    }

    @Inject(method = "setExperienceValues", at = @At("TAIL"))
    private void after_setExperienceValues_FancyMenu(float currentXP, int totalExperience, int level, CallbackInfo ci) {
        if (!Listeners.ON_EXPERIENCE_CHANGED.hasInstancesListening()) return;
        LocalPlayer self = (LocalPlayer)(Object)this;
        if (!this.shouldEmitExperienceChange_FancyMenu) {
            this.experienceInitialized_FancyMenu = true;
            return;
        }
        this.shouldEmitExperienceChange_FancyMenu = false;
        int newTotalExperience = self.totalExperience;
        if (this.previousTotalExperience_FancyMenu != newTotalExperience) {
            boolean levelUp = self.experienceLevel > this.previousExperienceLevel_FancyMenu;
            Listeners.ON_EXPERIENCE_CHANGED.onExperienceChanged(this.previousTotalExperience_FancyMenu, newTotalExperience, levelUp);
        }

    }

    @Unique
    private String resolveDamageTypeKey_FancyMenu(@Nullable DamageSource damageSource) {
        if (damageSource == null) {
            return "unknown";
        }
        return damageSource.typeHolder().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unknown");

    }

    @Unique
    private @Nullable String resolveDamageSourceKey_FancyMenu(@Nullable DamageSource damageSource) {
        if (damageSource == null) {
            return null;
        }
        Entity causingEntity = damageSource.getEntity();
        if (causingEntity != null) {
            Identifier entityLocation = BuiltInRegistries.ENTITY_TYPE.getKey(causingEntity.getType());
            return entityLocation != null ? entityLocation.toString() : null;
        }
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity != null) {
            Identifier entityLocation = BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType());
            return entityLocation != null ? entityLocation.toString() : null;
        }
        return null;
    }

    @Override
    public boolean fancymenu$isDrowningActive() {
        return this.drowningActive_FancyMenu;
    }

    @Override
    public void fancymenu$setDrowningActive(boolean active) {
        this.drowningActive_FancyMenu = active;
    }

    @Override
    public void fancymenu$prepareDrowningTracking() {
        if (!this.drowningTrackingWasDormant_FancyMenu) {
            return;
        }
        LocalPlayer self = (LocalPlayer)(Object)this;
        DamageSource lastDamageSource = self.getLastDamageSource();
        // Do not retain listener state while dormant. A recent vanilla drowning hit seeds an already-running episode silently when tracking resumes.
        this.drowningActive_FancyMenu = self.getAirSupply() < self.getMaxAirSupply() && lastDamageSource != null && lastDamageSource.is(DamageTypes.DROWN);
        this.drowningTrackingWasDormant_FancyMenu = false;
    }

    @Unique
    private void updateWeatherListener_FancyMenu(LocalPlayer self, @Nullable ClientLevel clientLevel) {
        if (!Listeners.ON_WEATHER_CHANGED.hasInstancesListening() || clientLevel == null) {
            if (!Listeners.ON_WEATHER_CHANGED.hasInstancesListening()) this.weatherTrackingWasDormant_FancyMenu = true;
            this.weatherStateInitialized_FancyMenu = false;
            this.lastWeatherType_FancyMenu = null;
            return;
        }

        BlockPos playerPos = self.blockPosition();
        if (!clientLevel.hasChunkAt(playerPos)) {
            return;
        }

        boolean isThundering = clientLevel.isThundering();
        boolean isRaining = clientLevel.isRaining();
        String weatherType = isThundering ? "thunder" : (isRaining ? "rain" : "clear");

        boolean canSnow = false;
        boolean canRain = false;
        if (isRaining) {
            Holder<Biome> biomeHolder = clientLevel.getBiome(playerPos);
            Biome biome = biomeHolder.value();
            Biome.Precipitation precipitation = biome.getPrecipitationAt(playerPos, clientLevel.getSeaLevel());
            if (precipitation == Biome.Precipitation.SNOW) {
                canSnow = clientLevel.canSeeSky(playerPos);
            } else if (precipitation == Biome.Precipitation.RAIN) {
                canRain = clientLevel.isRainingAt(playerPos);
            }
        }

        if (!this.weatherStateInitialized_FancyMenu
                || !Objects.equals(this.lastWeatherType_FancyMenu, weatherType)
                || this.lastWeatherCanSnow_FancyMenu != canSnow
                || this.lastWeatherCanRain_FancyMenu != canRain) {
            boolean wasInitialized = this.weatherStateInitialized_FancyMenu;
            this.weatherStateInitialized_FancyMenu = true;
            this.lastWeatherType_FancyMenu = weatherType;
            this.lastWeatherCanSnow_FancyMenu = canSnow;
            this.lastWeatherCanRain_FancyMenu = canRain;
            if (wasInitialized || !this.weatherTrackingWasDormant_FancyMenu) Listeners.ON_WEATHER_CHANGED.onWeatherChanged(weatherType, canSnow, canRain);
            this.weatherTrackingWasDormant_FancyMenu = false;
        }

    }

    /** @reason Fire FancyMenu listener when the local player drops an item. */
    @WrapOperation(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack wrap_removeFromSelected_FancyMenu(Inventory inventory, boolean fullStack, Operation<ItemStack> operation) {
        ItemStack removed = operation.call(inventory, fullStack);
        if (!removed.isEmpty() && Listeners.ON_ITEM_DROPPED.hasInstancesListening()) {
            Identifier itemLocation = BuiltInRegistries.ITEM.getKey(removed.getItem());
            String itemKey = itemLocation != null ? itemLocation.toString() : null;
            Listeners.ON_ITEM_DROPPED.onItemDropped(itemKey);
        }
        return removed;
    }

}

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
import net.minecraft.resources.ResourceLocation;
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
    private boolean lastSwimmingState_FancyMenu;

    @Unique
    private boolean runningStateInitialized_FancyMenu;

    @Unique
    private boolean lastRunningState_FancyMenu;

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
    private ResourceKey<Level> lastDimensionKey_FancyMenu;

    @Unique
    private boolean dimensionInitialized_FancyMenu;

    @Unique
    private boolean burningStateInitialized_FancyMenu;

    @Unique
    private boolean lastBurningState_FancyMenu;

    @Unique
    private boolean drowningActive_FancyMenu;

    @Unique
    private boolean freezingStateInitialized_FancyMenu;

    @Unique
    private boolean lastFreezingState_FancyMenu;

    @Unique
    private boolean fullyFrozenStateInitialized_FancyMenu;

    @Unique
    private boolean lastFullyFrozenState_FancyMenu;

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

    @Inject(method = "tick", at = @At("TAIL"))
    private void after_tick_FancyMenu(CallbackInfo info) {
        LocalPlayer self = (LocalPlayer)(Object)this;

        this.updateFluidListeners_FancyMenu(self);
        this.updatePositionChangedListener_FancyMenu(self);
        this.updateSteppingListener_FancyMenu(self);
        if (self.level() != null) {
            ResourceKey<Level> currentDimensionKey = self.level().dimension();
            if (!this.dimensionInitialized_FancyMenu || !Objects.equals(this.lastDimensionKey_FancyMenu, currentDimensionKey)) {
                this.dimensionInitialized_FancyMenu = true;
                this.lastDimensionKey_FancyMenu = currentDimensionKey;
                if (currentDimensionKey != null) {
                    Listeners.ON_DIMENSION_ENTERED.onDimensionEntered(currentDimensionKey);
                }
            }
        }

        boolean isBurning = self.isOnFire();
        if (!this.burningStateInitialized_FancyMenu) {
            this.burningStateInitialized_FancyMenu = true;
            this.lastBurningState_FancyMenu = isBurning;
            if (isBurning) {
                Listeners.ON_STARTED_BURNING.onStartedBurning();
            }
        } else {
            if (!this.lastBurningState_FancyMenu && isBurning) {
                Listeners.ON_STARTED_BURNING.onStartedBurning();
            } else if (this.lastBurningState_FancyMenu && !isBurning) {
                Listeners.ON_STOPPED_BURNING.onStoppedBurning();
            }
            this.lastBurningState_FancyMenu = isBurning;
        }

        if (self.getAirSupply() >= self.getMaxAirSupply()) {
            this.drowningActive_FancyMenu = false;
        }

        int ticksFrozen = self.getTicksFrozen();
        int ticksRequiredToFreeze = self.getTicksRequiredToFreeze();
        boolean isFreezing = ticksFrozen > 0;
        float freezingIntensity = 0.0F;
        if (ticksRequiredToFreeze > 0) {
            freezingIntensity = Mth.clamp((float)ticksFrozen / (float)ticksRequiredToFreeze, 0.0F, 1.0F);
        }

        if (!this.freezingStateInitialized_FancyMenu) {
            this.freezingStateInitialized_FancyMenu = true;
            this.lastFreezingState_FancyMenu = isFreezing;
            if (isFreezing) {
                Listeners.ON_STARTED_FREEZING.onStartedFreezing(freezingIntensity);
            }
        } else {
            if (!this.lastFreezingState_FancyMenu && isFreezing) {
                Listeners.ON_STARTED_FREEZING.onStartedFreezing(freezingIntensity);
            } else if (this.lastFreezingState_FancyMenu && !isFreezing) {
                Listeners.ON_STOPPED_FREEZING.onStoppedFreezing();
            }
        }

        this.lastFreezingState_FancyMenu = isFreezing;

        boolean isFullyFrozen = ticksRequiredToFreeze > 0 && ticksFrozen >= ticksRequiredToFreeze;
        if (!this.fullyFrozenStateInitialized_FancyMenu) {
            this.fullyFrozenStateInitialized_FancyMenu = true;
            this.lastFullyFrozenState_FancyMenu = isFullyFrozen;
            if (isFullyFrozen) {
                Listeners.ON_FULLY_FROZEN.onFullyFrozen();
            }
        } else {
            if (!this.lastFullyFrozenState_FancyMenu && isFullyFrozen) {
                Listeners.ON_FULLY_FROZEN.onFullyFrozen();
            }
        }
        this.lastFullyFrozenState_FancyMenu = isFullyFrozen;

        boolean isRunning = self.isSprinting();
        if (!this.runningStateInitialized_FancyMenu) {
            this.runningStateInitialized_FancyMenu = true;
            this.lastRunningState_FancyMenu = isRunning;
            if (isRunning) {
                Listeners.ON_STARTED_RUNNING.onStartedRunning();
            }
        } else {
            if (!this.lastRunningState_FancyMenu && isRunning) {
                Listeners.ON_STARTED_RUNNING.onStartedRunning();
            } else if (this.lastRunningState_FancyMenu && !isRunning) {
                Listeners.ON_STOPPED_RUNNING.onStoppedRunning();
            }
        }
        this.lastRunningState_FancyMenu = isRunning;

        this.updateRidingListeners_FancyMenu(self);

        ResourceKey<Biome> currentBiomeKey = null;
        ClientLevel cachedClientLevel = null;
        if (self.level() instanceof ClientLevel clientLevel) {
            cachedClientLevel = clientLevel;
            if (clientLevel.hasChunkAt(self.getBlockX(), self.getBlockZ())) {
                Holder<Biome> biomeHolder = clientLevel.getBiome(self.blockPosition());
                currentBiomeKey = biomeHolder.unwrapKey().orElse(null);
            }
        }

        this.updateWeatherListener_FancyMenu(self, cachedClientLevel);

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

    @Inject(method = "setExperienceValues", at = @At("HEAD"))
    private void before_setExperienceValues_FancyMenu(float currentXP, int totalExperience, int level, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer)(Object)this;
        this.shouldEmitExperienceChange_FancyMenu = this.experienceInitialized_FancyMenu;
        this.previousTotalExperience_FancyMenu = self.totalExperience;
        this.previousExperienceLevel_FancyMenu = self.experienceLevel;

    }

    @Inject(method = "setExperienceValues", at = @At("TAIL"))
    private void after_setExperienceValues_FancyMenu(float currentXP, int totalExperience, int level, CallbackInfo ci) {
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
                .map(key -> key.location().toString())
                .orElse("unknown");

    }

    @Unique
    private @Nullable String resolveDamageSourceKey_FancyMenu(@Nullable DamageSource damageSource) {
        if (damageSource == null) {
            return null;
        }
        Entity causingEntity = damageSource.getEntity();
        if (causingEntity != null) {
            ResourceLocation entityLocation = BuiltInRegistries.ENTITY_TYPE.getKey(causingEntity.getType());
            return entityLocation != null ? entityLocation.toString() : null;
        }
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity != null) {
            ResourceLocation entityLocation = BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType());
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

    @Unique
    private void updateWeatherListener_FancyMenu(LocalPlayer self, @Nullable ClientLevel clientLevel) {
        if (clientLevel == null) {
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
            this.weatherStateInitialized_FancyMenu = true;
            this.lastWeatherType_FancyMenu = weatherType;
            this.lastWeatherCanSnow_FancyMenu = canSnow;
            this.lastWeatherCanRain_FancyMenu = canRain;
            Listeners.ON_WEATHER_CHANGED.onWeatherChanged(weatherType, canSnow, canRain);
        }

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

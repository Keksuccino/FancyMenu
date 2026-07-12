package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.FluidContactInfo;
import de.keksuccino.fancymenu.mixin.interfaces.LocalPlayerDrowningTracker;
import de.keksuccino.fancymenu.mixin.support.client.DormantTransitionTracker;
import de.keksuccino.fancymenu.mixin.support.client.DrowningEpisodeTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Objects;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer implements LocalPlayerDrowningTracker {

    @Unique private final DormantTransitionTracker<ResourceKey<Biome>> biomeTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<Boolean> swimmingTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<Boolean> runningTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<Boolean> touchingTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<BlockPos> positionTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<BlockPos> steppingTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<Boolean> ridingTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<ResourceKey<Level>> dimensionTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<Boolean> burningTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<Boolean> freezingTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private final DormantTransitionTracker<Boolean> fullyFrozenTransition_FancyMenu = new DormantTransitionTracker<>();
    @Unique private String lastSwimmingFluidKey_FancyMenu;
    @Unique private String lastTouchingFluidKey_FancyMenu;
    @Unique private Entity lastMountedEntity_FancyMenu;
    @Unique private final DrowningEpisodeTracker drowningEpisodeTracker_FancyMenu = new DrowningEpisodeTracker();

    @Unique private static final FluidContactInfo NO_FLUID_FANCYMENU = new FluidContactInfo(false, null);
    @Unique private boolean experienceInitialized_FancyMenu;
    @Unique private boolean shouldEmitExperienceChange_FancyMenu;
    @Unique private int previousTotalExperience_FancyMenu;
    @Unique private int previousExperienceLevel_FancyMenu;
    @Unique private boolean healthInitialized_FancyMenu;
    @Unique private float lastKnownHealth_FancyMenu;
    @Unique private boolean weatherStateInitialized_FancyMenu;
    @Unique private String lastWeatherType_FancyMenu;
    @Unique private boolean lastWeatherCanSnow_FancyMenu;
    @Unique private boolean lastWeatherCanRain_FancyMenu;
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

    @Inject(method = "removeEffectNoUpdate", at = @At("TAIL"))
    private void after_removeEffectNoUpdate_FancyMenu(@Nullable MobEffect effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        if (!Listeners.ON_EFFECT_LOST.hasInstancesListening()) return;
        MobEffectInstance removedInstance = cir.getReturnValue();
        if (removedInstance == null || effect == null) return;
        String effectKey = this.resolveEffectKey_FancyMenu(effect);
        String effectType = this.resolveEffectTypeName_FancyMenu(effect);
        Listeners.ON_EFFECT_LOST.onEffectLost(effectKey, effectType);
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

    /** @reason Fire FancyMenu listener when the local player drops an item. */
    @WrapOperation(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack wrap_removeFromSelected_FancyMenu(Inventory inventory, boolean fullStack, Operation<ItemStack> operation) {
        ItemStack removed = operation.call(inventory, fullStack);
        if (!removed.isEmpty() && Listeners.ON_ITEM_DROPPED.hasInstancesListening()) {
            ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(removed.getItem());
            String itemKey = itemLocation != null ? itemLocation.toString() : null;
            Listeners.ON_ITEM_DROPPED.onItemDropped(itemKey);
        }
        return removed;
    }

    @Unique
    private void updateDimensionListener_FancyMenu(LocalPlayer self) {
        boolean tracking = Listeners.ON_DIMENSION_ENTERED.hasInstancesListening();
        if (!tracking) {
            this.dimensionTransition_FancyMenu.observe(false, null);
            return;
        }
        if (self.level() == null) return;
        ResourceKey<Level> currentDimensionKey = self.level().dimension();
        DormantTransitionTracker.Phase phase = this.dimensionTransition_FancyMenu.observe(true, currentDimensionKey);
        if ((phase == DormantTransitionTracker.Phase.INITIAL || phase == DormantTransitionTracker.Phase.CHANGED) && currentDimensionKey != null) {
            Listeners.ON_DIMENSION_ENTERED.onDimensionEntered(currentDimensionKey);
        }
    }

    @Unique
    private void updateBurningListeners_FancyMenu(LocalPlayer self) {
        boolean trackStarted = Listeners.ON_STARTED_BURNING.hasInstancesListening();
        boolean trackStopped = Listeners.ON_STOPPED_BURNING.hasInstancesListening();
        boolean isBurning = self.isOnFire();
        DormantTransitionTracker.Phase phase = this.burningTransition_FancyMenu.observe(trackStarted || trackStopped, isBurning);
        if (phase == DormantTransitionTracker.Phase.INITIAL && isBurning && trackStarted) {
            Listeners.ON_STARTED_BURNING.onStartedBurning();
        } else if (phase == DormantTransitionTracker.Phase.CHANGED) {
            if (isBurning && trackStarted) Listeners.ON_STARTED_BURNING.onStartedBurning();
            else if (!isBurning && trackStopped) Listeners.ON_STOPPED_BURNING.onStoppedBurning();
        }
    }

    @Unique
    private void updateDrowningListenerState_FancyMenu(LocalPlayer self) {
        if (!Listeners.ON_STARTED_DROWNING.hasInstancesListening()) {
            this.drowningEpisodeTracker_FancyMenu.deactivate();
            return;
        }
        this.prepareDrowningTracking_FancyMenu(self);
        if (self.getAirSupply() >= self.getMaxAirSupply()) {
            this.drowningEpisodeTracker_FancyMenu.recover();
        }
    }

    @Override
    public boolean beginDrowningEpisode_FancyMenu() {
        this.prepareDrowningTracking_FancyMenu((LocalPlayer)(Object)this);
        return this.drowningEpisodeTracker_FancyMenu.beginEpisode();
    }

    @Unique
    private void updateFreezingListeners_FancyMenu(LocalPlayer self) {
        boolean trackFreezing = Listeners.ON_STARTED_FREEZING.hasInstancesListening() || Listeners.ON_STOPPED_FREEZING.hasInstancesListening();
        boolean trackFullyFrozen = Listeners.ON_FULLY_FROZEN.hasInstancesListening();
        if (!trackFreezing && !trackFullyFrozen) {
            this.freezingTransition_FancyMenu.observe(false, false);
            this.fullyFrozenTransition_FancyMenu.observe(false, false);
            return;
        }
        boolean trackStartedFreezing = Listeners.ON_STARTED_FREEZING.hasInstancesListening();
        boolean trackStoppedFreezing = Listeners.ON_STOPPED_FREEZING.hasInstancesListening();
        int ticksFrozen = self.getTicksFrozen();
        int ticksRequiredToFreeze = self.getTicksRequiredToFreeze();
        boolean isFreezing = ticksFrozen > 0;
        float freezingIntensity = ticksRequiredToFreeze > 0 ? Mth.clamp((float)ticksFrozen / (float)ticksRequiredToFreeze, 0.0F, 1.0F) : 0.0F;
        DormantTransitionTracker.Phase freezingPhase = this.freezingTransition_FancyMenu.observe(trackFreezing, isFreezing);
        if (freezingPhase == DormantTransitionTracker.Phase.INITIAL && isFreezing && trackStartedFreezing) {
            Listeners.ON_STARTED_FREEZING.onStartedFreezing(freezingIntensity);
        } else if (freezingPhase == DormantTransitionTracker.Phase.CHANGED) {
            if (isFreezing && trackStartedFreezing) Listeners.ON_STARTED_FREEZING.onStartedFreezing(freezingIntensity);
            else if (!isFreezing && trackStoppedFreezing) Listeners.ON_STOPPED_FREEZING.onStoppedFreezing();
        }

        boolean isFullyFrozen = ticksRequiredToFreeze > 0 && ticksFrozen >= ticksRequiredToFreeze;
        DormantTransitionTracker.Phase fullyFrozenPhase = this.fullyFrozenTransition_FancyMenu.observe(trackFullyFrozen, isFullyFrozen);
        if ((fullyFrozenPhase == DormantTransitionTracker.Phase.INITIAL || fullyFrozenPhase == DormantTransitionTracker.Phase.CHANGED) && isFullyFrozen) Listeners.ON_FULLY_FROZEN.onFullyFrozen();
    }

    @Unique
    private void updateRunningListeners_FancyMenu(LocalPlayer self) {
        boolean trackStarted = Listeners.ON_STARTED_RUNNING.hasInstancesListening();
        boolean trackStopped = Listeners.ON_STOPPED_RUNNING.hasInstancesListening();
        boolean isRunning = self.isSprinting();
        DormantTransitionTracker.Phase phase = this.runningTransition_FancyMenu.observe(trackStarted || trackStopped, isRunning);
        if (phase == DormantTransitionTracker.Phase.INITIAL && isRunning && trackStarted) {
            Listeners.ON_STARTED_RUNNING.onStartedRunning();
        } else if (phase == DormantTransitionTracker.Phase.CHANGED) {
            if (isRunning && trackStarted) Listeners.ON_STARTED_RUNNING.onStartedRunning();
            else if (!isRunning && trackStopped) Listeners.ON_STOPPED_RUNNING.onStoppedRunning();
        }
    }

    @Unique
    private void updateBiomeListeners_FancyMenu(LocalPlayer self) {
        boolean trackEntering = Listeners.ON_ENTER_BIOME.hasInstancesListening();
        boolean trackLeaving = Listeners.ON_LEAVE_BIOME.hasInstancesListening();
        if (!trackEntering && !trackLeaving) {
            this.biomeTransition_FancyMenu.observe(false, null);
            return;
        }
        ResourceKey<Biome> currentBiomeKey = null;
        if (self.level() instanceof ClientLevel clientLevel && clientLevel.hasChunkAt(self.getBlockX(), self.getBlockZ())) {
            Holder<Biome> biomeHolder = clientLevel.getBiome(self.blockPosition());
            currentBiomeKey = biomeHolder.unwrapKey().orElse(null);
        }
        DormantTransitionTracker.Phase phase = this.biomeTransition_FancyMenu.observe(true, currentBiomeKey);
        if (phase == DormantTransitionTracker.Phase.INITIAL) {
            if (trackEntering) Listeners.ON_ENTER_BIOME.onBiomeChanged(currentBiomeKey);
        } else if (phase == DormantTransitionTracker.Phase.CHANGED) {
            ResourceKey<Biome> previousBiomeKey = this.biomeTransition_FancyMenu.previousValue();
            if (trackLeaving && previousBiomeKey != null) Listeners.ON_LEAVE_BIOME.onBiomeLeft(previousBiomeKey);
            if (trackEntering) Listeners.ON_ENTER_BIOME.onBiomeChanged(currentBiomeKey);
        }
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
            this.touchingTransition_FancyMenu.observe(false, false);
            this.swimmingTransition_FancyMenu.observe(false, false);
            this.lastTouchingFluidKey_FancyMenu = null;
            this.lastSwimmingFluidKey_FancyMenu = null;
            return;
        }

        FluidContactInfo contactInfo = this.detectFluidContact_FancyMenu(self);
        boolean isTouchingFluid = contactInfo.touching();
        String currentFluidKey = contactInfo.fluidKey();
        boolean trackStartedTouching = Listeners.ON_START_TOUCHING_FLUID.hasInstancesListening();
        boolean trackStoppedTouching = Listeners.ON_STOP_TOUCHING_FLUID.hasInstancesListening();

        DormantTransitionTracker.Phase touchingPhase = this.touchingTransition_FancyMenu.observe(trackTouching, isTouchingFluid);
        if (touchingPhase == DormantTransitionTracker.Phase.INITIAL && isTouchingFluid && trackStartedTouching) {
            Listeners.ON_START_TOUCHING_FLUID.onStartTouchingFluid(currentFluidKey);
        } else if (touchingPhase == DormantTransitionTracker.Phase.CHANGED) {
            if (isTouchingFluid && trackStartedTouching) Listeners.ON_START_TOUCHING_FLUID.onStartTouchingFluid(currentFluidKey);
            else if (!isTouchingFluid && trackStoppedTouching) Listeners.ON_STOP_TOUCHING_FLUID.onStopTouchingFluid(this.lastTouchingFluidKey_FancyMenu);
        }
        this.lastTouchingFluidKey_FancyMenu = trackTouching && isTouchingFluid ? currentFluidKey : null;

        boolean isSwimming = self.isSwimming();
        String swimmingFluidKey = isSwimming ? currentFluidKey : null;
        boolean trackStartedSwimming = Listeners.ON_START_SWIMMING.hasInstancesListening();
        boolean trackStoppedSwimming = Listeners.ON_STOP_SWIMMING.hasInstancesListening();

        DormantTransitionTracker.Phase swimmingPhase = this.swimmingTransition_FancyMenu.observe(trackSwimming, isSwimming);
        if (swimmingPhase == DormantTransitionTracker.Phase.INITIAL && isSwimming && trackStartedSwimming) {
            Listeners.ON_START_SWIMMING.onStartSwimming(swimmingFluidKey);
        } else if (swimmingPhase == DormantTransitionTracker.Phase.CHANGED) {
            if (isSwimming && trackStartedSwimming) Listeners.ON_START_SWIMMING.onStartSwimming(swimmingFluidKey);
            else if (!isSwimming && trackStoppedSwimming) Listeners.ON_STOP_SWIMMING.onStopSwimming(this.lastSwimmingFluidKey_FancyMenu);
        }
        this.lastSwimmingFluidKey_FancyMenu = trackSwimming && isSwimming ? swimmingFluidKey : null;
    }

    @Unique
    private void updatePositionChangedListener_FancyMenu(LocalPlayer self) {
        if (!Listeners.ON_POSITION_CHANGED.hasInstancesListening()) {
            this.positionTransition_FancyMenu.observe(false, null);
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
        DormantTransitionTracker.Phase phase = this.positionTransition_FancyMenu.observe(true, immutablePos);
        BlockPos previousPos = this.positionTransition_FancyMenu.previousValue();
        if (phase == DormantTransitionTracker.Phase.CHANGED && previousPos != null) Listeners.ON_POSITION_CHANGED.onPositionChanged(previousPos, immutablePos);
    }

    @Unique
    private void updateSteppingListener_FancyMenu(LocalPlayer self) {
        if (!Listeners.ON_STEPPING_ON_BLOCK.hasInstancesListening()) {
            this.steppingTransition_FancyMenu.observe(false, null);
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
        DormantTransitionTracker.Phase phase = this.steppingTransition_FancyMenu.observe(true, immutablePos);
        if (phase == DormantTransitionTracker.Phase.INITIAL || phase == DormantTransitionTracker.Phase.CHANGED) Listeners.ON_STEPPING_ON_BLOCK.onSteppedOnBlock(immutablePos, blockState);
    }

    @Unique
    private void updateRidingListeners_FancyMenu(LocalPlayer self) {
        boolean trackMounted = Listeners.ON_ENTITY_MOUNTED.hasInstancesListening();
        boolean trackUnmounted = Listeners.ON_ENTITY_UNMOUNTED.hasInstancesListening();
        Entity vehicle = self.getVehicle();
        boolean isRiding = vehicle != null;
        DormantTransitionTracker.Phase phase = this.ridingTransition_FancyMenu.observe(trackMounted || trackUnmounted, isRiding);
        if (phase == DormantTransitionTracker.Phase.DISABLED) {
            this.clearMountedEntityCache_FancyMenu();
            return;
        }
        if (phase == DormantTransitionTracker.Phase.INITIAL && isRiding) {
            this.updateMountedEntityCache_FancyMenu(vehicle);
            if (trackMounted) Listeners.ON_ENTITY_MOUNTED.onEntityMounted(vehicle);
        } else if (phase == DormantTransitionTracker.Phase.REACTIVATED && isRiding) {
            this.updateMountedEntityCache_FancyMenu(vehicle);
        } else if (phase == DormantTransitionTracker.Phase.CHANGED) {
            if (isRiding) {
                this.updateMountedEntityCache_FancyMenu(vehicle);
                if (trackMounted) Listeners.ON_ENTITY_MOUNTED.onEntityMounted(vehicle);
            } else if (trackUnmounted) {
                this.fireEntityUnmountedListener_FancyMenu();
            }
        } else if (isRiding) {
            this.updateMountedEntityCache_FancyMenu(vehicle);
        }
        if (!isRiding) this.clearMountedEntityCache_FancyMenu();
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
        this.steppingTransition_FancyMenu.resetBaseline();
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

    @Unique
    private String resolveEffectTypeName_FancyMenu(MobEffect effect) {
        MobEffectCategory category = effect.getCategory();
        return switch (category) {
            case BENEFICIAL -> "positive";
            case HARMFUL -> "negative";
            case NEUTRAL -> "neutral";
        };
    }

    @Unique
    private String resolveEffectKey_FancyMenu(MobEffect effect) {
        ResourceLocation effectLocation = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return effectLocation != null ? effectLocation.toString() : "unknown";
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

    @Unique
    private void updateWeatherListener_FancyMenu(LocalPlayer self, @Nullable ClientLevel clientLevel) {
        boolean tracking = Listeners.ON_WEATHER_CHANGED.hasInstancesListening();
        if (!tracking || clientLevel == null) {
            if (!tracking) this.weatherTrackingWasDormant_FancyMenu = true;
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
            Biome.Precipitation precipitation = biome.getPrecipitationAt(playerPos);
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

    @Unique
    private void prepareDrowningTracking_FancyMenu(LocalPlayer self) {
        if (!this.drowningEpisodeTracker_FancyMenu.needsPreparation()) return;
        DamageSource lastDamageSource = self.getLastDamageSource();
        // A recent vanilla drowning hit seeds an already-running episode silently when tracking resumes.
        boolean alreadyDrowning = self.getAirSupply() < self.getMaxAirSupply() && lastDamageSource != null && lastDamageSource.is(DamageTypes.DROWN);
        this.drowningEpisodeTracker_FancyMenu.prepare(alreadyDrowning);
    }

}

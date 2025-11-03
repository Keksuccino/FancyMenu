package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtBlockListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtEntityListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStopLookingAtBlockListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStopLookingAtEntityListener;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Unique
    private static final double ENTITY_LOOK_DISTANCE_FANCYMENU = 20.0D;
    @Unique
    private static final double BLOCK_LOOK_DISTANCE_FANCYMENU = OnStartLookingAtBlockListener.MAX_LOOK_DISTANCE;

    @Shadow @Final Minecraft minecraft;

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo info) {
        ScreenCustomization.onPreGameRenderTick();
    }

    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void tail_onPick_FancyMenu(float partialTicks, CallbackInfo info) {

        if (this.minecraft == null) {
            return;
        }

        HitResult hitResult = this.minecraft.hitResult;
        OnStartLookingAtBlockListener startBlockListener = Listeners.ON_START_LOOKING_AT_BLOCK;
        OnStopLookingAtBlockListener stopBlockListener = Listeners.ON_STOP_LOOKING_AT_BLOCK;
        OnStartLookingAtEntityListener startLookingListener = Listeners.ON_START_LOOKING_AT_ENTITY;
        OnStopLookingAtEntityListener stopLookingListener = Listeners.ON_STOP_LOOKING_AT_ENTITY;

        if (hitResult == null) {
            stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
            stopLooking_FancyMenu(startLookingListener, stopLookingListener);
            return;
        }

        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null) {
            stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
            stopLooking_FancyMenu(startLookingListener, stopLookingListener);
            return;
        }

        Vec3 eyePosition = cameraEntity.getEyePosition(partialTicks);

        EntityHitResult extendedEntityHit = findExtendedEntityHit_FancyMenu(cameraEntity, partialTicks);

        if (extendedEntityHit == null && hitResult instanceof EntityHitResult vanillaEntityHit) {
            extendedEntityHit = vanillaEntityHit;
        }

        if (extendedEntityHit != null) {
            Entity targetEntity = extendedEntityHit.getEntity();
            double distance = extendedEntityHit.getLocation().distanceTo(eyePosition);
            startLookingListener.onLookAtEntity(targetEntity, distance);
            stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
            return;
        }

        stopLooking_FancyMenu(startLookingListener, stopLookingListener);

        ClientLevel clientLevel = this.minecraft.level;

        if (clientLevel == null) {
            stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
            return;
        }

        HitResult blockPickResult = cameraEntity.pick(BLOCK_LOOK_DISTANCE_FANCYMENU, partialTicks, false);
        if (!(blockPickResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() != HitResult.Type.BLOCK) {
            stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
            return;
        }

        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState blockState = clientLevel.getBlockState(blockPos);
        if (blockState.isAir()) {
            stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
            return;
        }

        double distance = blockHitResult.getLocation().distanceTo(eyePosition);
        if (distance > BLOCK_LOOK_DISTANCE_FANCYMENU) {
            stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
            return;
        }

        OnStartLookingAtBlockListener.LookedBlockData previousBlock = startBlockListener.getCurrentBlockData();
        if (previousBlock != null) {
            boolean sameBlock = previousBlock.blockPos().equals(blockPos)
                && previousBlock.blockState().equals(blockState)
                && previousBlock.levelKey().equals(clientLevel.dimension());
            if (!sameBlock) {
                stopBlockListener.onStopLooking(previousBlock);
            }
        }

        startBlockListener.onLookAtBlock(clientLevel, blockHitResult, distance);

    }

    @Unique
    private static void stopLooking_FancyMenu(OnStartLookingAtEntityListener startListener, OnStopLookingAtEntityListener stopListener) {
        OnStartLookingAtEntityListener.LookedEntityData previousEntity = startListener.getCurrentEntityData();
        if (previousEntity != null) {
            stopListener.onStopLooking(previousEntity);
            startListener.clearCurrentEntity();
        }
    }

    @Unique
    private static void stopLookingBlock_FancyMenu(OnStartLookingAtBlockListener startListener, OnStopLookingAtBlockListener stopListener) {
        OnStartLookingAtBlockListener.LookedBlockData previousBlock = startListener.getCurrentBlockData();
        if (previousBlock != null) {
            stopListener.onStopLooking(previousBlock);
        }
        startListener.clearCurrentBlock();
    }

    @Nullable
    @Unique
    private static EntityHitResult findExtendedEntityHit_FancyMenu(Entity cameraEntity, float partialTicks) {
        Vec3 eyePosition = cameraEntity.getEyePosition(partialTicks);
        Vec3 viewVector = cameraEntity.getViewVector(partialTicks);
        Vec3 reachVector = eyePosition.add(viewVector.scale(ENTITY_LOOK_DISTANCE_FANCYMENU));
        AABB searchBox = cameraEntity.getBoundingBox().expandTowards(viewVector.scale(ENTITY_LOOK_DISTANCE_FANCYMENU)).inflate(1.0D);
        double maxDistanceSqr = ENTITY_LOOK_DISTANCE_FANCYMENU * ENTITY_LOOK_DISTANCE_FANCYMENU;

        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
            cameraEntity,
            eyePosition,
            reachVector,
            searchBox,
            entity -> !entity.isSpectator() && entity.isPickable(),
            maxDistanceSqr
        );

        if (entityHitResult == null) {
            return null;
        }

        Vec3 hitLocation = entityHitResult.getLocation();
        double entityDistanceSqr = hitLocation.distanceToSqr(eyePosition);
        if (entityDistanceSqr > maxDistanceSqr) {
            return null;
        }

        HitResult blockHitResult = cameraEntity.pick(ENTITY_LOOK_DISTANCE_FANCYMENU, partialTicks, false);
        if (blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS) {
            double blockDistanceSqr = blockHitResult.getLocation().distanceToSqr(eyePosition);
            if (blockDistanceSqr <= entityDistanceSqr) {
                return null;
            }
        }

        return entityHitResult;
    }

}

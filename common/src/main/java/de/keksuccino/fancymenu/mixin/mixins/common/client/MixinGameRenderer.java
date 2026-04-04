package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtBlockListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtEntityListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStopLookingAtBlockListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStopLookingAtEntityListener;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.DeltaTracker;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Unique
    private static final double ENTITY_LOOK_DISTANCE_FANCYMENU = 20.0D;
    @Unique
    private static final double BLOCK_LOOK_DISTANCE_FANCYMENU = OnStartLookingAtBlockListener.MAX_LOOK_DISTANCE;

    @Shadow @Final Minecraft minecraft;

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void head_processBlurEffect_FancyMenu(CallbackInfo info) {
        if (RenderingUtils.isMenuBlurringBlocked()) info.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(DeltaTracker $$0, boolean $$1, CallbackInfo info) {
        ScreenCustomization.onPreGameRenderTick();
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

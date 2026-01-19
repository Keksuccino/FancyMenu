package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", at = @At("HEAD"))
    private void before_renderLevel_FancyMenu(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f frustumMatrix, CallbackInfo info) {
        Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.onRenderFrameStart();
    }

    @WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private void wrap_renderEntity_FancyMenu(LevelRenderer levelRenderer, Entity entity, double cameraX, double cameraY, double cameraZ, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, Operation<Void> original) {
        if (Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.shouldCheckVisibility()) {
            double interpolatedX = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double interpolatedY = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double interpolatedZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            Vec3 entityPosition = new Vec3(interpolatedX, interpolatedY, interpolatedZ);
            Vec3 cameraPosition = new Vec3(cameraX, cameraY, cameraZ);
            if (isEntityVisibleForListener_FancyMenu(entity, cameraPosition, entityPosition)) {
                double distance = entityPosition.distanceTo(cameraPosition);
                Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.onEntityVisible(entity, distance);
            }
        }
        original.call(levelRenderer, entity, cameraX, cameraY, cameraZ, partialTicks, poseStack, bufferSource);
    }

    @Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", at = @At("TAIL"))
    private void after_renderLevel_FancyMenu(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, Matrix4f frustumMatrix, CallbackInfo info) {
        Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.onRenderFrameEnd();
    }

    @Unique
    private static boolean isEntityVisibleForListener_FancyMenu(Entity entity, Vec3 cameraPosition, Vec3 entityPosition) {
        double distanceToEntitySqr = entityPosition.distanceToSqr(cameraPosition);
        if (distanceToEntitySqr > 40000.0D) {
            return false;
        }
        double distanceToEntity = Math.sqrt(distanceToEntitySqr);

        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity == null) {
            return true;
        }
        if (entity == cameraEntity) {
            return false;
        }

        if (distanceToEntity <= 1.0E-8) {
            return false;
        }

        BlockPos cameraBlockPos = BlockPos.containing(cameraPosition);
        BlockState cameraBlockState = entity.level().getBlockState(cameraBlockPos);
        if (cameraBlockState.canOcclude()) {
            return false;
        }

        Vec3 direction = entityPosition.subtract(cameraPosition);
        Vec3 start = cameraPosition.add(direction.normalize().scale(1.0E-3D));

        BlockHitResult hitResult = entity.level().clip(new ClipContext(start, entityPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cameraEntity));
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return true;
        }

        double blockDistance = hitResult.getLocation().distanceTo(cameraPosition);
        if (blockDistance >= distanceToEntity - 1.0E-4) {
            return true;
        }

        BlockState blockState = entity.level().getBlockState(hitResult.getBlockPos());
        return !blockState.canOcclude();
    }

}

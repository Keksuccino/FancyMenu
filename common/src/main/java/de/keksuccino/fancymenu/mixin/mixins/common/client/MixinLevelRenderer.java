package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.mixin.support.client.EntityVisibilityRaycastCache;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Unique private final EntityVisibilityRaycastCache entityVisibilityRaycastCache_FancyMenu = new EntityVisibilityRaycastCache();
    @Unique private boolean trackEntityVisibility_FancyMenu;

    @Inject(method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", at = @At("HEAD"))
    private void before_renderLevel_FancyMenu(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix, Matrix4f cullingProjectionMatrix, GpuBufferSlice shaderFog, Vector4f fogColor, boolean renderSky, CallbackInfo info) {
        this.trackEntityVisibility_FancyMenu = Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.onRenderFrameStart();
        if (!this.trackEntityVisibility_FancyMenu) {
            this.entityVisibilityRaycastCache_FancyMenu.reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        ClientLevel level = minecraft.level;
        if (cameraEntity == null || level == null) {
            this.entityVisibilityRaycastCache_FancyMenu.reset();
            return;
        }

        Item mainHandItem = Items.AIR;
        Item feetItem = Items.AIR;
        if (cameraEntity instanceof LivingEntity livingEntity) {
            mainHandItem = livingEntity.getMainHandItem().getItem();
            feetItem = livingEntity.getItemBySlot(EquipmentSlot.FEET).getItem();
        }
        Vec3 cameraPosition = camera.position();
        this.entityVisibilityRaycastCache_FancyMenu.beginFrame(level, level.getGameTime(), cameraEntity, cameraPosition.x, cameraPosition.y, cameraPosition.z, cameraEntity.getY(), cameraEntity.isDescending(), cameraEntity.fallDistance, mainHandItem, feetItem);
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void before_setLevel_FancyMenu(@Nullable ClientLevel level, CallbackInfo info) {
        this.entityVisibilityRaycastCache_FancyMenu.reset();
    }

    @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private EntityRenderState wrap_extractEntity_FancyMenu(LevelRenderer levelRenderer, Entity entity, float partialTicks, Operation<EntityRenderState> original) {
        if (this.trackEntityVisibility_FancyMenu && !Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.shouldCheckVisibility()) {
            Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.resetTrackingForDormancy();
            this.entityVisibilityRaycastCache_FancyMenu.reset();
            this.trackEntityVisibility_FancyMenu = false;
        }
        if (this.trackEntityVisibility_FancyMenu) {
            double interpolatedX = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double interpolatedY = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double interpolatedZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            Vec3 entityPosition = new Vec3(interpolatedX, interpolatedY, interpolatedZ);
            Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().position();
            if (isEntityVisibleForListener_FancyMenu(entity, cameraPosition, entityPosition)) {
                double distance = entityPosition.distanceTo(cameraPosition);
                Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.onEntityVisible(entity, distance);
            }
        }
        return original.call(levelRenderer, entity, partialTicks);
    }

    @Inject(method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", at = @At("TAIL"))
    private void after_renderLevel_FancyMenu(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f frustumMatrix, Matrix4f projectionMatrix, Matrix4f cullingProjectionMatrix, GpuBufferSlice shaderFog, Vector4f fogColor, boolean renderSky, CallbackInfo info) {
        if (this.trackEntityVisibility_FancyMenu && !Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.shouldCheckVisibility()) {
            Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.resetTrackingForDormancy();
            this.entityVisibilityRaycastCache_FancyMenu.reset();
            this.trackEntityVisibility_FancyMenu = false;
        }
        if (this.trackEntityVisibility_FancyMenu) {
            Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.onRenderFrameEnd();
            this.trackEntityVisibility_FancyMenu = false;
        }
    }

    @Unique
    private boolean isEntityVisibleForListener_FancyMenu(Entity entity, Vec3 cameraPosition, Vec3 entityPosition) {
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

        UUID entityUuid = entity.getUUID();
        int entityId = entity.getId();
        Boolean cachedVisibility = this.entityVisibilityRaycastCache_FancyMenu.getCachedVisibility(entity, entityUuid, entityId, entityPosition.x, entityPosition.y, entityPosition.z);
        if (cachedVisibility != null) return cachedVisibility;

        boolean visible = raycastEntityVisibility_FancyMenu(entity, cameraEntity, cameraPosition, entityPosition, distanceToEntity);
        this.entityVisibilityRaycastCache_FancyMenu.store(entity, entityUuid, entityId, entityPosition.x, entityPosition.y, entityPosition.z, visible);
        return visible;
    }

    @Unique
    private static boolean raycastEntityVisibility_FancyMenu(Entity entity, Entity cameraEntity, Vec3 cameraPosition, Vec3 entityPosition, double distanceToEntity) {
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

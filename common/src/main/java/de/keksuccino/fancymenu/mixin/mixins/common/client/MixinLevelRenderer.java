package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class MixinLevelRenderer {

    @Inject(method = "extract", at = @At("HEAD"))
    private void before_extract_FancyMenu(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo info) {
        Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.onRenderFrameStart();
    }

    @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
	private EntityRenderState wrap_extractEntity_FancyMenu(LevelExtractor levelExtractor, Entity entity, float partialTicks, Operation<EntityRenderState> original, Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState) {
		if (Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.shouldCheckVisibility()) {
			double interpolatedX = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double interpolatedY = Mth.lerp(partialTicks, entity.yo, entity.getY());
            double interpolatedZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            Vec3 entityPosition = new Vec3(interpolatedX, interpolatedY, interpolatedZ);
            Vec3 cameraPosition = camera.position();
            if (isEntityVisibleForListener_FancyMenu(entity, cameraPosition, entityPosition)) {
                double distance = entityPosition.distanceTo(cameraPosition);
                Listeners.ON_ENTITY_STARTS_BEING_IN_SIGHT.onEntityVisible(entity, distance);
            }
        }
        return original.call(levelExtractor, entity, partialTicks);
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void after_extract_FancyMenu(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo info) {
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

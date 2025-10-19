package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtEntityListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStopLookingAtEntityListener;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow @Final Minecraft minecraft;

    @Inject(method = "render", at = @At("HEAD"))
    private void before_render_FancyMenu(DeltaTracker $$0, boolean $$1, CallbackInfo info) {
        ScreenCustomization.onPreGameRenderTick();
    }

    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void tail_onPick_FancyMenu(float partialTicks, CallbackInfo info) {

        if (this.minecraft == null) {
            return;
        }

        HitResult hitResult = this.minecraft.hitResult;
        OnStartLookingAtEntityListener startLookingListener = Listeners.ON_START_LOOKING_AT_ENTITY;
        OnStopLookingAtEntityListener stopLookingListener = Listeners.ON_STOP_LOOKING_AT_ENTITY;

        if (hitResult == null) {
            Listeners.ON_LOOKING_AT_BLOCK.onStopLooking();
            OnStartLookingAtEntityListener.LookedEntityData previousEntity = startLookingListener.getCurrentEntityData();
            if (previousEntity != null) {
                stopLookingListener.onStopLooking(previousEntity);
                startLookingListener.clearCurrentEntity();
            }
            return;
        }

        Entity cameraEntity = this.minecraft.getCameraEntity();
        if (cameraEntity == null) {
            Listeners.ON_LOOKING_AT_BLOCK.onStopLooking();
            OnStartLookingAtEntityListener.LookedEntityData previousEntity = startLookingListener.getCurrentEntityData();
            if (previousEntity != null) {
                stopLookingListener.onStopLooking(previousEntity);
                startLookingListener.clearCurrentEntity();
            }
            return;
        }

        Vec3 eyePosition = cameraEntity.getEyePosition(partialTicks);

        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity targetEntity = entityHitResult.getEntity();
            double distance = entityHitResult.getLocation().distanceTo(eyePosition);

            OnStartLookingAtEntityListener.LookedEntityData previousEntity = startLookingListener.getCurrentEntityData();
            if (previousEntity != null && !previousEntity.uuid().equals(targetEntity.getUUID())) {
                stopLookingListener.onStopLooking(previousEntity);
                startLookingListener.clearCurrentEntity();
            }

            startLookingListener.onLookAtEntity(targetEntity, distance);
            Listeners.ON_LOOKING_AT_BLOCK.onStopLooking();
            return;
        }

        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            Listeners.ON_LOOKING_AT_BLOCK.onStopLooking();
            OnStartLookingAtEntityListener.LookedEntityData previousEntity = startLookingListener.getCurrentEntityData();
            if (previousEntity != null) {
                stopLookingListener.onStopLooking(previousEntity);
                startLookingListener.clearCurrentEntity();
            }
            return;
        }

        if (!(this.minecraft.level instanceof ClientLevel clientLevel)) {
            Listeners.ON_LOOKING_AT_BLOCK.onStopLooking();
            OnStartLookingAtEntityListener.LookedEntityData previousEntity = startLookingListener.getCurrentEntityData();
            if (previousEntity != null) {
                stopLookingListener.onStopLooking(previousEntity);
                startLookingListener.clearCurrentEntity();
            }
            return;
        }

        double distance = blockHitResult.getLocation().distanceTo(eyePosition);
        Listeners.ON_LOOKING_AT_BLOCK.onLookAtBlock(clientLevel, blockHitResult, distance);
        OnStartLookingAtEntityListener.LookedEntityData previousEntity = startLookingListener.getCurrentEntityData();
        if (previousEntity != null) {
            stopLookingListener.onStopLooking(previousEntity);
            startLookingListener.clearCurrentEntity();
        }

    }

}

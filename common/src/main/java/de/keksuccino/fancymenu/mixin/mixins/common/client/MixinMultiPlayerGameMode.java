package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult wrap_interactWithEntity_FancyMenu(Player player, Entity target, InteractionHand hand, Operation<InteractionResult> original) {
        InteractionResult result = original.call(player, target, hand);
        this.handleEntityInteractionResult_FancyMenu(player, target, result);
        return result;
    }

    @WrapOperation(method = "interactAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult wrap_interactAtWithEntity_FancyMenu(Entity target, Player player, Vec3 hitVec, InteractionHand hand, Operation<InteractionResult> original) {
        InteractionResult result = original.call(target, player, hitVec, hand);
        this.handleEntityInteractionResult_FancyMenu(player, target, result);
        return result;
    }

    @Unique
    private void handleEntityInteractionResult_FancyMenu(Player player, Entity target, InteractionResult result) {
        if (!(player instanceof LocalPlayer) || target == null) {
            return;
        }
        if (result == null || !result.consumesAction()) {
            return;
        }
        Listeners.ON_INTERACTED_WITH_ENTITY.onEntityInteracted(target);
    }
}

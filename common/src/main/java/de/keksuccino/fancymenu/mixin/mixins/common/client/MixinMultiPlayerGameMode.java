package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    /** @reason Fire FancyMenu listener after the local player successfully breaks a block. */
    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;destroy(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void wrap_destroy_in_destroyBlock_FancyMenu(Block block, LevelAccessor level, BlockPos pos, BlockState state, Operation<Void> operation) {
        String usedItemKey = this.getMainHandItemKey_FancyMenu();
        operation.call(block, level, pos, state);
        if (level != null && level.isClientSide()) {
            Listeners.ON_BLOCK_BROKE.onBlockBroke(pos, state, usedItemKey);
        }
    }

    /** @reason Fire FancyMenu listener when the local player interacts with a block. */
    @Inject(method = "performUseItemOn", at = @At("RETURN"))
    private void after_performUseItemOn_FancyMenu(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if ((result != null) && result.consumesAction()) {
            Minecraft minecraft = Minecraft.getInstance();
            Level level = minecraft.level;
            if (level != null && level.isLoaded(hitResult.getBlockPos())) {
                BlockPos blockPos = hitResult.getBlockPos().immutable();
                BlockState state = level.getBlockState(blockPos);
                Listeners.ON_INTERACTED_WITH_BLOCK.onBlockInteracted(blockPos, state);
            }
        }
    }

    /** @reason Fire FancyMenu listener when the local player places a block. */
    @WrapOperation(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult wrap_useOn_FancyMenu(ItemStack stack, UseOnContext context, Operation<InteractionResult> operation) {
        BlockItem blockItem = (stack.getItem() instanceof BlockItem) ? (BlockItem)stack.getItem() : null;
        BlockPos placePos = null;
        if (blockItem != null) {
            placePos = new BlockPlaceContext(context).getClickedPos();
        }

        InteractionResult result = operation.call(stack, context);

        if ((blockItem != null) && (result != null) && result.consumesAction()) {
            Level level = context.getLevel();
            if ((level != null) && level.isClientSide() && (placePos != null)) {
                BlockState placedState = level.getBlockState(placePos);
                if (placedState.is(blockItem.getBlock())) {
                    Listeners.ON_BLOCK_PLACED.onBlockPlaced(placePos, placedState);
                }
            }
        }

        return result;
    }

    /** @reason Fire FancyMenu listener when the local player interacts with an entity. */
    @WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult wrap_interactWithEntity_FancyMenu(Player player, Entity target, InteractionHand hand, Operation<InteractionResult> original) {
        InteractionResult result = original.call(player, target, hand);
        this.handleEntityInteractionResult_FancyMenu(player, target, result);
        return result;
    }

    /** @reason Fire FancyMenu listener when the local player interacts with an entity at a precise location. */
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

    @Unique
    private String getMainHandItemKey_FancyMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            return null;
        }
        ItemStack stack = localPlayer.getMainHandItem();
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemLocation != null ? itemLocation.toString() : null;
    }
}

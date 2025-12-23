package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Unique
    @Nullable
    private String capturedItemUseKey_FancyMenu;

    @Unique
    @Nullable
    private String capturedUseItemKey_FancyMenu;

    /** @reason Fire FancyMenu listener after the local player successfully breaks a block. */
    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;destroy(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void wrap_destroy_in_destroyBlock_FancyMenu(Block block, LevelAccessor level, BlockPos pos, BlockState state, Operation<Void> operation) {
        String usedItemKey = this.getMainHandItemKey_FancyMenu();
        operation.call(block, level, pos, state);
        if (level != null && level.isClientSide()) {
            Listeners.ON_BLOCK_BROKE.onBlockBroke(pos, state, usedItemKey);
        }
    }

    /** @reason Capture the item key before the local player uses an item on a block. */
    @Inject(method = "performUseItemOn", at = @At("HEAD"))
    private void before_performUseItemOn_captureItem_FancyMenu(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        this.capturedItemUseKey_FancyMenu = this.resolveItemKeyFromHand_FancyMenu(player, hand);
    }

    /** @reason Fire FancyMenu listeners when the local player interacts with a block. */
    @Inject(method = "performUseItemOn", at = @At("RETURN"))
    private void after_performUseItemOn_FancyMenu(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if ((result != null) && result.consumesAction()) {
            Minecraft minecraft = Minecraft.getInstance();
            Level level = minecraft.level;
            BlockPos blockPos = hitResult.getBlockPos().immutable();
            BlockState state = null;
            String blockKey = "";
            String targetPosX = "-1";
            String targetPosY = "-1";
            String targetPosZ = "-1";

            if ((level != null) && level.isLoaded(blockPos)) {
                state = level.getBlockState(blockPos);
                blockKey = this.resolveBlockKey_FancyMenu(state);
                targetPosX = Integer.toString(blockPos.getX());
                targetPosY = Integer.toString(blockPos.getY());
                targetPosZ = Integer.toString(blockPos.getZ());
                Listeners.ON_INTERACTED_WITH_BLOCK.onBlockInteracted(blockPos, state);
            }

            String itemKey = this.capturedItemUseKey_FancyMenu;
            if (itemKey == null) {
                itemKey = this.resolveItemKeyFromHand_FancyMenu(player, hand);
            }

            Listeners.ON_ITEM_USED.onItemUsed(itemKey, "block", "", blockKey, targetPosX, targetPosY, targetPosZ);
        }

        this.capturedItemUseKey_FancyMenu = null;
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

    /** @reason Cache the item key before the local player uses an item without targeting a block or entity. */
    @Inject(method = "useItem", at = @At("HEAD"))
    private void before_useItem_captureItem_FancyMenu(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof LocalPlayer) {
            this.capturedUseItemKey_FancyMenu = this.resolveItemKeyFromHand_FancyMenu(player, hand);
        } else {
            this.capturedUseItemKey_FancyMenu = null;
        }
    }

    /** @reason Fire FancyMenu listener when the local player successfully uses an item without a direct target. */
    @Inject(method = "useItem", at = @At("RETURN"))
    private void after_useItem_FancyMenu(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if ((player instanceof LocalPlayer localPlayer) && (result != null) && result.consumesAction()) {
            String itemKey = this.capturedUseItemKey_FancyMenu;
            if (itemKey == null) {
                itemKey = this.resolveItemKeyFromHand_FancyMenu(player, hand);
            }
            String usedOnType = "none";
            String entityKey = "";
            String targetPosX = "-1";
            String targetPosY = "-1";
            String targetPosZ = "-1";
            if (localPlayer.isUsingItem()) {
                usedOnType = "self";
                entityKey = this.resolveEntityKey_FancyMenu(localPlayer);
                targetPosX = Double.toString(localPlayer.getX());
                targetPosY = Double.toString(localPlayer.getY());
                targetPosZ = Double.toString(localPlayer.getZ());
            }
            Listeners.ON_ITEM_USED.onItemUsed(itemKey, usedOnType, entityKey, "", targetPosX, targetPosY, targetPosZ);
        }
        this.capturedUseItemKey_FancyMenu = null;
    }

    /** @reason Fire FancyMenu listener when the local player interacts with an entity. */
    @WrapOperation(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult wrap_interactWithEntity_FancyMenu(Player player, Entity target, InteractionHand hand, Operation<InteractionResult> original) {
        String itemKey = this.resolveItemKeyFromHand_FancyMenu(player, hand);
        InteractionResult result = original.call(player, target, hand);
        if (itemKey == null) {
            itemKey = this.resolveItemKeyFromHand_FancyMenu(player, hand);
        }
        this.handleEntityInteractionResult_FancyMenu(player, target, result, itemKey);
        return result;
    }

    /** @reason Fire FancyMenu listener when the local player interacts with an entity at a precise location. */
    @WrapOperation(method = "interactAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult wrap_interactAtWithEntity_FancyMenu(Entity target, Player player, Vec3 hitVec, InteractionHand hand, Operation<InteractionResult> original) {
        String itemKey = this.resolveItemKeyFromHand_FancyMenu(player, hand);
        InteractionResult result = original.call(target, player, hitVec, hand);
        if (itemKey == null) {
            itemKey = this.resolveItemKeyFromHand_FancyMenu(player, hand);
        }
        this.handleEntityInteractionResult_FancyMenu(player, target, result, itemKey);
        return result;
    }

    @Unique
    private void handleEntityInteractionResult_FancyMenu(Player player, Entity target, InteractionResult result, @Nullable String itemKey) {
        if (!(player instanceof LocalPlayer) || target == null) {
            return;
        }
        if (result == null || !result.consumesAction()) {
            return;
        }
        String entityKey = this.resolveEntityKey_FancyMenu(target);
        Listeners.ON_ITEM_USED.onItemUsed(itemKey, "entity", entityKey, "",
                Double.toString(target.getX()),
                Double.toString(target.getY()),
                Double.toString(target.getZ()));
        Listeners.ON_INTERACTED_WITH_ENTITY.onEntityInteracted(target);
    }

    @Unique
    private String getMainHandItemKey_FancyMenu() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null) {
            return null;
        }
        return this.resolveItemKey_FancyMenu(localPlayer.getMainHandItem());
    }

    @Unique
    @Nullable
    private String resolveItemKeyFromHand_FancyMenu(@Nullable Player player, @Nullable InteractionHand hand) {
        if (player == null || hand == null) {
            return null;
        }
        return this.resolveItemKey_FancyMenu(player.getItemInHand(hand));
    }

    @Unique
    @Nullable
    private String resolveItemKey_FancyMenu(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Identifier itemLocation = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemLocation != null ? itemLocation.toString() : null;
    }

    @Unique
    private String resolveBlockKey_FancyMenu(@Nullable BlockState state) {
        if (state == null) {
            return "";
        }
        Identifier blockLocation = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockLocation != null ? blockLocation.toString() : "";
    }

    @Unique
    private String resolveEntityKey_FancyMenu(@Nullable Entity entity) {
        if (entity == null) {
            return "";
        }
        Identifier entityLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entityLocation != null ? entityLocation.toString() : "";
    }
}


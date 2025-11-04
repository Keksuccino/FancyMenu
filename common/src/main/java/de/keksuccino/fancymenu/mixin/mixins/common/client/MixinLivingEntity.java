package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Unique
    private ItemStack lastBrokenStack_FancyMenu = ItemStack.EMPTY;

    @Unique
    private String lastBrokenItemType_FancyMenu;

    /** @reason Fire FancyMenu listener when the local player gains a status effect. */
    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void after_onEffectAdded_FancyMenu(MobEffectInstance effectInstance, @Nullable Entity entity, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof LocalPlayer)) {
            return;
        }
        MobEffect effect = effectInstance.getEffect();
        String effectKey = this.resolveEffectKey_FancyMenu(effect);
        String effectType = this.resolveEffectTypeName_FancyMenu(effect);
        Listeners.ON_EFFECT_GAINED.onEffectGained(effectKey, effectType, effectInstance.getDuration());
    }

    /** @reason Fire FancyMenu listener when the local player finishes consuming an item. */
    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack wrap_finishUsingItem_FancyMenu(ItemStack stack, Level level, LivingEntity living, Operation<ItemStack> operation) {
        String itemKey = null;
        if (!stack.isEmpty()) {
            ResourceLocation itemLocation = Registry.ITEM.getKey(stack.getItem());
            if (itemLocation != null) {
                itemKey = itemLocation.toString();
            }
        }

        ItemStack result = operation.call(stack, level, living);

        if (itemKey != null && living instanceof LocalPlayer) {
            Listeners.ON_ITEM_CONSUMED.onItemConsumed(itemKey);
        }

        return result;
    }

    /** @reason Capture the item that is about to break for the local player. */
    @Inject(method = "handleEntityEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;breakItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void before_breakItem_FancyMenu(byte eventId, CallbackInfo ci) {
        this.captureBrokenItem_FancyMenu(eventId);
    }

    /** @reason Fire FancyMenu listener after the item break animation for the local player. */
    @Inject(method = "handleEntityEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;breakItem(Lnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.AFTER))
    private void after_breakItem_FancyMenu(byte eventId, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof LocalPlayer) || this.lastBrokenStack_FancyMenu.isEmpty()) {
            this.clearBrokenItemCache_FancyMenu();
            return;
        }

        ResourceLocation itemLocation = Registry.ITEM.getKey(this.lastBrokenStack_FancyMenu.getItem());
        String itemKey = itemLocation != null ? itemLocation.toString() : null;
        Listeners.ON_ITEM_BROKE.onItemBroke(itemKey, this.lastBrokenItemType_FancyMenu);
        this.clearBrokenItemCache_FancyMenu();
    }

    @Unique
    private void captureBrokenItem_FancyMenu(byte eventId) {
        this.clearBrokenItemCache_FancyMenu();

        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof LocalPlayer localPlayer)) {
            return;
        }

        EquipmentSlot slot = this.mapEquipmentSlot_FancyMenu(eventId);
        if (slot == null) {
            return;
        }

        ItemStack stack = localPlayer.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return;
        }

        this.lastBrokenStack_FancyMenu = stack.copy();
        this.lastBrokenItemType_FancyMenu = this.resolveItemType_FancyMenu(slot, stack);
    }

    @Unique
    private void clearBrokenItemCache_FancyMenu() {
        this.lastBrokenStack_FancyMenu = ItemStack.EMPTY;
        this.lastBrokenItemType_FancyMenu = null;
    }

    @Unique
    private EquipmentSlot mapEquipmentSlot_FancyMenu(byte eventId) {
        return switch (eventId) {
            case 47 -> EquipmentSlot.MAINHAND;
            case 48 -> EquipmentSlot.OFFHAND;
            case 49 -> EquipmentSlot.HEAD;
            case 50 -> EquipmentSlot.CHEST;
            case 51 -> EquipmentSlot.LEGS;
            case 52 -> EquipmentSlot.FEET;
            default -> null;
        };
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
        ResourceLocation effectLocation = Registry.MOB_EFFECT.getKey(effect);
        return effectLocation != null ? effectLocation.toString() : "unknown";
    }

    @Unique
    private String resolveItemType_FancyMenu(EquipmentSlot slot, ItemStack stack) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            return "armor";
        }
        if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && stack.isDamageableItem()) {
            return "tool";
        }
        return "other";
    }

}


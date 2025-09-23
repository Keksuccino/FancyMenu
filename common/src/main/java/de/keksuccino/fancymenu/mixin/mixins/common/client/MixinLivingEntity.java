package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    /** @reason Fire FancyMenu listener when the local player finishes consuming an item. */
    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack wrap_finishUsingItem_FancyMenu(ItemStack stack, Level level, LivingEntity living, Operation<ItemStack> operation) {
        String itemKey = null;
        if (!stack.isEmpty()) {
            ResourceLocation itemLocation = BuiltInRegistries.ITEM.getKey(stack.getItem());
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
}

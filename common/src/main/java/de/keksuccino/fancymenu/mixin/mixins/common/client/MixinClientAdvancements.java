package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.AdvancementUnlockTracker;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientAdvancements.class)
public abstract class MixinClientAdvancements {

    @Shadow @Final private AdvancementTree tree;

    @Unique private final AdvancementUnlockTracker advancementUnlockTracker_FancyMenu = new AdvancementUnlockTracker();

    /** @reason Hides achievement toasts when they are disabled in FancyMenu's Global Customizations. The only toast added in update() is the AdvancementToast of a newly completed achievement. */
    @WrapWithCondition(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;addToast(Lnet/minecraft/client/gui/components/toasts/Toast;)V"))
    private boolean wrap_addToast_in_update_FancyMenu(ToastManager instance, Toast toast) {
        return FancyMenu.getOptions().showAchievementToasts.getValue();
    }

    // Fires the "On Achievement Unlocked" listener, independent of whether vanilla shows a toast
    @Inject(method = "update", at = @At("RETURN"))
    private void after_update_FancyMenu(ClientboundUpdateAdvancementsPacket packet, CallbackInfo info) {
        this.advancementUnlockTracker_FancyMenu.onAdvancementsUpdated(packet, this.tree);
    }

}

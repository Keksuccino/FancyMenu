package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientAdvancements.class)
public abstract class MixinClientAdvancements {

    /** @reason Hides achievement toasts when they are disabled in FancyMenu's Global Customizations. The only toast added in update() is the AdvancementToast of a newly completed achievement. */
    @WrapWithCondition(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastComponent;addToast(Lnet/minecraft/client/gui/components/toasts/Toast;)V"))
    private boolean wrap_addToast_in_update_FancyMenu(ToastComponent instance, Toast toast) {
        return FancyMenu.getOptions().showAchievementToasts.getValue();
    }

}

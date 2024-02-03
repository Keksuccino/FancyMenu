package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueLabeledSwitchCycleButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.SwitchGrid;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

//TODO übernehmen
@Mixin(SwitchGrid.SwitchBuilder.class)
public class MixinSwitchGrid_SwitchBuilder {

    @Shadow @Final private Component label;

    @WrapOperation(method = "build", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CycleButton$Builder;create(IIIILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/CycleButton$OnValueChange;)Lnet/minecraft/client/gui/components/CycleButton;"))
    private CycleButton<Boolean> wrapCreateCycleButtonInBuild_FancyMenu(CycleButton.Builder<Boolean> instance, int p_168937_, int p_168938_, int p_168939_, int p_168940_, Component label, CycleButton.OnValueChange<Boolean> p_168942_, Operation<CycleButton<Boolean>> original) {

        CycleButton<Boolean> button = original.call(instance, p_168937_, p_168938_, p_168939_, p_168940_, label, p_168942_);
        ((UniqueLabeledSwitchCycleButton)button).setLabeledSwitchComponentLabel_FancyMenu(this.label);

        return button;

    }

}

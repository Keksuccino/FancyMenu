package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteIconButton.CenteredIcon.class)
public class MixinSpriteIconButton_CenteredIcon extends Button {

    // Dummy constructor
    private MixinSpriteIconButton_CenteredIcon() {
        super(0, 0, 0, 0, Component.empty(), button -> {}, DEFAULT_NARRATION);
    }

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void wrap_blitSprite_in_renderWidget_FancyMenu(GuiGraphics instance, ResourceLocation sprite, int i1, int i2, int i3, int i4, Operation<Void> original) {
        // Fix for making the icon of icon buttons react to alpha changes
        instance.setColor(1.0f, 1.0f, 1.0f, this.alpha);
        original.call(instance, sprite, i1, i2, i3, i4);
        instance.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

}

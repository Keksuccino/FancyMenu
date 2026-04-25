package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteIconButton.CenteredIcon.class)
public class MixinSpriteIconButton_CenteredIcon extends Button {

    // Dummy constructor
    private MixinSpriteIconButton_CenteredIcon() {
        super(0, 0, 0, 0, Component.empty(), button -> {}, DEFAULT_NARRATION);
    }

    @WrapOperation(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SpriteIconButton$CenteredIcon;renderSprite(Lnet/minecraft/client/gui/GuiGraphics;II)V"), require = 0)
    private void wrap_renderSprite_in_renderContents_FancyMenu(SpriteIconButton.CenteredIcon instance, GuiGraphics graphics, int x, int y, Operation<Void> original) {
        // Fix for making the icon of icon buttons react to alpha changes
        int previousColor = RenderingUtils.getShaderColor();
        RenderingUtils.setShaderColor(graphics, ARGB.white(this.alpha));
        original.call(instance, graphics, x, y);
        RenderingUtils.setShaderColor(graphics, previousColor);
    }

    @Override
    public void renderContents(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
    }

}

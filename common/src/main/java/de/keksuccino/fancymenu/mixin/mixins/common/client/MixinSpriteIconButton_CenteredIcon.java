package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CenteredIconButtonLabelResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteIconButton.CenteredIcon.class)
public class MixinSpriteIconButton_CenteredIcon extends Button {

    // Dummy constructor
    private MixinSpriteIconButton_CenteredIcon() {
        super(0, 0, 0, 0, Component.empty(), button -> {}, DEFAULT_NARRATION);
    }

    /**
     * @reason Centered icon buttons intentionally skip vanilla label rendering. The explicit FancyMenu label must replace the icon at this call so the vanilla background is preserved and clearing the label restores the icon.
     */
    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void wrap_blitSprite_in_renderWidget_FancyMenu(GuiGraphics instance, ResourceLocation sprite, int i1, int i2, int i3, int i4, Operation<Void> original) {
        CustomizableWidget widget = (CustomizableWidget)this;
        Component customLabel = CenteredIconButtonLabelResolver.resolve(widget.getCustomLabelFancyMenu(), widget.getHoverLabelFancyMenu(), this.isHoveredOrFocused(), this.visible, this.active);
        if (customLabel != null) {
            int labelColor = this.active ? 16777215 : 10526880;
            this.renderScrollingString(instance, Minecraft.getInstance().font, 2, labelColor | Mth.ceil(this.alpha * 255.0F) << 24);
            return;
        }

        // Fix for making the icon of icon buttons react to alpha changes
        instance.setColor(1.0f, 1.0f, 1.0f, this.alpha);
        original.call(instance, sprite, i1, i2, i3, i4);
        instance.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

}

package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.mixin.support.client.CenteredIconButtonLabelResolver;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpriteIconButton.CenteredIcon.class)
public abstract class MixinSpriteIconButton_CenteredIcon extends Button {

    // Dummy constructor
    private MixinSpriteIconButton_CenteredIcon() {
        super(0, 0, 0, 0, Component.empty(), button -> {}, DEFAULT_NARRATION);
    }

    /**
     * @reason Centered icon buttons never render their message, so explicit FancyMenu labels must replace the icon to behave like labels on other vanilla buttons.
     */
    @WrapOperation(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SpriteIconButton$CenteredIcon;renderSprite(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void wrap_renderSprite_in_renderContents_FancyMenu(SpriteIconButton.CenteredIcon instance, GuiGraphics graphics, int x, int y, Operation<Void> original) {
        CustomizableWidget widget = (CustomizableWidget)this;
        Component customLabel = CenteredIconButtonLabelResolver.selectCustomLabel(widget.getCustomLabelFancyMenu(), widget.getHoverLabelFancyMenu(), this.isHoveredOrFocused(), this.visible, this.active);
        if (customLabel != null) {
            Component activeLabel = this.active ? this.getMessage() : customLabel;
            Component renderedLabel = CenteredIconButtonLabelResolver.resolveRenderedLabel(customLabel, activeLabel, this.active);
            this.renderScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE), renderedLabel, 2);
            return;
        }

        // Fix for making the icon of icon buttons react to alpha changes
        int previousColor = RenderingUtils.getShaderColor();
        RenderingUtils.setShaderColor(graphics, ARGB.white(this.alpha));
        original.call(instance, graphics, x, y);
        RenderingUtils.setShaderColor(graphics, previousColor);
    }

}

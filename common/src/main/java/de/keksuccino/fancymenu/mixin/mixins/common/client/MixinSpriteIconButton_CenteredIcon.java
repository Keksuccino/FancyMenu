package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import javax.annotation.Nullable;

@Mixin(SpriteIconButton.CenteredIcon.class)
public abstract class MixinSpriteIconButton_CenteredIcon extends Button {

    // Dummy constructor
    private MixinSpriteIconButton_CenteredIcon() {
        super(0, 0, 0, 0, Component.empty(), button -> {}, DEFAULT_NARRATION);
    }

    /**
     * @reason Centered icon buttons never render their message, so explicit FancyMenu labels must replace the icon to behave like labels on other vanilla buttons.
     */
    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SpriteIconButton$CenteredIcon;extractSprite(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void wrap_extractSprite_in_extractContents_FancyMenu(SpriteIconButton.CenteredIcon instance, GuiGraphicsExtractor graphics, int x, int y, Operation<Void> original) {
        Component customLabel = this.getCustomLabelToRender_FancyMenu();
        if (customLabel != null) {
            Component renderedLabel = this.active ? this.getMessage() : AbstractWidget.WithInactiveMessage.defaultInactiveMessage(customLabel);
            this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), renderedLabel, 2);
            return;
        }

        // Fix for making the icon of icon buttons react to alpha changes
        int previousColor = RenderingUtils.getShaderColor();
        RenderingUtils.setShaderColor(graphics, ARGB.white(this.alpha));
        original.call(instance, graphics, x, y);
        RenderingUtils.setShaderColor(graphics, previousColor);
    }

    @Unique
    @Nullable
    private Component getCustomLabelToRender_FancyMenu() {
        CustomizableWidget widget = (CustomizableWidget)this;
        Component hoverLabel = widget.getHoverLabelFancyMenu();
        if (hoverLabel != null && this.isHoveredOrFocused() && this.visible && this.active) return hoverLabel;
        return widget.getCustomLabelFancyMenu();
    }

}

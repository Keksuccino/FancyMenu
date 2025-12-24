package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ImageButton.class)
public abstract class MixinImageButton {

	@WrapWithCondition(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private boolean wrapRenderTextureFancyMenu(GuiGraphics graphics, RenderPipeline $$0, Identifier $$1, int $$2, int $$3, int $$4, int $$5) {

		ImageButton button = (ImageButton)((Object)this);
		CustomizableWidget customizable = ((CustomizableWidget)(Object)this);

		//Render custom background if present
		boolean renderVanilla = customizable.renderCustomBackgroundFancyMenu(button, graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight());

		//Render custom labels if present
		Component label = button.isHoveredOrFocused() ? customizable.getHoverLabelFancyMenu() : customizable.getCustomLabelFancyMenu();
		if (!renderVanilla && label != null) {
			int labelColor = button.active ? 0xFFFFFF : 0xA0A0A0;
			customizable.renderScrollingWidgetLabel(button, label.copy().withStyle(style -> style.withColor(labelColor)), graphics, -1);
		}

		//If it should render the Vanilla background
		return renderVanilla;

	}
	
}

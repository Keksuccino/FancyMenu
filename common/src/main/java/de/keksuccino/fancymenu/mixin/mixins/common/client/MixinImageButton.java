package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ImageButton.class)
public abstract class MixinImageButton {

	@WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private boolean wrapRenderTextureFancyMenu(GuiGraphics graphics, RenderPipeline $$0, Identifier $$1, int $$2, int $$3, int $$4, int $$5) {

		ImageButton button = (ImageButton)((Object)this);
		CustomizableWidget customizable = ((CustomizableWidget)this);

		//Render custom background if present
		boolean renderVanilla = ((CustomizableWidget)this).renderCustomBackgroundFancyMenu(button, graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight());

		//Render custom labels if present
		if (!renderVanilla && (((customizable.getCustomLabelFancyMenu() != null) && !button.isHoveredOrFocused()) || ((customizable.getHoverLabelFancyMenu() != null) && button.isHoveredOrFocused()))) {
			int labelColor = button.active ? 16777215 : 10526880;
			button.renderString(graphics, Minecraft.getInstance().font, labelColor | Mth.ceil(((IMixinAbstractWidget)button).getAlphaFancyMenu() * 255.0F) << 24);
		}

		//If it should render the Vanilla background
		return renderVanilla;

	}
	
}

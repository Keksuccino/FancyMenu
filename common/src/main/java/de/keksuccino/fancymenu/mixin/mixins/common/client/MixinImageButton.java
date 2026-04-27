package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.opengl.GlStateManager;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(ImageButton.class)
public abstract class MixinImageButton {

	@Unique private int cachedShaderColor_FancyMenu = -1;

	@WrapWithCondition(method = "extractContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private boolean wrapRenderTextureFancyMenu(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {

		ImageButton button = (ImageButton)((Object)this);
		CustomizableWidget customizable = ((CustomizableWidget)(Object)this);

		//Render custom background if present
		boolean renderVanilla = customizable.renderCustomBackgroundFancyMenu(button, graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight());

		//Render custom labels if present
		Component label = button.isHoveredOrFocused() ? customizable.getHoverLabelFancyMenu() : customizable.getCustomLabelFancyMenu();
		if (!renderVanilla && label != null) {
			int labelColor = button.active ? 0xFFFFFF : 0xA0A0A0;
			int color = labelColor | Mth.ceil(((IMixinAbstractWidget)button).getAlphaFancyMenu() * 255.0F) << 24;
			graphics.centeredText(Minecraft.getInstance().font, label, button.getX() + button.getWidth() / 2, button.getY() + (button.getHeight() - 8) / 2, color);
		}

		cachedShaderColor_FancyMenu = RenderingUtils.getShaderColor();

		GlStateManager._enableBlend();
		//Fix missing alpha handling for ImageButtons (Vanilla bug)
		int baseColor = cachedShaderColor_FancyMenu == -1 ? 0xFFFFFFFF : cachedShaderColor_FancyMenu;
		RenderingUtils.setShaderColor(graphics, ARGB.color(((IMixinAbstractWidget)button).getAlphaFancyMenu(), baseColor));

		//If it should render the Vanilla background
		return renderVanilla;

	}

	@Inject(method = "extractContents", at = @At("RETURN"))
	private void afterRenderWidgetFancyMenu(GuiGraphicsExtractor graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
		//Reset shader color after alpha handling
		RenderingUtils.setShaderColor(graphics, cachedShaderColor_FancyMenu);
		cachedShaderColor_FancyMenu = -1;
	}
	
}

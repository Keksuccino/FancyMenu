package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@SuppressWarnings("unused")
@Mixin(ImageButton.class)
public abstract class MixinImageButton {

	@Unique private float[] cachedShaderColor_FancyMenu;

	@WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
	private boolean wrapRenderTextureFancyMenu(GuiGraphics graphics, Function<ResourceLocation, RenderType> $$0, ResourceLocation $$1, int $$2, int $$3, int $$4, int $$5) {

		ImageButton button = (ImageButton)((Object)this);
		CustomizableWidget customizable = ((CustomizableWidget)this);

		//Render custom background if present
		boolean renderVanilla = ((CustomizableWidget)this).renderCustomBackgroundFancyMenu(button, graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight());

		//Render custom labels if present
		if (!renderVanilla && (((customizable.getCustomLabelFancyMenu() != null) && !button.isHoveredOrFocused()) || ((customizable.getHoverLabelFancyMenu() != null) && button.isHoveredOrFocused()))) {
			int labelColor = button.active ? 16777215 : 10526880;
			button.renderString(graphics, Minecraft.getInstance().font, labelColor | Mth.ceil(((IMixinAbstractWidget)button).getAlphaFancyMenu() * 255.0F) << 24);
		}

		//TODO fix missing color handling
//		cachedShaderColor_FancyMenu = RenderSystem.getShaderColor();
//		if (cachedShaderColor_FancyMenu.length < 4) cachedShaderColor_FancyMenu = new float[] { 1.0F, 1.0F, 1.0F, 1.0F };
//
//		RenderSystem.enableBlend();
//		//Fix missing alpha handling for ImageButtons (Vanilla bug)
//		RenderSystem.setShaderColor(cachedShaderColor_FancyMenu[0], cachedShaderColor_FancyMenu[1], cachedShaderColor_FancyMenu[2], ((IMixinAbstractWidget)button).getAlphaFancyMenu());

		//If it should render the Vanilla background
		return renderVanilla;

	}

	@Inject(method = "renderWidget", at = @At("RETURN"))
	private void afterRenderWidgetFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
		//TODO fix missing color handling
//		//Reset shader color after alpha handling
//		if (cachedShaderColor_FancyMenu == null) cachedShaderColor_FancyMenu = new float[] { 1.0F, 1.0F, 1.0F, 1.0F };
//		RenderSystem.setShaderColor(cachedShaderColor_FancyMenu[0], cachedShaderColor_FancyMenu[1], cachedShaderColor_FancyMenu[2], cachedShaderColor_FancyMenu[3]);
//		cachedShaderColor_FancyMenu = null;
	}
	
}

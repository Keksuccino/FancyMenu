package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.opengl.GlStateManager;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(ImageButton.class)
public abstract class MixinImageButton extends Button {

	@Unique private int cachedShaderColor_FancyMenu = -1;

	// Dummy constructor
	private MixinImageButton() {
		super(0, 0, 0, 0, Component.empty(), button -> {}, DEFAULT_NARRATION);
	}

	@WrapWithCondition(method = "renderContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private boolean wrapRenderTextureFancyMenu(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {

		ImageButton button = (ImageButton)((Object)this);
		CustomizableWidget customizable = ((CustomizableWidget)this);

		//Render custom background if present
		boolean renderVanilla = ((CustomizableWidget)this).renderCustomBackgroundFancyMenu(button, graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight());

		//Render custom labels if present, the same way other Vanilla buttons render theirs, so all label customizations apply to them too
		Component label = button.isHoveredOrFocused() ? customizable.getHoverLabelFancyMenu() : customizable.getCustomLabelFancyMenu();
		if (!renderVanilla && label != null) {
			Component renderedLabel = this.active ? this.getMessage() : AbstractWidget.WithInactiveMessage.defaultInactiveMessage(label);
			this.renderScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE), renderedLabel, 2);
		}

		cachedShaderColor_FancyMenu = RenderingUtils.getShaderColor();

		GlStateManager._enableBlend();
		//Fix missing alpha handling for ImageButtons (Vanilla bug)
		int baseColor = cachedShaderColor_FancyMenu == -1 ? 0xFFFFFFFF : cachedShaderColor_FancyMenu;
		RenderingUtils.setShaderColor(graphics, ARGB.color(((IMixinAbstractWidget)button).getAlphaFancyMenu(), baseColor));

		//If it should render the Vanilla background
		return renderVanilla;

	}

	@Inject(method = "renderContents", at = @At("RETURN"))
	private void afterRenderWidgetFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
		//Reset shader color after alpha handling
		RenderingUtils.setShaderColor(graphics, cachedShaderColor_FancyMenu);
		cachedShaderColor_FancyMenu = -1;
	}
	
}

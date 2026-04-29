package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(ImageButton.class)
public abstract class MixinImageButton extends GuiComponent {

	@Unique
	private float[] cachedShaderColor_FancyMenu;

	/**
	 * @reason Render FancyMenu custom image button textures through the 1.19.2 PoseStack path.
	 */
	@WrapWithCondition(method = "renderButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ImageButton;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIFFIIII)V"))
	private boolean wrapRenderTextureFancyMenu(PoseStack pose, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
		GuiGraphics graphics = GuiGraphics.currentGraphics();
		ImageButton button = (ImageButton)((Object)this);
		CustomizableWidget customizable = (CustomizableWidget)this;

		boolean renderVanilla = customizable.renderCustomBackgroundFancyMenu(button, graphics, button.x, button.y, button.getWidth(), button.getHeight());
		if (!renderVanilla && (((customizable.getCustomLabelFancyMenu() != null) && !button.isHoveredOrFocused()) || ((customizable.getHoverLabelFancyMenu() != null) && button.isHoveredOrFocused()))) {
			int labelColor = button.active ? 16777215 : 10526880;
			graphics.drawCenteredString(Minecraft.getInstance().font, button.getMessage(), button.x + button.getWidth() / 2, button.y + (button.getHeight() - 8) / 2, labelColor | Mth.ceil(((IMixinAbstractWidget)button).getAlphaFancyMenu() * 255.0F) << 24);
		}

		this.cachedShaderColor_FancyMenu = RenderSystem.getShaderColor();
		if (this.cachedShaderColor_FancyMenu.length < 4) this.cachedShaderColor_FancyMenu = new float[] { 1.0F, 1.0F, 1.0F, 1.0F };

		RenderSystem.enableBlend();
		graphics.setColor(this.cachedShaderColor_FancyMenu[0], this.cachedShaderColor_FancyMenu[1], this.cachedShaderColor_FancyMenu[2], ((IMixinAbstractWidget)button).getAlphaFancyMenu());
		return renderVanilla;
	}

	@Inject(method = "renderButton", at = @At("RETURN"))
	private void afterRenderWidgetFancyMenu(PoseStack pose, int mouseX, int mouseY, float partial, CallbackInfo info) {
		if (this.cachedShaderColor_FancyMenu == null) this.cachedShaderColor_FancyMenu = new float[] { 1.0F, 1.0F, 1.0F, 1.0F };
		RenderSystem.setShaderColor(this.cachedShaderColor_FancyMenu[0], this.cachedShaderColor_FancyMenu[1], this.cachedShaderColor_FancyMenu[2], this.cachedShaderColor_FancyMenu[3]);
		this.cachedShaderColor_FancyMenu = null;
	}

}

package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.events.widget.RenderedGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Unique private static final int LIST_BACKGROUND_COLOR_FANCYMENU = 0x70000000;

	/**
	 * @reason Some 1.20.1 list screens render their scroll list before calling the normal screen background renderer.
	 */
	@Inject(method = "render", at = @At("HEAD"))
	private void before_render_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
		Screen screen = Minecraft.getInstance().screen;
		ScreenCustomizationLayer layer = getCurrentLayer_FancyMenu(screen);
		if ((layer != null) && layer.shouldRenderBackgroundFromScrollList()) {
			EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(screen, graphics, mouseX, mouseY, partialTick));
		}
	}

	/**
	 * @reason 1.20.1 list backgrounds are opaque dirt textures; use a translucent 1.21-style panel when FancyMenu owns the background.
	 */
	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V", ordinal = 0))
	private void wrap_listBackgroundBlit_in_render_FancyMenu(GuiGraphics graphics, ResourceLocation location, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight, Operation<Void> original) {
		if (shouldUseFancyMenuListBackground_FancyMenu()) {
			RenderingUtils.setupAlphaBlend();
			RenderingUtils.resetShaderColor(graphics);
			graphics.fill(x, y, x + width, y + height, LIST_BACKGROUND_COLOR_FANCYMENU);
			return;
		}
		original.call(graphics, location, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
	}

	/**
	 * @reason 1.20.1 list header/footer dirt panels cover FancyMenu's custom screen background.
	 */
	@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V", ordinal = 1))
	private boolean wrap_headerBackgroundBlit_in_render_FancyMenu(GuiGraphics graphics, ResourceLocation location, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
		return !shouldUseFancyMenuListBackground_FancyMenu();
	}

	/**
	 * @reason 1.20.1 list header/footer dirt panels cover FancyMenu's custom screen background.
	 */
	@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V", ordinal = 2))
	private boolean wrap_footerBackgroundBlit_in_render_FancyMenu(GuiGraphics graphics, ResourceLocation location, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
		return !shouldUseFancyMenuListBackground_FancyMenu();
	}

	/**
	 * @reason FancyMenu layouts can hide the vanilla scroll-list header shadow.
	 */
	@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(Lnet/minecraft/client/renderer/RenderType;IIIIIII)V", ordinal = 0))
	private boolean wrap_headerShadow_in_render_FancyMenu(GuiGraphics graphics, RenderType renderType, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, int z) {
		ScreenCustomizationLayer layer = getCurrentLayer_FancyMenu(Minecraft.getInstance().screen);
		return (layer == null) || layer.layoutBase.renderScrollListHeaderShadow;
	}

	/**
	 * @reason FancyMenu layouts can hide the vanilla scroll-list footer shadow.
	 */
	@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(Lnet/minecraft/client/renderer/RenderType;IIIIIII)V", ordinal = 1))
	private boolean wrap_footerShadow_in_render_FancyMenu(GuiGraphics graphics, RenderType renderType, int x1, int y1, int x2, int y2, int colorFrom, int colorTo, int z) {
		ScreenCustomizationLayer layer = getCurrentLayer_FancyMenu(Minecraft.getInstance().screen);
		return (layer == null) || layer.layoutBase.renderScrollListFooterShadow;
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;setColor(FFFF)V", ordinal = 3, shift = At.Shift.AFTER))
	private void after_renderTopAndBottom_FancyMenu(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
		//Render custom header/footer textures after the 1.20.1 vanilla header/footer background.
		EventHandler.INSTANCE.postEvent(new RenderedGuiListHeaderFooterEvent(graphics, (AbstractSelectionList) ((Object)this)));
	}

	@Unique
	private static boolean shouldUseFancyMenuListBackground_FancyMenu() {
		ScreenCustomizationLayer layer = getCurrentLayer_FancyMenu(Minecraft.getInstance().screen);
		return (layer != null) && layer.shouldReplaceVanillaScrollListBackground();
	}

	@Unique
	@Nullable
	private static ScreenCustomizationLayer getCurrentLayer_FancyMenu(@Nullable Screen screen) {
		if ((screen == null) || !ScreenCustomization.isCustomizationEnabledForScreen(screen)) {
			return null;
		}
		return ScreenCustomizationLayerHandler.getLayerOfScreen(screen);
	}
	
}

package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.events.screen.RenderedScreenBackgroundEvent;
import de.keksuccino.fancymenu.events.widget.RenderedGuiListHeaderFooterEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

	@Unique private static final int LIST_BACKGROUND_COLOR_FANCYMENU = 0x70000000;

	@Shadow protected int width;
	@Shadow protected int height;
	@Shadow protected int x0;
	@Shadow protected int x1;
	@Shadow protected int y0;
	@Shadow protected int y1;
	@Shadow private boolean renderBackground;
	@Shadow private boolean renderTopAndBottom;

	@Unique private boolean changedListBackgroundRenderingFancyMenu;
	@Unique private boolean previousRenderBackgroundFancyMenu;
	@Unique private boolean changedHeaderFooterRenderingFancyMenu;
	@Unique private boolean previousRenderTopAndBottomFancyMenu;
	@Unique private boolean shouldRenderHeaderFooterFancyMenu;
	@Unique @Nullable private ScreenCustomizationLayer headerFooterLayerFancyMenu;

	/**
	 * @reason Some 1.20.1 list screens render their scroll list before calling the normal screen background renderer.
	 */
	@Inject(method = "render", at = @At("HEAD"))
	private void before_render_FancyMenu(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
		Screen screen = Minecraft.getInstance().screen;
		ScreenCustomizationLayer layer = getCurrentLayer_FancyMenu(screen);
		GuiGraphics graphics = GuiGraphics.currentGraphics();
		if ((graphics != null) && (layer != null) && layer.shouldRenderBackgroundFromScrollList()) {
			EventHandler.INSTANCE.postEvent(new RenderedScreenBackgroundEvent(screen, graphics, mouseX, mouseY, partialTick));
		}

		this.changedListBackgroundRenderingFancyMenu = false;
		if ((layer != null) && layer.shouldReplaceVanillaScrollListBackground() && this.renderBackground) {
			this.changedListBackgroundRenderingFancyMenu = true;
			this.previousRenderBackgroundFancyMenu = this.renderBackground;
			this.renderBackground = false;
		}
	}

	/**
	 * @reason 1.19.2 list backgrounds are opaque dirt textures; use a translucent 1.21-style panel when FancyMenu owns the background.
	 */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getRowLeft()I"))
	private void after_listBackground_FancyMenu(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
		if (!this.changedListBackgroundRenderingFancyMenu) {
			return;
		}

		this.renderBackground = this.previousRenderBackgroundFancyMenu;
		GuiGraphics graphics = GuiGraphics.currentGraphics();
		if (graphics != null) {
			RenderingUtils.setupAlphaBlend();
			RenderingUtils.resetShaderColor(graphics);
			graphics.fill(this.x0, this.y0, this.x1, this.y1, LIST_BACKGROUND_COLOR_FANCYMENU);
		}
		this.changedListBackgroundRenderingFancyMenu = false;
	}

	/**
	 * @reason 1.19.2 renders list header/footer dirt and shadows as one block; FancyMenu needs to replace or selectively suppress parts of it.
	 */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderList(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", shift = At.Shift.AFTER))
	private void before_headerFooter_FancyMenu(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
		this.changedHeaderFooterRenderingFancyMenu = false;
		this.shouldRenderHeaderFooterFancyMenu = this.renderTopAndBottom;
		this.headerFooterLayerFancyMenu = getCurrentLayer_FancyMenu(Minecraft.getInstance().screen);
		if (!this.shouldRenderHeaderFooterFancyMenu || (this.headerFooterLayerFancyMenu == null)) {
			return;
		}

		if (this.shouldReplaceHeaderFooterBlock_FancyMenu(this.headerFooterLayerFancyMenu)) {
			this.changedHeaderFooterRenderingFancyMenu = true;
			this.previousRenderTopAndBottomFancyMenu = this.renderTopAndBottom;
			this.renderTopAndBottom = false;
		}
	}

	/**
	 * @reason Render FancyMenu's custom list header/footer textures at the same point as the modern GuiGraphics implementation.
	 */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;getMaxScroll()I"))
	private void after_headerFooter_FancyMenu(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo info) {
		GuiGraphics graphics = GuiGraphics.currentGraphics();
		if (this.changedHeaderFooterRenderingFancyMenu) {
			if ((graphics != null) && (this.headerFooterLayerFancyMenu != null)) {
				if (!this.headerFooterLayerFancyMenu.shouldReplaceVanillaScrollListBackground()) {
					this.renderVanillaHeaderFooterBackground_FancyMenu(graphics);
				}
				this.renderHeaderFooterShadows_FancyMenu(graphics, this.headerFooterLayerFancyMenu);
			}
			this.renderTopAndBottom = this.previousRenderTopAndBottomFancyMenu;
			this.changedHeaderFooterRenderingFancyMenu = false;
		}

		if (this.shouldRenderHeaderFooterFancyMenu && (graphics != null)) {
			EventHandler.INSTANCE.postEvent(new RenderedGuiListHeaderFooterEvent(graphics, (AbstractSelectionList<?>) (Object) this));
			RenderingUtils.resetShaderColor(graphics);
		}
		this.shouldRenderHeaderFooterFancyMenu = false;
		this.headerFooterLayerFancyMenu = null;
	}

	@Unique
	private boolean shouldReplaceHeaderFooterBlock_FancyMenu(@Nullable ScreenCustomizationLayer layer) {
		return (layer != null) && (layer.shouldReplaceVanillaScrollListBackground()
				|| !layer.layoutBase.renderScrollListHeaderShadow
				|| !layer.layoutBase.renderScrollListFooterShadow);
	}

	@Unique
	private void renderVanillaHeaderFooterBackground_FancyMenu(GuiGraphics graphics) {
		graphics.flush();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(519);
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bufferBuilder.vertex(this.x0, this.y0, -100.0).uv(0.0F, (float) this.y0 / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferBuilder.vertex(this.x0 + this.width, this.y0, -100.0).uv((float) this.width / 32.0F, (float) this.y0 / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferBuilder.vertex(this.x0 + this.width, 0.0, -100.0).uv((float) this.width / 32.0F, 0.0F).color(64, 64, 64, 255).endVertex();
		bufferBuilder.vertex(this.x0, 0.0, -100.0).uv(0.0F, 0.0F).color(64, 64, 64, 255).endVertex();
		bufferBuilder.vertex(this.x0, this.height, -100.0).uv(0.0F, (float) this.height / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferBuilder.vertex(this.x0 + this.width, this.height, -100.0).uv((float) this.width / 32.0F, (float) this.height / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferBuilder.vertex(this.x0 + this.width, this.y1, -100.0).uv((float) this.width / 32.0F, (float) this.y1 / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferBuilder.vertex(this.x0, this.y1, -100.0).uv(0.0F, (float) this.y1 / 32.0F).color(64, 64, 64, 255).endVertex();
		Tesselator.getInstance().end();
		RenderSystem.depthFunc(515);
		RenderSystem.disableDepthTest();
	}

	@Unique
	private void renderHeaderFooterShadows_FancyMenu(GuiGraphics graphics, ScreenCustomizationLayer layer) {
		graphics.flush();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		RenderSystem.disableTexture();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		if (layer.layoutBase.renderScrollListHeaderShadow) {
			bufferBuilder.vertex(this.x0, this.y0 + 4, 0.0).color(0, 0, 0, 0).endVertex();
			bufferBuilder.vertex(this.x1, this.y0 + 4, 0.0).color(0, 0, 0, 0).endVertex();
			bufferBuilder.vertex(this.x1, this.y0, 0.0).color(0, 0, 0, 255).endVertex();
			bufferBuilder.vertex(this.x0, this.y0, 0.0).color(0, 0, 0, 255).endVertex();
		}
		if (layer.layoutBase.renderScrollListFooterShadow) {
			bufferBuilder.vertex(this.x0, this.y1, 0.0).color(0, 0, 0, 255).endVertex();
			bufferBuilder.vertex(this.x1, this.y1, 0.0).color(0, 0, 0, 255).endVertex();
			bufferBuilder.vertex(this.x1, this.y1 - 4, 0.0).color(0, 0, 0, 0).endVertex();
			bufferBuilder.vertex(this.x0, this.y1 - 4, 0.0).color(0, 0, 0, 0).endVertex();
		}
		Tesselator.getInstance().end();
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

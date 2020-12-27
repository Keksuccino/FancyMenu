package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.worldselection;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.world.WorldListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class WorldSelectionMenuList extends WorldListWidget {

	private MenuHandlerBase handler;
	private Screen screen;
	
	public WorldSelectionMenuList(SelectWorldScreen p_i49846_1_, MinecraftClient p_i49846_2_, int p_i49846_3_, int p_i49846_4_, int p_i49846_5_, int p_i49846_6_, int p_i49846_7_, Supplier<String> p_i49846_8_, WorldListWidget p_i49846_9_, MenuHandlerBase handler) {
		super(p_i49846_1_, p_i49846_2_, p_i49846_3_, p_i49846_4_, p_i49846_5_, p_i49846_6_, p_i49846_7_, p_i49846_8_, p_i49846_9_);
		this.handler = handler;
		this.screen = p_i49846_1_;
	}

	@Override
	public void render(MatrixStack matrix, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
		int i = this.getScrollbarPositionX();
		int j = i + 6;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();

		if (!this.handler.canRenderBackground()) {
			MinecraftClient.getInstance().getTextureManager().bindTexture(DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			bufferbuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
			bufferbuilder.vertex((double)this.left, (double)this.bottom, 0.0D).texture((float)this.left / 32.0F, (float)(this.bottom + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).next();
			bufferbuilder.vertex((double)this.right, (double)this.bottom, 0.0D).texture((float)this.right / 32.0F, (float)(this.bottom + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).next();
			bufferbuilder.vertex((double)this.right, (double)this.top, 0.0D).texture((float)this.right / 32.0F, (float)(this.top + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).next();
			bufferbuilder.vertex((double)this.left, (double)this.top, 0.0D).texture((float)this.left / 32.0F, (float)(this.top + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).next();
			tessellator.draw();
		} else {
			this.screen.renderBackground(matrix);
		}

		int k = this.getRowLeft();
		int l = this.top + 4 - (int)this.getScrollAmount();
		if (this.renderHeader()) {
			this.renderHeader(matrix, k, l, tessellator);
		}

		this.renderList(matrix, k, l, p_230430_2_, p_230430_3_, p_230430_4_);
		this.client.getTextureManager().bindTexture(DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(519);
		bufferbuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
		bufferbuilder.vertex((double)this.left, (double)this.top, -100.0D).texture(0.0F, (float)this.top / 32.0F).color(64, 64, 64, 255).next();
		bufferbuilder.vertex((double)(this.left + this.width), (double)this.top, -100.0D).texture((float)this.width / 32.0F, (float)this.top / 32.0F).color(64, 64, 64, 255).next();
		bufferbuilder.vertex((double)(this.left + this.width), 0.0D, -100.0D).texture((float)this.width / 32.0F, 0.0F).color(64, 64, 64, 255).next();
		bufferbuilder.vertex((double)this.left, 0.0D, -100.0D).texture(0.0F, 0.0F).color(64, 64, 64, 255).next();
		bufferbuilder.vertex((double)this.left, (double)this.height, -100.0D).texture(0.0F, (float)this.height / 32.0F).color(64, 64, 64, 255).next();
		bufferbuilder.vertex((double)(this.left + this.width), (double)this.height, -100.0D).texture((float)this.width / 32.0F, (float)this.height / 32.0F).color(64, 64, 64, 255).next();
		bufferbuilder.vertex((double)(this.left + this.width), (double)this.bottom, -100.0D).texture((float)this.width / 32.0F, (float)this.bottom / 32.0F).color(64, 64, 64, 255).next();
		bufferbuilder.vertex((double)this.left, (double)this.bottom, -100.0D).texture(0.0F, (float)this.bottom / 32.0F).color(64, 64, 64, 255).next();
		tessellator.draw();
		RenderSystem.depthFunc(515);
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(7425);
		RenderSystem.disableTexture();
		bufferbuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
		bufferbuilder.vertex((double)this.left, (double)(this.top + 4), 0.0D).texture(0.0F, 1.0F).color(0, 0, 0, 0).next();
		bufferbuilder.vertex((double)this.right, (double)(this.top + 4), 0.0D).texture(1.0F, 1.0F).color(0, 0, 0, 0).next();
		bufferbuilder.vertex((double)this.right, (double)this.top, 0.0D).texture(1.0F, 0.0F).color(0, 0, 0, 255).next();
		bufferbuilder.vertex((double)this.left, (double)this.top, 0.0D).texture(0.0F, 0.0F).color(0, 0, 0, 255).next();
		bufferbuilder.vertex((double)this.left, (double)this.bottom, 0.0D).texture(0.0F, 1.0F).color(0, 0, 0, 255).next();
		bufferbuilder.vertex((double)this.right, (double)this.bottom, 0.0D).texture(1.0F, 1.0F).color(0, 0, 0, 255).next();
		bufferbuilder.vertex((double)this.right, (double)(this.bottom - 4), 0.0D).texture(1.0F, 0.0F).color(0, 0, 0, 0).next();
		bufferbuilder.vertex((double)this.left, (double)(this.bottom - 4), 0.0D).texture(0.0F, 0.0F).color(0, 0, 0, 0).next();
		tessellator.draw();
		int o = this.getMaxScrollValue();
		if (o > 0) {
			int p = (int)((float)((this.bottom - this.top) * (this.bottom - this.top)) / (float)this.getMaxPosition());
			p = MathHelper.clamp(p, 32, this.bottom - this.top - 8);
			int q = (int)this.getScrollAmount() * (this.bottom - this.top - p) / o + this.top;
			if (q < this.top) {
				q = this.top;
			}

			bufferbuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
			bufferbuilder.vertex((double)i, (double)this.bottom, 0.0D).texture(0.0F, 1.0F).color(0, 0, 0, 255).next();
			bufferbuilder.vertex((double)j, (double)this.bottom, 0.0D).texture(1.0F, 1.0F).color(0, 0, 0, 255).next();
			bufferbuilder.vertex((double)j, (double)this.top, 0.0D).texture(1.0F, 0.0F).color(0, 0, 0, 255).next();
			bufferbuilder.vertex((double)i, (double)this.top, 0.0D).texture(0.0F, 0.0F).color(0, 0, 0, 255).next();
			bufferbuilder.vertex((double)i, (double)(q + p), 0.0D).texture(0.0F, 1.0F).color(128, 128, 128, 255).next();
			bufferbuilder.vertex((double)j, (double)(q + p), 0.0D).texture(1.0F, 1.0F).color(128, 128, 128, 255).next();
			bufferbuilder.vertex((double)j, (double)q, 0.0D).texture(1.0F, 0.0F).color(128, 128, 128, 255).next();
			bufferbuilder.vertex((double)i, (double)q, 0.0D).texture(0.0F, 0.0F).color(128, 128, 128, 255).next();
			bufferbuilder.vertex((double)i, (double)(q + p - 1), 0.0D).texture(0.0F, 1.0F).color(192, 192, 192, 255).next();
			bufferbuilder.vertex((double)(j - 1), (double)(q + p - 1), 0.0D).texture(1.0F, 1.0F).color(192, 192, 192, 255).next();
			bufferbuilder.vertex((double)(j - 1), (double)q, 0.0D).texture(1.0F, 0.0F).color(192, 192, 192, 255).next();
			bufferbuilder.vertex((double)i, (double)q, 0.0D).texture(0.0F, 0.0F).color(192, 192, 192, 255).next();
			tessellator.draw();
		}

		this.renderDecorations(matrix, p_230430_2_, p_230430_3_);
		RenderSystem.enableTexture();
		RenderSystem.shadeModel(7424);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}
	
	private boolean renderHeader() {
		try {
			Field f = ReflectionHelper.findField(EntryListWidget.class, "renderHeader", "field_22747");
			return f.getBoolean(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	protected int getMaxScrollValue() {
		return Math.max(0, this.getMaxPosition() - (this.bottom - this.top - 4));
	}

}

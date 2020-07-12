package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.videosettings;

import java.lang.reflect.Field;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.list.OptionsRowList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class VideoSettingsList extends OptionsRowList {

	private MenuHandlerBase handler;
	
	public VideoSettingsList(Minecraft mc, int width, int height, int p_i51130_4_, int p_i51130_5_, int p_i51130_6_, MenuHandlerBase handler) {
		super(mc, width, height, p_i51130_4_, p_i51130_5_, p_i51130_6_);
		this.handler = handler;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void render(MatrixStack matrix, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
		int i = this.getScrollbarPosition();
		int j = i + 6;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();

		if (!this.handler.canRenderBackground()) {
			Minecraft.getInstance().getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
			tessellator.draw();
		}

		int k = this.getRowLeft();
		int l = this.y0 + 4 - (int)this.getScrollAmount();
		if (this.renderHeader()) {
			this.renderHeader(matrix, k, l, tessellator);
		}

		this.renderList(matrix, k, l, p_230430_2_, p_230430_3_, p_230430_4_);
		this.minecraft.getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(519);
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		bufferbuilder.pos((double)this.x0, (double)this.y0, -100.0D).tex(0.0F, (float)this.y0 / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)(this.x0 + this.width), (double)this.y0, -100.0D).tex((float)this.width / 32.0F, (float)this.y0 / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)(this.x0 + this.width), 0.0D, -100.0D).tex((float)this.width / 32.0F, 0.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)this.x0, 0.0D, -100.0D).tex(0.0F, 0.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)this.x0, (double)this.height, -100.0D).tex(0.0F, (float)this.height / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)(this.x0 + this.width), (double)this.height, -100.0D).tex((float)this.width / 32.0F, (float)this.height / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)(this.x0 + this.width), (double)this.y1, -100.0D).tex((float)this.width / 32.0F, (float)this.y1 / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)this.x0, (double)this.y1, -100.0D).tex(0.0F, (float)this.y1 / 32.0F).color(64, 64, 64, 255).endVertex();
		tessellator.draw();
		RenderSystem.depthFunc(515);
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(7425);
		RenderSystem.disableTexture();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		bufferbuilder.pos((double)this.x0, (double)(this.y0 + 4), 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 0).endVertex();
		bufferbuilder.pos((double)this.x1, (double)(this.y0 + 4), 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 0).endVertex();
		bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.x1, (double)(this.y1 - 4), 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
		bufferbuilder.pos((double)this.x0, (double)(this.y1 - 4), 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
		tessellator.draw();
		int k1 = this.getMaxScroll();
		if (k1 > 0) {
			int l1 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
			l1 = MathHelper.clamp(l1, 32, this.y1 - this.y0 - 8);
			int i2 = (int)this.getScrollAmount() * (this.y1 - this.y0 - l1) / k1 + this.y0;
			if (i2 < this.y0) {
				i2 = this.y0;
			}

			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)i, (double)this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)j, (double)this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)j, (double)this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)i, (double)this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)i, (double)(i2 + l1), 0.0D).tex(0.0F, 1.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.pos((double)j, (double)(i2 + l1), 0.0D).tex(1.0F, 1.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.pos((double)j, (double)i2, 0.0D).tex(1.0F, 0.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.pos((double)i, (double)i2, 0.0D).tex(0.0F, 0.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.pos((double)i, (double)(i2 + l1 - 1), 0.0D).tex(0.0F, 1.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.pos((double)(j - 1), (double)(i2 + l1 - 1), 0.0D).tex(1.0F, 1.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.pos((double)(j - 1), (double)i2, 0.0D).tex(1.0F, 0.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.pos((double)i, (double)i2, 0.0D).tex(0.0F, 0.0F).color(192, 192, 192, 255).endVertex();
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
			Field f = ObfuscationReflectionHelper.findField(AbstractList.class, "field_230680_q_");
			return f.getBoolean(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private int getMaxScroll() {
		return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
	}

}

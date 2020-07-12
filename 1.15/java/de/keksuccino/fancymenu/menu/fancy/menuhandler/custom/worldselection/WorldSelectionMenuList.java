//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.worldselection;

import java.util.function.Supplier;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionList;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;

public class WorldSelectionMenuList extends WorldSelectionList {

	private MenuHandlerBase handler;
	private Screen screen;
	
	public WorldSelectionMenuList(WorldSelectionScreen p_i49846_1_, Minecraft p_i49846_2_, int p_i49846_3_, int p_i49846_4_, int p_i49846_5_, int p_i49846_6_, int p_i49846_7_, Supplier<String> p_i49846_8_, WorldSelectionList p_i49846_9_, MenuHandlerBase handler) {
		super(p_i49846_1_, p_i49846_2_, p_i49846_3_, p_i49846_4_, p_i49846_5_, p_i49846_6_, p_i49846_7_, p_i49846_8_, p_i49846_9_);
		this.handler = handler;
		this.screen = p_i49846_1_;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
		int i = this.getScrollbarPosition();
		int j = i + 6;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		
		if (!this.handler.canRenderBackground()) {
			this.minecraft.getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
			tessellator.draw();
		} else {
			this.screen.renderBackground();
		}
		
		int k = this.getRowLeft();
		int l = this.y0 + 4 - (int)this.getScrollAmount();

		this.renderList(k, l, p_render_1_, p_render_2_, p_render_3_);
		RenderSystem.disableDepthTest();
		this.renderHoleBackground(0, this.y0, 255, 255);
		this.renderHoleBackground(this.y1, this.height, 255, 255);
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
		tessellator.draw();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.x1, (double)(this.y1 - 4), 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
		bufferbuilder.pos((double)this.x0, (double)(this.y1 - 4), 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
		tessellator.draw();
		int j1 = this.getMaxScroll();
		if (j1 > 0) {
			int k1 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
			k1 = MathHelper.clamp(k1, 32, this.y1 - this.y0 - 8);
			int l1 = (int)this.getScrollAmount() * (this.y1 - this.y0 - k1) / j1 + this.y0;
			if (l1 < this.y0) {
				l1 = this.y0;
			}

			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)i, (double)this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)j, (double)this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)j, (double)this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)i, (double)this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			tessellator.draw();
			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)i, (double)(l1 + k1), 0.0D).tex(0.0F, 1.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.pos((double)j, (double)(l1 + k1), 0.0D).tex(1.0F, 1.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.pos((double)j, (double)l1, 0.0D).tex(1.0F, 0.0F).color(128, 128, 128, 255).endVertex();
			bufferbuilder.pos((double)i, (double)l1, 0.0D).tex(0.0F, 0.0F).color(128, 128, 128, 255).endVertex();
			tessellator.draw();
			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)i, (double)(l1 + k1 - 1), 0.0D).tex(0.0F, 1.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.pos((double)(j - 1), (double)(l1 + k1 - 1), 0.0D).tex(1.0F, 1.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.pos((double)(j - 1), (double)l1, 0.0D).tex(1.0F, 0.0F).color(192, 192, 192, 255).endVertex();
			bufferbuilder.pos((double)i, (double)l1, 0.0D).tex(0.0F, 0.0F).color(192, 192, 192, 255).endVertex();
			tessellator.draw();
		}

		this.renderDecorations(p_render_1_, p_render_2_);
		RenderSystem.enableTexture();
		RenderSystem.shadeModel(7424);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}
	
	private int getMaxScroll() {
		return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
	}

}

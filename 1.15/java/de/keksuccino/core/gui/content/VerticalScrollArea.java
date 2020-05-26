package de.keksuccino.core.gui.content;

import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.list.AbstractOptionList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class VerticalScrollArea extends AbstractOptionList<VerticalScrollArea.Entry> {

	protected final Screen parent;
	protected final VerticalScrollArea.Entry body;

	public VerticalScrollArea(int x, int y, int width, int height, int bodyHeight, Screen parent) {
		super(Minecraft.getInstance(), width + 45, height, 43, height, bodyHeight);

		this.parent = parent;
		this.x0 = x;
		this.y0 = y;
		this.width = width;
		this.height = height;

		this.body = new Entry();
		this.addEntry(this.body);
	}
	
	/**
	 * The scrollarea body.
	 */
	public abstract void renderBody(int mouseX, int mouseY);

	@SuppressWarnings("deprecation")
	@Override
	public void render(int mouseX, int mouseY, float partial) {
		int i = this.getScrollbarPosition();
		int j = i + 6;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		this.minecraft.getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
		bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
		bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
		bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
		tessellator.draw();
		int k = this.getRowLeft();
		int l = this.y0 + 4 - (int)this.getScrollAmount();
		if (this.renderHeader) {
			this.renderHeader(k, l, tessellator);
		}

		this.renderList(k, l, mouseX, mouseY, partial);
		
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

		this.renderDecorations(mouseX, mouseY);
		RenderSystem.enableTexture();
		RenderSystem.shadeModel(7424);
		RenderSystem.enableAlphaTest();
		RenderSystem.disableBlend();
	}

	private int getMaxScroll() {
		return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
	}

	protected int getScrollbarPosition() {
		return super.getScrollbarPosition() + 15 + 20;
	}

	@Override
	public int getRowWidth() {
		return this.width;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public int getX() {
		return this.x0;
	}

	public int getY() {
		return this.y0;
	}

	public void setWidth(int i) {
		this.width = i;
	}

	public void setHeight(int i) {
		this.height = i;
	}

	public void setX(int i) {
		this.x0 = i;
	}

	public void setY(int i) {
		this.y0 = i;
	}

	public class Entry extends AbstractOptionList.Entry<VerticalScrollArea.Entry> {
		public void render(int p_render_1_, int p_render_2_, int p_render_3_, int p_render_4_, int p_render_5_, int p_render_6_, int p_render_7_, boolean p_render_8_, float p_render_9_) {
			VerticalScrollArea.this.renderBody(MouseInput.getMouseX(), MouseInput.getMouseY());
		}

		@Override
		public List<? extends IGuiEventListener> children() {
			return Collections.emptyList();
		}
	}

}
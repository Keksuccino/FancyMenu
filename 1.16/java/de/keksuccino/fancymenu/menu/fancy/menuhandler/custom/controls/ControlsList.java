package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.controls;

import java.lang.reflect.Field;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.ControlsScreen;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.list.KeyBindingList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ControlsList extends KeyBindingList {

	private MenuHandlerBase handler;
	
	public ControlsList(ControlsScreen controls, Minecraft mcIn, MenuHandlerBase handler) {
		super(controls, mcIn);
		this.handler = handler;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void func_230430_a_(MatrixStack matrix, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
		int i = this.func_230952_d_();
		int j = i + 6;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();

		if (!this.handler.canRenderBackground()) {
			Minecraft.getInstance().getTextureManager().bindTexture(AbstractGui.field_230663_f_);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)this.field_230675_l_, (double)this.field_230673_j_, 0.0D).tex((float)this.field_230675_l_ / 32.0F, (float)(this.field_230673_j_ + (int)this.func_230966_l_()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.field_230674_k_, (double)this.field_230673_j_, 0.0D).tex((float)this.field_230674_k_ / 32.0F, (float)(this.field_230673_j_ + (int)this.func_230966_l_()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.field_230674_k_, (double)this.field_230672_i_, 0.0D).tex((float)this.field_230674_k_ / 32.0F, (float)(this.field_230672_i_ + (int)this.func_230966_l_()) / 32.0F).color(32, 32, 32, 255).endVertex();
			bufferbuilder.pos((double)this.field_230675_l_, (double)this.field_230672_i_, 0.0D).tex((float)this.field_230675_l_ / 32.0F, (float)(this.field_230672_i_ + (int)this.func_230966_l_()) / 32.0F).color(32, 32, 32, 255).endVertex();
			tessellator.draw();
		}

		int k = this.func_230968_n_();
		int l = this.field_230672_i_ + 4 - (int)this.func_230966_l_();
		if (this.renderHeader()) {
			this.func_230448_a_(matrix, k, l, tessellator);
		}

		this.func_238478_a_(matrix, k, l, p_230430_2_, p_230430_3_, p_230430_4_);
		this.field_230668_b_.getTextureManager().bindTexture(AbstractGui.field_230663_f_);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(519);
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		bufferbuilder.pos((double)this.field_230675_l_, (double)this.field_230672_i_, -100.0D).tex(0.0F, (float)this.field_230672_i_ / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)(this.field_230675_l_ + this.field_230670_d_), (double)this.field_230672_i_, -100.0D).tex((float)this.field_230670_d_ / 32.0F, (float)this.field_230672_i_ / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)(this.field_230675_l_ + this.field_230670_d_), 0.0D, -100.0D).tex((float)this.field_230670_d_ / 32.0F, 0.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)this.field_230675_l_, 0.0D, -100.0D).tex(0.0F, 0.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)this.field_230675_l_, (double)this.field_230671_e_, -100.0D).tex(0.0F, (float)this.field_230671_e_ / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)(this.field_230675_l_ + this.field_230670_d_), (double)this.field_230671_e_, -100.0D).tex((float)this.field_230670_d_ / 32.0F, (float)this.field_230671_e_ / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)(this.field_230675_l_ + this.field_230670_d_), (double)this.field_230673_j_, -100.0D).tex((float)this.field_230670_d_ / 32.0F, (float)this.field_230673_j_ / 32.0F).color(64, 64, 64, 255).endVertex();
		bufferbuilder.pos((double)this.field_230675_l_, (double)this.field_230673_j_, -100.0D).tex(0.0F, (float)this.field_230673_j_ / 32.0F).color(64, 64, 64, 255).endVertex();
		tessellator.draw();
		RenderSystem.depthFunc(515);
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		RenderSystem.disableAlphaTest();
		RenderSystem.shadeModel(7425);
		RenderSystem.disableTexture();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		bufferbuilder.pos((double)this.field_230675_l_, (double)(this.field_230672_i_ + 4), 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 0).endVertex();
		bufferbuilder.pos((double)this.field_230674_k_, (double)(this.field_230672_i_ + 4), 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 0).endVertex();
		bufferbuilder.pos((double)this.field_230674_k_, (double)this.field_230672_i_, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.field_230675_l_, (double)this.field_230672_i_, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.field_230675_l_, (double)this.field_230673_j_, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.field_230674_k_, (double)this.field_230673_j_, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
		bufferbuilder.pos((double)this.field_230674_k_, (double)(this.field_230673_j_ - 4), 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
		bufferbuilder.pos((double)this.field_230675_l_, (double)(this.field_230673_j_ - 4), 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
		tessellator.draw();
		int k1 = this.getMaxScroll();
		if (k1 > 0) {
			int l1 = (int)((float)((this.field_230673_j_ - this.field_230672_i_) * (this.field_230673_j_ - this.field_230672_i_)) / (float)this.func_230945_b_());
			l1 = MathHelper.clamp(l1, 32, this.field_230673_j_ - this.field_230672_i_ - 8);
			int i2 = (int)this.func_230966_l_() * (this.field_230673_j_ - this.field_230672_i_ - l1) / k1 + this.field_230672_i_;
			if (i2 < this.field_230672_i_) {
				i2 = this.field_230672_i_;
			}

			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)i, (double)this.field_230673_j_, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)j, (double)this.field_230673_j_, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)j, (double)this.field_230672_i_, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)i, (double)this.field_230672_i_, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
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

		this.func_230447_a_(matrix, p_230430_2_, p_230430_3_);
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
		return Math.max(0, this.func_230945_b_() - (this.field_230673_j_ - this.field_230672_i_ - 4));
	}

}

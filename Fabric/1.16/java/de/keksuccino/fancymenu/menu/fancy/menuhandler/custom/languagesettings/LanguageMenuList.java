package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.languagesettings;

import java.lang.reflect.Field;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;

public class LanguageMenuList extends AlwaysSelectedEntryListWidget<LanguageMenuList.LanguageEntry> {

	private MenuHandlerBase handler;
	
	public LanguageMenuList(LanguageOptionsScreen screen, MinecraftClient mcIn, MenuHandlerBase handler) {
		super(mcIn, screen.width, screen.height, 32, screen.height - 65 + 4, 18);

		this.handler = handler;
		
		for(LanguageDefinition language : MinecraftClient.getInstance().getLanguageManager().getAllLanguages()) {
			LanguageMenuList.LanguageEntry languagescreen$list$languageentry = new LanguageMenuList.LanguageEntry(language);
			this.addEntry(languagescreen$list$languageentry);
			if (MinecraftClient.getInstance().getLanguageManager().getLanguage().getCode().equals(language.getCode())) {
				this.setSelected(languagescreen$list$languageentry);
			}
		}

		if (this.getSelected() != null) {
			this.centerScrollOn(this.getSelected());
		}

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

	@Override
	protected int getScrollbarPositionX() {
		return super.getScrollbarPositionX() + 20;
	}

	//getRowWidth
	@Override
	public int getRowWidth() {
		return super.getRowWidth() + 50;
	}

	//setSelected
	@Override
	public void setSelected(@Nullable LanguageMenuList.LanguageEntry p_setSelected_1_) {
		super.setSelected(p_setSelected_1_);
		if (p_setSelected_1_ != null) {
			NarratorManager.INSTANCE.narrate((new TranslatableText("narrator.select", p_setSelected_1_.languageDefinition)).getString());
		}

	}

	public class LanguageEntry extends AlwaysSelectedEntryListWidget.Entry<LanguageMenuList.LanguageEntry> {
		public final LanguageDefinition languageDefinition;

		public LanguageEntry(LanguageDefinition p_i50494_2_) {
			this.languageDefinition = p_i50494_2_;
		}

		@Override
		public void render(MatrixStack p_230432_1_, int p_230432_2_, int p_230432_3_, int p_230432_4_, int p_230432_5_, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean p_230432_9_, float p_230432_10_) {
			String s = this.languageDefinition.toString();
			MinecraftClient.getInstance().textRenderer.drawWithShadow(p_230432_1_, s, (float)(LanguageMenuList.this.width / 2 - MinecraftClient.getInstance().textRenderer.getWidth(s) / 2), (float)(p_230432_3_ + 1), 16777215, true);
		}

		@Override
		public boolean mouseClicked(double p_231044_1_, double p_231044_3_, int p_231044_5_) {
			if (p_231044_5_ == 0) {
				LanguageMenuList.this.setSelected(this);
				return true;
			} else {
				return false;
			}
		}

	}
}
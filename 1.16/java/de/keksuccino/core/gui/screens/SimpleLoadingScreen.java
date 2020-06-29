package de.keksuccino.core.gui.screens;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.rendering.animation.AnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleLoadingScreen extends Screen {
	private static ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojangstudios.png");
	private final Minecraft mc;
	private AnimationRenderer loading = new AnimationRenderer("keksuccino/animations/loading", 15, true, 0, 0, 200, 37, "fancymenu");
	private String status = "";
	private boolean darkmode = false;
	
	public SimpleLoadingScreen(Minecraft mc) {
		super(new StringTextComponent(""));
		this.mc = mc;
	}
	
	//render
	@Override
	public void func_230430_a_(MatrixStack matrix, int p_render_1_, int p_render_2_, float p_render_3_) {
		int color = new Color(239, 50, 61).getRGB();
		if (darkmode) {
			color = new Color(26, 26, 26).getRGB();
		}
		func_238467_a_(matrix, 0, 0, field_230708_k_, field_230709_l_, color);
		
		int j2 = (int)((double)mc.getMainWindow().getScaledWidth() * 0.5D);
		int i1 = (int)((double)mc.getMainWindow().getScaledHeight() * 0.5D);
		double d0 = Math.min((double)mc.getMainWindow().getScaledWidth() * 0.75D, (double)mc.getMainWindow().getScaledHeight()) * 0.25D;
		int j1 = (int)(d0 * 0.5D);
		double d1 = d0 * 4.0D;
		int k1 = (int)(d1 * 0.5D);
		mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		RenderSystem.enableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		func_238466_a_(matrix, j2 - k1, i1 - j1, k1, (int)d0, -0.0625F, 0.0F, 120, 60, 120, 120);
		func_238466_a_(matrix, j2, i1 - j1, k1, (int)d0, 0.0625F, 60.0F, 120, 60, 120, 120);

		this.loading.setPosX((field_230708_k_ /2) - 100);
		this.loading.setPosY(field_230709_l_ - 80);
		
		this.loading.render(matrix);
		
		this.drawStatus(this.status, matrix, field_230708_k_ / 2, field_230709_l_ - 30);
		
		super.func_230430_a_(matrix, p_render_1_, p_render_2_, p_render_3_);
	}
	
	public void setStatusText(String status) {
		this.status = status;
	}

	public void drawStatus(String text, MatrixStack matrix, int width, int height) {
		//drawString
		mc.fontRenderer.func_238421_b_(matrix, text, (float) (width - Minecraft.getInstance().fontRenderer.getStringWidth(text) / 2), (float) height, Color.WHITE.getRGB());
	}
	
	public void setDarkmode(boolean b) {
		this.darkmode = b;
	}
}
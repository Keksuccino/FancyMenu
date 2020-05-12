package de.keksuccino.core.gui.screens;

import java.awt.Color;

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
	private static ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojang.png");
	private static ResourceLocation MOJANG_LOGO_TEXTURE_DARK = new ResourceLocation("keksuccino", "mojang_dark.png");
	private final Minecraft mc;
	private AnimationRenderer loading = new AnimationRenderer("keksuccino/animations/loading", 15, true, 0, 0, 200, 37, "fancymenu");
	private String status = "";
	private boolean darkmode = false;
	
	public SimpleLoadingScreen(Minecraft mc) {
		super(new StringTextComponent(""));
		this.mc = mc;
	}
	
	@Override
	public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
		int color = Color.WHITE.getRGB();
		if (darkmode) {
			color = new Color(26, 26, 26).getRGB();
		}
		fill(0, 0, width, height, color);
		
		int k1 = (width - 256) / 2;
		int i1 = (height - 256) / 2;
		if (darkmode) {
			this.mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE_DARK);
		} else {
			this.mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		}
		RenderSystem.enableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.blit(k1, i1, 0, 0, 256, 256);

		this.loading.setPosX((width /2) - 100);
		this.loading.setPosY(i1 + 170);
		
		this.loading.render();
		
		this.drawStatus(this.status, width / 2, i1 + 170 + 50);
		
		super.render(p_render_1_, p_render_2_, p_render_3_);
	}
	
	public void setStatusText(String status) {
		this.status = status;
	}

	public void drawStatus(String text, int width, int height) {
		mc.fontRenderer.drawString(text, (float) (width - Minecraft.getInstance().fontRenderer.getStringWidth(text) / 2), (float) height, 14821431);
	}
	
	public void setDarkmode(boolean b) {
		this.darkmode = b;
	}
}
package de.keksuccino.gui;

import de.keksuccino.rendering.RenderUtils;
import de.keksuccino.rendering.animation.AnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

public class SimpleLoadingScreen extends GuiScreen {
	private static ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojang.png");
	private final Minecraft mc;
	private AnimationRenderer loading = new AnimationRenderer("keksuccino/animations/loading", 15, true, 0, 0, 200, 37, "fancymenu");
	private String status = "";
	
	public SimpleLoadingScreen(Minecraft mc) {
		super();
		this.mc = mc;
	}
	
	@Override
	public void drawScreen(int p_render_1_, int p_render_2_, float p_render_3_) {

		this.mc.getTextureManager().bindTexture(RenderUtils.getWhiteImageResource());
		this.drawTexturedModalRect(0, 0, width, height, width, height);
		
		int k1 = (width - 256) / 2;
		int i1 = (height - 256) / 2;
		this.mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		this.drawTexturedModalRect(k1, i1, 0, 0, 256, 256);

		this.loading.setPosX((width /2) - 100);
		this.loading.setPosY(i1 + 170);
		
		this.loading.render();
		
		this.drawStatus(this.status, width, i1 + 170 + 50);
		
		super.drawScreen(p_render_1_, p_render_2_, p_render_3_);
	}
	
	public void setStatusText(String status) {
		this.status = status;
	}

	public void drawStatus(String text, int width, int height) {
		Minecraft.getMinecraft().fontRenderer.drawString(text, (width - Minecraft.getMinecraft().fontRenderer.getStringWidth(text)) / 2, height, 14821431);
	}
}
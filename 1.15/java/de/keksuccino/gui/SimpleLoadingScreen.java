package de.keksuccino.gui;

import de.keksuccino.rendering.RenderUtils;
import de.keksuccino.rendering.animation.AnimationRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleLoadingScreen extends Screen {
	private static ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojang.png");
	private final Minecraft mc;
	private AnimationRenderer loading = new AnimationRenderer("keksuccino/animations/loading", 15, true, 0, 0, 200, 37, "fancymenu");
	private String status = "";
//	private float progress = 0.0F;
//	private Screen fallbackGui;

	public SimpleLoadingScreen(Minecraft mc) {
		super(new StringTextComponent("loading"));
		this.mc = mc;
	}
	
	@Override
	public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
//		if ((fallbackGui != null) && (this.progress >= 1.0F)) {
//			Minecraft.getInstance().displayGuiScreen(this.fallbackGui);
//			return;
//		}

		this.mc.getTextureManager().bindTexture(RenderUtils.getWhiteImageResource());
		this.blit(0, 0, width, height, width, height);
		
		int k1 = (width - 256) / 2;
		int i1 = (height - 256) / 2;
		this.mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		this.blit(k1, i1, 0, 0, 256, 256);

		this.loading.setPosX((width /2) - 100);
		this.loading.setPosY(i1 + 170);
		
		this.loading.render();
		
		this.drawStatus(this.status, width / 2, i1 + 170 + 50);
		
		super.render(p_render_1_, p_render_2_, p_render_3_);
	}

//	private void drawProgressBar(int fromX, int fromY, int toX, int toY, float progress) {
//		// int i = MathHelper.ceil((float)(toX - fromX - 1));
//		fill(fromX - 1, fromY - 1, toX + 1, toY + 1, -16777216 | Math.round((1.0F - progress) * 255.0F) << 16 | Math.round((1.0F - progress) * 255.0F) << 8 | Math.round((1.0F - progress) * 255.0F));
//		fill(fromX, fromY, toX, toY, -1);
//		fill(fromX + 1, fromY + 1, fromX, toY - 1, -16777216 | (int) MathHelper.lerp(1.0F - progress, 226.0F, 255.0F) << 16 | (int) MathHelper.lerp(1.0F - progress, 40.0F, 255.0F) << 8 | (int) MathHelper.lerp(1.0F - progress, 55.0F, 255.0F));
//	}

//	public void setProgress(float progress) {
//		this.progress = progress;
//	}
	
	public void setStatusText(String status) {
		this.status = status;
	}

	public void drawStatus(String text, int width, int height) {
		Minecraft.getInstance().fontRenderer.drawString(text, (float) (width - Minecraft.getInstance().fontRenderer.getStringWidth(text) / 2), (float) height, 14821431);
	}
}
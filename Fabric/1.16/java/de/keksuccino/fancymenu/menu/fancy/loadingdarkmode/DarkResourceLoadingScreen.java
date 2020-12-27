package de.keksuccino.fancymenu.menu.fancy.loadingdarkmode;

import java.awt.Color;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BackgroundHelper;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceReloadMonitor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class DarkResourceLoadingScreen extends SplashScreen {

	private static final Identifier MOJANG_LOGO_TEXTURE = new Identifier("textures/gui/title/mojangstudios.png");
	private ResourceReloadMonitor asyncReloader;
	private boolean reloading;
	private Consumer<Optional<Throwable>> callback;
	private float progress;
	private long fadeOutStart = -1L;
	private long fadeInStart = -1L;
	
	public DarkResourceLoadingScreen(MinecraftClient mc, ResourceReloadMonitor reloader, Consumer<Optional<Throwable>> callback, boolean reloading) {
		super(mc, reloader, callback, reloading);
		this.asyncReloader = reloader;
		this.reloading = reloading;
		this.callback = callback;
	}

	@Override
	public void render(MatrixStack matrix, int p_render_1_, int p_render_2_, float p_render_3_) {
		MinecraftClient mc = MinecraftClient.getInstance();
		int i = mc.getWindow().getScaledWidth();
		int j = mc.getWindow().getScaledHeight();
		long k = Util.getMeasuringTimeMs();
		if (this.reloading && (this.asyncReloader.isPrepareStageComplete() || mc.currentScreen != null) && this.fadeInStart == -1L) {
			this.fadeInStart = k;
		}

		int color = new Color(26, 26, 26).getRGB();
		float f = this.fadeOutStart > -1L ? (float)(k - this.fadeOutStart) / 1000.0F : -1.0F;
		float f1 = this.fadeInStart > -1L ? (float)(k - this.fadeInStart) / 500.0F : -1.0F;
		float f2;
		if (f >= 1.0F) {
			if (mc.currentScreen != null) {
				mc.currentScreen.render(matrix, 0, 0, p_render_3_);
			}

			int l = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
			fill(matrix, 0, 0, i, j, color | l << 24);
			f2 = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
		} else if (this.reloading) {
			if (mc.currentScreen != null && f1 < 1.0F) {
				mc.currentScreen.render(matrix, p_render_1_, p_render_2_, p_render_2_);
			}

			int i2 = MathHelper.ceil(MathHelper.clamp((double)f1, 0.15D, 1.0D) * 255.0D);
			fill(matrix, 0, 0, i, j, color | i2 << 24);
			f2 = MathHelper.clamp(f1, 0.0F, 1.0F);
		} else {
			fill(matrix, 0, 0, i, j, color);
			f2 = 1.0F;
		}

		int j2 = (int)((double)mc.getWindow().getScaledWidth() * 0.5D);
		int i1 = (int)((double)mc.getWindow().getScaledHeight() * 0.5D);
		double d0 = Math.min((double)mc.getWindow().getScaledWidth() * 0.75D, (double)mc.getWindow().getScaledHeight()) * 0.25D;
		int j1 = (int)(d0 * 0.5D);
		double d1 = d0 * 4.0D;
		int k1 = (int)(d1 * 0.5D);
		mc.getTextureManager().bindTexture(MOJANG_LOGO_TEXTURE);
		RenderSystem.enableBlend();
		RenderSystem.blendEquation(32774);
		RenderSystem.blendFunc(770, 1);
		RenderSystem.alphaFunc(516, 0.0F);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, f2);
		drawTexture(matrix, j2 - k1, i1 - j1, k1, (int)d0, -0.0625F, 0.0F, 120, 60, 120, 120);
		drawTexture(matrix, j2, i1 - j1, k1, (int)d0, 0.0625F, 60.0F, 120, 60, 120, 120);
		RenderSystem.defaultBlendFunc();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.disableBlend();
		int l1 = (int)((double)mc.getWindow().getScaledHeight() * 0.8325D);
		float f3 = this.asyncReloader.getProgress();
		this.progress = MathHelper.clamp(this.progress * 0.95F + f3 * 0.050000012F, 0.0F, 1.0F);
		
		if (f < 1.0F) {
			this.renderProgressBar(matrix, i / 2 - k1, l1 - 5, i / 2 + k1, l1 + 5, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
		}

		if (f >= 2.0F) {
			mc.setOverlay((Overlay)null);
		}

		if (this.fadeOutStart == -1L && this.asyncReloader.isApplyStageComplete() && (!this.reloading || f1 >= 2.0F)) {
			this.fadeOutStart = Util.getMeasuringTimeMs();
			try {
				this.asyncReloader.throwExceptions();
				this.callback.accept(Optional.empty());
			} catch (Throwable throwable) {
				this.callback.accept(Optional.of(throwable));
			}

			if (mc.currentScreen != null) {
				mc.currentScreen.init(mc, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
			}
		}

	}
	
	private void renderProgressBar(MatrixStack matrix, int p_238629_2_, int p_238629_3_, int p_238629_4_, int p_238629_5_, float p_238629_6_) {
		int i = MathHelper.ceil((float)(p_238629_4_ - p_238629_2_ - 2) * this.progress);
		int j = Math.round(p_238629_6_ * 255.0F);
		int k = BackgroundHelper.ColorMixer.getArgb(j, 255, 255, 255);
		fill(matrix, p_238629_2_ + 1, p_238629_3_, p_238629_4_ - 1, p_238629_3_ + 1, k);
		fill(matrix, p_238629_2_ + 1, p_238629_5_, p_238629_4_ - 1, p_238629_5_ - 1, k);
		fill(matrix, p_238629_2_, p_238629_3_, p_238629_2_ + 1, p_238629_5_, k);
		fill(matrix, p_238629_4_, p_238629_3_, p_238629_4_ - 1, p_238629_5_, k);
		fill(matrix, p_238629_2_ + 2, p_238629_3_ + 2, p_238629_2_ + i, p_238629_5_ - 2, k);
	}
	
}

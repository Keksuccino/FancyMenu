package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SplashOverlay.class)
public abstract class MixinSplashScreen extends DrawableHelper {

	protected MinecraftClient mc = MinecraftClient.getInstance();

	@Shadow @Final private boolean reloading;
	@Shadow private long reloadCompleteTime;
	@Shadow private long reloadStartTime;


	@Inject(at = @At("HEAD"), method = "render", cancellable = false)
	protected void onRender(MatrixStack matrix, int mouseX, int mouseY, float partialTicks, CallbackInfo info) {

		if (!FancyMenu.isDrippyLoadingScreenLoaded()) {

			MixinCache.isSplashScreenRendering = true;

			long l = Util.getMeasuringTimeMs();
			float f = this.reloadCompleteTime > -1L ? (float)(l - this.reloadCompleteTime) / 1000.0F : -1.0F;
			float g = this.reloadStartTime > -1L ? (float)(l - this.reloadStartTime) / 500.0F : -1.0F;
			int m;
			if (f >= 1.0F) {
				m = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
				MixinCache.currentSplashAlpha = m;
			} else if (this.reloading) {
				m = MathHelper.ceil(MathHelper.clamp(g, 0.15D, 1.0D) * 255.0D);
				MixinCache.currentSplashAlpha = m;
			} else {
				MixinCache.currentSplashAlpha = 255;
			}

		}
	}

	@Inject(at = @At("HEAD"), method = "renderProgressBar", cancellable = false)
	private void onRenderLoadingBar(MatrixStack matrix, int i1, int i2, int i3, int i4, float f1, CallbackInfo info) {
		MixinCache.isSplashScreenRendering = false;
	}

}


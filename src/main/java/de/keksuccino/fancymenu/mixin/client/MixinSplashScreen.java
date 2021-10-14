package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SplashScreen.class)
public abstract class MixinSplashScreen extends DrawableHelper {

	protected MinecraftClient mc = MinecraftClient.getInstance();

	@Shadow @Final private boolean reloading;
	@Shadow private long applyCompleteTime;
	@Shadow private long prepareCompleteTime;

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"), method = "render")
	private void onRenderCurrentScreenInRender(Screen screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (!MenuCustomization.isMenuCustomizable(screen)) {
			screen.render(matrixStack, mouseX, mouseY, partialTicks);
		}
	}

	@Inject(at = @At("HEAD"), method = "render")
	protected void onRender(MatrixStack matrix, int mouseX, int mouseY, float partialTicks, CallbackInfo info) {

		if (!FancyMenu.isDrippyLoadingScreenLoaded()) {

			MixinCache.isSplashScreenRendering = true;

			long l = Util.getMeasuringTimeMs();
			float f = this.applyCompleteTime > -1L ? (float)(l - this.applyCompleteTime) / 1000.0F : -1.0F;
			float g = this.prepareCompleteTime > -1L ? (float)(l - this.prepareCompleteTime) / 500.0F : -1.0F;
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

	@Inject(at = @At("HEAD"), method = "renderProgressBar")
	private void onRenderLoadingBar(MatrixStack matrix, int i1, int i2, int i3, int i4, float f1, CallbackInfo info) {
		MixinCache.isSplashScreenRendering = false;
	}

}


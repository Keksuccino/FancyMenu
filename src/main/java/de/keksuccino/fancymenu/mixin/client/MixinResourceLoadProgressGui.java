package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.ResourceLoadProgressGui;

@Mixin(value = ResourceLoadProgressGui.class)
public abstract class MixinResourceLoadProgressGui extends AbstractGui {

	@Shadow @Final private boolean reloading;
	@Shadow private long fadeOutStart;
	@Shadow private long fadeInStart;

	//TODO übernehmen
	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V"), method = "render(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V")
	private void onRenderCurrentScreenInRender(Screen screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (!MenuCustomization.isMenuCustomizable(screen)) {
			screen.render(matrixStack, mouseX, mouseY, partialTicks);
		}
	}

	//TODO übernehmen (remove cancellable)
	@Inject(at = @At("HEAD"), method = "render")
	private void onRender(MatrixStack matrix, int mouseX, int mouseY, float partialTicks, CallbackInfo info) {
		if (!FancyMenu.isDrippyLoadingScreenLoaded()) {

			MixinCache.isSplashScreenRendering = true;

			long k = Util.milliTime();

			float f = this.fadeOutStart > -1L ? (float)(k - this.fadeOutStart) / 1000.0F : -1.0F;
			float f1 = this.fadeInStart > -1L ? (float)(k - this.fadeInStart) / 500.0F : -1.0F;
			if (f >= 1.0F) {
				int l = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
				MixinCache.currentSplashAlpha = l;
			} else if (this.reloading) {
				int i2 = MathHelper.ceil(MathHelper.clamp(f1, 0.15D, 1.0D) * 255.0D);
				MixinCache.currentSplashAlpha = i2;
			} else {
				MixinCache.currentSplashAlpha = 255;
			}

		}
	}

	//TODO übernehmen (remove cancellable)
	@Inject(at = @At("HEAD"), method = "func_238629_a_")
	private void onRenderLoadingBar(MatrixStack matrix, int i1, int i2, int i3, int i4, float f1, CallbackInfo info) {
		MixinCache.isSplashScreenRendering = false;
	}

}


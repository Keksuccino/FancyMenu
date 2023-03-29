package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoadingOverlay.class)
public abstract class MixinLoadingOverlay extends GuiComponent {

	@Shadow @Final private boolean fadeIn;
	@Shadow private long fadeOutStart;
	@Shadow private long fadeInStart;

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"), method = "render")
	private void onRenderCurrentScreenInRender(Screen screen, PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (!MenuCustomization.isMenuCustomizable(screen)) {
			screen.render(matrixStack, mouseX, mouseY, partialTicks);
		}
	}

	@Inject(at = @At("HEAD"), method = "render")
	private void onRender(PoseStack matrix, int mouseX, int mouseY, float partialTicks, CallbackInfo info) {
		if (!FancyMenu.isDrippyLoadingScreenLoaded()) {

			MixinCache.isSplashScreenRendering = true;

			long k = Util.getMillis();

			float f = this.fadeOutStart > -1L ? (float)(k - this.fadeOutStart) / 1000.0F : -1.0F;
			float f1 = this.fadeInStart > -1L ? (float)(k - this.fadeInStart) / 500.0F : -1.0F;
			if (f >= 1.0F) {
				int l = Mth.ceil((1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
				MixinCache.currentSplashAlpha = l;
			} else if (this.fadeIn) {
				int i2 = Mth.ceil(Mth.clamp(f1, 0.15D, 1.0D) * 255.0D);
				MixinCache.currentSplashAlpha = i2;
			} else {
				MixinCache.currentSplashAlpha = 255;
			}

		}
	}

	@Inject(at = @At("HEAD"), method = "drawProgressBar")
	private void onRenderLoadingBar(PoseStack matrix, int i1, int i2, int i3, int i4, float f1, CallbackInfo info) {
		MixinCache.isSplashScreenRendering = false;
	}

}


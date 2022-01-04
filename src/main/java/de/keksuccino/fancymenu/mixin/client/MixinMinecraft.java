package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.mixin.cache.MixinCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LoadingGui;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	private static boolean customWindowInit = false;
	
	@Inject(at = @At(value = "HEAD"), method = "getWindowTitle", cancellable = true)
	private void onGetWindowTitle(CallbackInfoReturnable<String> info) {

		if (FancyMenu.config != null) {
			if (!customWindowInit) {
				MainWindowHandler.init();
				MainWindowHandler.updateWindowIcon();
				MainWindowHandler.updateWindowTitle();
				customWindowInit = true;
			}
		}
		
		String title = MainWindowHandler.getCustomWindowTitle();
		if (title != null) {
			info.setReturnValue(title);
		}
		
	}

	@Inject(at = @At(value = "HEAD"), method = "setLoadingGui")
	private void onSetLoadingGui(LoadingGui loadingGuiIn, CallbackInfo info) {
		if (FancyMenu.config == null) {
			return;
		}
		if (loadingGuiIn == null) {
			AnimationHandler.preloadAnimations();
			MixinCache.isSplashScreenRendering = false;
			MenuCustomization.isLoadingScreen = false;
			MenuCustomization.reloadCurrentMenu();
		} else {
			MenuCustomization.isLoadingScreen = true;
		}
	}
	
}

package de.keksuccino.fancymenu.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;

@Mixin(value = MinecraftClient.class)
public class MixinMinecraftClient {
	
	private static boolean customWindowInit = false;

	@Inject(at = @At(value = "HEAD"), method = "getWindowTitle", cancellable = true)
	public void onGetWindowTitle(CallbackInfoReturnable<String> info) {
		
		if ((FancyMenu.config != null) && (MinecraftClient.getInstance().getWindow() != null)) {
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
	
	//TODO neu in 1.17
	@Inject(at = @At(value = "TAIL"), method = "setOverlay")
	private void onSetOverlay(Overlay overlay, CallbackInfo info) {
		
		if (overlay == null) {
			MenuCustomization.isLoadingScreen = false;
			MenuCustomization.reloadCurrentMenu();
		} else {
			MenuCustomization.isLoadingScreen = true;
		}
		
//		if (FancyMenu.config.getOrDefault("loadingscreendarkmode", false)) {
//			if (overlay instanceof SplashScreen) {
//				MinecraftClient.getInstance().overlay = new DarkResourceLoadingScreen(MinecraftClient.getInstance(), getReloader((SplashScreen)overlay), getCallback((SplashScreen)overlay), getReloading((SplashScreen)overlay));
//			}
//		}
		
	}
	
//	private static Consumer<Optional<Throwable>> getCallback(SplashScreen screen) {
//		try {
//			Field f = ReflectionHelper.findField(SplashScreen.class, "exceptionHandler", "field_18218");
//			return (Consumer<Optional<Throwable>>) f.get(screen);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
	
//	private static ResourceReloadMonitor getReloader(SplashScreen screen) {
//		try {
//			Field f = ReflectionHelper.findField(SplashScreen.class, "reloadMonitor", "field_17767");
//			return (ResourceReloadMonitor) f.get(screen);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
	
//	private static boolean getReloading(SplashScreen screen) {
//		try {
//			Field f = ReflectionHelper.findField(SplashScreen.class, "reloading", "field_18219");
//			return (boolean) f.get(screen);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return false;
//	}
	
}

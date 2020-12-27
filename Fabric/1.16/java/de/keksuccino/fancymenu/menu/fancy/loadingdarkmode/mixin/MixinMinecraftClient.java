package de.keksuccino.fancymenu.menu.fancy.loadingdarkmode.mixin;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.loadingdarkmode.DarkResourceLoadingScreen;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.resource.ResourceReloadMonitor;

@Mixin(value = MinecraftClient.class)
public class MixinMinecraftClient {

	@Inject(at = @At(value = "TAIL"), method = "setOverlay")
	private void onSetOverlay(Overlay overlay, CallbackInfo info) {
		if (FancyMenu.config.getOrDefault("loadingscreendarkmode", false)) {
			if (overlay instanceof SplashScreen) {
				MinecraftClient.getInstance().overlay = new DarkResourceLoadingScreen(MinecraftClient.getInstance(), getReloader((SplashScreen)overlay), getCallback((SplashScreen)overlay), getReloading((SplashScreen)overlay));
			}
		}
	}
	
	private static Consumer<Optional<Throwable>> getCallback(SplashScreen screen) {
		try {
			Field f = ReflectionHelper.findField(SplashScreen.class, "exceptionHandler", "field_18218");
			return (Consumer<Optional<Throwable>>) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static ResourceReloadMonitor getReloader(SplashScreen screen) {
		try {
			Field f = ReflectionHelper.findField(SplashScreen.class, "reloadMonitor", "field_17767");
			return (ResourceReloadMonitor) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static boolean getReloading(SplashScreen screen) {
		try {
			Field f = ReflectionHelper.findField(SplashScreen.class, "reloading", "field_18219");
			return (boolean) f.get(screen);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
}

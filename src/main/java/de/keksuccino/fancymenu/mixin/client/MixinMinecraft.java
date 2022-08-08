package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.menu.world.LastWorldHandler;
import net.minecraft.world.WorldSettings;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import net.minecraft.client.Minecraft;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	private static boolean customWindowInit = false;

	@Inject(at = @At("HEAD"), method = "setInitialDisplayMode", cancellable = true)
	private void onSetInitialDisplayMode(CallbackInfo info) {
		FancyMenu.updateConfig();
		if (MainWindowHandler.handleForceFullscreen()) {
			info.cancel();
		}
	}

	@Inject(at = @At(value = "HEAD"), method = "displayGuiScreen", cancellable = true)
	private void onGetWindowTitle(CallbackInfo info) {
		if (!FancyMenu.isKonkreteLoaded()) {
			return;
		}

		try {
			if (FancyMenu.config != null) {
				if (!customWindowInit) {
					MainWindowHandler.init();
					MainWindowHandler.updateWindowIcon();
					String title = MainWindowHandler.getCustomWindowTitle();
					Display.setTitle(title);
					customWindowInit = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Inject(at = @At("HEAD"), method = "launchIntegratedServer")
	private void onLaunchIntegratedServer(String folderName, String worldName, WorldSettings settings, CallbackInfo info) {
		LastWorldHandler.setLastWorld(folderName, false);
	}
	
}

package de.keksuccino.fancymenu.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.MainMenuHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LoadingGui;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	private static boolean customWindowInit = false;
	
	@Inject(at = @At(value = "HEAD"), method = "getWindowTitle", cancellable = true)
	public void onGetWindowTitle(CallbackInfoReturnable<String> info) {

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

	@Inject(at = @At(value = "HEAD"), method = "setLoadingGui", cancellable = false)
	public void onSetLoadingGui(LoadingGui loadingGuiIn, CallbackInfo info) {
		if (loadingGuiIn == null) {
			MainMenuHandler.isLoadingScreen = false;
		}
	}
	
}

package de.keksuccino.fancymenu.mixin.client;

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
	
	@Inject(at = @At(value = "HEAD"), method = "displayGuiScreen", cancellable = true)
	public void onGetWindowTitle(CallbackInfo info) {
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
	
}

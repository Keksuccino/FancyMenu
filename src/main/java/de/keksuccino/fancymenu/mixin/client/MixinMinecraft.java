package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.thread.MainThreadTaskExecutor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.MainWindowHandler;
import net.minecraft.client.Minecraft;


@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	private static boolean customWindowInit = false;

	@Inject(method = "setScreen", at = @At("HEAD"))
	private void beforeSetScreen(Screen screen, CallbackInfo info) {
		//Reset GUI scale in case some layout changed it
		Window m = Minecraft.getInstance().getWindow();
		m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onClientTickPre(CallbackInfo info) {
		for (Runnable r : MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK)) {
			try {
				r.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onClientTickPost(CallbackInfo info) {
		for (Runnable r : MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK)) {
			try {
				r.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "createTitle", cancellable = true)
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

	//TODO übernehmen
	@Inject(method = "setOverlay", at = @At(value = "HEAD"))
	private void onSetOverlayFancyMenu(Overlay overlay, CallbackInfo info) {
		if (overlay == null) {
			//Second attempt on enabling the animation engine and customization engine when loading screen is done (in case something goes wrong in the actual loading screen)
			AnimationHandler.setReady(true);
			MenuCustomization.allowScreenCustomization = true;
		} else {
			//Disable animation engine and customization engine in loading screen to not load the current screen's customizations too early
			AnimationHandler.setReady(false);
			MenuCustomization.allowScreenCustomization = false;
		}
	}
	
}

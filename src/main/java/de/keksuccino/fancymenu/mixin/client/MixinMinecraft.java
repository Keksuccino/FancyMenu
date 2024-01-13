package de.keksuccino.fancymenu.mixin.client;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.events.OpenScreenEvent;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.thread.MainThreadTaskExecutor;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.mainwindow.WindowHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;

@Mixin(Minecraft.class)
public class MixinMinecraft {

	@Inject(method = "<init>", at = @At("RETURN"))
	private void setCustomWindowIconFancyMenu(GameConfig p_91084_, CallbackInfo info) {
		WindowHandler.updateWindowIcon();
	}

	@Inject(at = @At("TAIL"), method = "<init>")
	private void onConstruction(CallbackInfo info) {
		WindowHandler.handleForceFullscreen();
	}

	@Inject(method = "setScreen", at = @At("HEAD"))
	private void beforeSetScreen(Screen screen, CallbackInfo info) {

		//Reset GUI scale in case some layout changed it
		Window m = Minecraft.getInstance().getWindow();
		m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));

		if (screen != null) {
			Konkrete.getEventHandler().callEventsFor(new OpenScreenEvent(screen));
		}

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
	private void setCustomWindowTitleFancyMenu(CallbackInfoReturnable<String> info) {
		WindowHandler.readCustomWindowTitleFromConfig();
		String title = WindowHandler.getCustomWindowTitle();
		if (title != null) {
			info.setReturnValue(title);
		}
	}

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

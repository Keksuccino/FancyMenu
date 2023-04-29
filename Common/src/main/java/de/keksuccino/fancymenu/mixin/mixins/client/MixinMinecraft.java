package de.keksuccino.fancymenu.mixin.mixins.client;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.events.acara.EventHandler;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.screen.OpenScreenEvent;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.threading.MainThreadTaskExecutor;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.window.WindowHandler;
import net.minecraft.client.Minecraft;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	private static boolean customWindowInit = false;

	@Inject(method = "setScreen", at = @At("HEAD"))
	private void beforeSetScreenFancyMenu(Screen screen, CallbackInfo info) {

		//Reset GUI scale in case some layout changed it
		Window m = Minecraft.getInstance().getWindow();
		m.setGuiScale(m.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get()));

		if (screen != null) {
			EventHandler.INSTANCE.postEvent(new OpenScreenEvent.Pre(screen));
		}

	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onClientTickPreFancyMenu(CallbackInfo info) {

		for (Runnable r : MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK)) {
			try {
				r.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		EventHandler.INSTANCE.postEvent(new ClientTickEvent.Pre());

	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onClientTickPostFancyMenu(CallbackInfo info) {

		EventHandler.INSTANCE.postEvent(new ClientTickEvent.Post());

		for (Runnable r : MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK)) {
			try {
				r.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	@Inject(at = @At(value = "HEAD"), method = "createTitle", cancellable = true)
	private void onGetWindowTitleFancyMenu(CallbackInfoReturnable<String> info) {

		if (FancyMenu.getConfig() != null) {
			if (!customWindowInit) {
				WindowHandler.init();
				WindowHandler.updateWindowIcon();
				WindowHandler.updateWindowTitle();
				customWindowInit = true;
			}
		}
		
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

	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;resize(Lnet/minecraft/client/Minecraft;II)V"))
	private void beforeResizeCurrentScreenFancyMenu(CallbackInfo info) {
		InitOrResizeScreenEvent.Pre e = new InitOrResizeScreenEvent.Pre(Minecraft.getInstance().screen);
		EventHandler.INSTANCE.postEvent(e);
	}

	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;resize(Lnet/minecraft/client/Minecraft;II)V", shift = At.Shift.AFTER))
	private void afterResizeCurrentScreenFancyMenu(CallbackInfo info) {
		InitOrResizeScreenEvent.Post e = new InitOrResizeScreenEvent.Post(Minecraft.getInstance().screen);
		EventHandler.INSTANCE.postEvent(e);
		InitOrResizeScreenCompletedEvent e2 = new InitOrResizeScreenCompletedEvent(Minecraft.getInstance().screen);
		EventHandler.INSTANCE.postEvent(e2);
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V"))
	private void beforeInitCurrentScreenFancyMenu(Screen screen, CallbackInfo info) {
		InitOrResizeScreenEvent.Pre e = new InitOrResizeScreenEvent.Pre(Minecraft.getInstance().screen);
		EventHandler.INSTANCE.postEvent(e);
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V", shift = At.Shift.AFTER))
	private void afterInitCurrentScreenFancyMenu(Screen screen, CallbackInfo info) {
		InitOrResizeScreenEvent.Post e = new InitOrResizeScreenEvent.Post(Minecraft.getInstance().screen);
		EventHandler.INSTANCE.postEvent(e);
		InitOrResizeScreenCompletedEvent e2 = new InitOrResizeScreenCompletedEvent(Minecraft.getInstance().screen);
		EventHandler.INSTANCE.postEvent(e2);
	}
	
}

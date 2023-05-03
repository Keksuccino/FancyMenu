package de.keksuccino.fancymenu.mixin.mixins.client;

import de.keksuccino.fancymenu.event.acara.EventHandler;
import de.keksuccino.fancymenu.event.events.screen.*;
import de.keksuccino.fancymenu.event.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.customization.backend.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.MenuCustomization;
import de.keksuccino.fancymenu.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.utils.RenderUtils;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.keksuccino.fancymenu.window.WindowHandler;
import net.minecraft.client.Minecraft;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	@Shadow @Nullable public Screen screen;

	private static boolean customWindowInit = false;

	@Inject(method = "tick", at = @At("HEAD"))
	private void beforeGameTickFancyMenu(CallbackInfo info) {
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
	private void afterGameTickFancyMenu(CallbackInfo info) {
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
	private void changeWindowTitleFancyMenu(CallbackInfoReturnable<String> info) {
		//TODO WindowHandler rewriten
		if (!customWindowInit) {
			WindowHandler.init();
			WindowHandler.updateWindowIcon();
			WindowHandler.updateWindowTitle();
			customWindowInit = true;
		}
		String title = WindowHandler.getCustomWindowTitle();
		if (title != null) {
			info.setReturnValue(title);
		}
	}

	@Inject(method = "setOverlay", at = @At(value = "HEAD"))
	private void headSetOverlayFancyMenu(Overlay overlay, CallbackInfo info) {
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

	@Inject(method = "setScreen", at = @At("HEAD"))
	private void headSetScreenFancyMenu(Screen screen, CallbackInfo info) {
		//Reset GUI scale in case some layout changed it
		RenderUtils.resetGuiScale();
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;reset()V", shift = At.Shift.AFTER))
	private void beforeInitCurrentScreenFancyMenu(Screen screen, CallbackInfo info) {
		if (screen != null) {
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(screen));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(screen));
		}
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateTitle()V"))
	private void afterInitCurrentScreenFancyMenu(Screen screen, CallbackInfo info) {
		if (screen != null) {
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(screen));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(screen));
		}
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
	private void beforeCloseScreenFancyMenu(Screen screen, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new CloseScreenEvent(this.screen));
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
	private void beforeOpenScreenFancyMenu(Screen screen, CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new OpenScreenEvent(this.screen));
	}

	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V", shift = At.Shift.AFTER))
	private void beforeResizeCurrentScreenFancyMenu(CallbackInfo info) {
		if (this.screen != null) {
			RenderUtils.resetGuiScale();
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(screen));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(this.screen));
		}
	}

	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
	private void afterResizeCurrentScreenFancyMenu(CallbackInfo info) {
		if (this.screen != null) {
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(this.screen));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(this.screen));
		}
	}
	
}

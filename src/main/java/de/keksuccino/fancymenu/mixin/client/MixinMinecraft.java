package de.keksuccino.fancymenu.mixin.client;

import de.keksuccino.fancymenu.events.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.InitOrResizeScreenEvent;
import de.keksuccino.fancymenu.events.InitOrResizeScreenStartingEvent;
import de.keksuccino.fancymenu.events.OpenScreenEvent;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.thread.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.v3.rendering.RenderingUtils;
import de.keksuccino.konkrete.Konkrete;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.keksuccino.fancymenu.mainwindow.WindowHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;

@Mixin(Minecraft.class)
public class MixinMinecraft {

	@Shadow @Nullable public Screen screen;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void setCustomWindowIconFancyMenu(GameConfig p_91084_, CallbackInfo info) {
		WindowHandler.updateWindowIcon();
	}

	@Inject(at = @At("TAIL"), method = "<init>")
	private void onConstructFancyMenu(CallbackInfo info) {
		WindowHandler.handleForceFullscreen();
	}

	//TODO übernehmen
	@Inject(method = "setScreen", at = @At("HEAD"))
	private void headSetScreenFancyMenu(Screen screen, CallbackInfo info) {
		//Reset GUI scale in case some layout changed it
		RenderingUtils.resetGuiScale();
	}

	//TODO übernehmen
	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
	private void beforeScreenAddedFancyMenu(Screen screen, CallbackInfo info) {
		if (this.screen == null) return;
		Konkrete.getEventHandler().callEventsFor(new OpenScreenEvent(this.screen));
	}

	//TODO übernehmen
	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;reset()V", shift = At.Shift.AFTER))
	private void beforeInitCurrentScreenFancyMenu(Screen screen, CallbackInfo info) {
		if (screen != null) {
			Konkrete.getEventHandler().callEventsFor(new InitOrResizeScreenStartingEvent(screen));
			Konkrete.getEventHandler().callEventsFor(new InitOrResizeScreenEvent.Pre(screen));
		}
	}

	//TODO übernehmen
	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateTitle()V"))
	private void afterInitCurrentScreenFancyMenu(Screen screen, CallbackInfo info) {
		if (screen != null) {
			Konkrete.getEventHandler().callEventsFor(new InitOrResizeScreenEvent.Post(screen));
			Konkrete.getEventHandler().callEventsFor(new InitOrResizeScreenCompletedEvent(screen));
		}
	}

	//TODO übernehmen
	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V", shift = At.Shift.AFTER))
	private void beforeResizeCurrentScreenFancyMenu(CallbackInfo info) {
		if (this.screen != null) {
			RenderingUtils.resetGuiScale();
			Konkrete.getEventHandler().callEventsFor(new InitOrResizeScreenStartingEvent(this.screen));
			Konkrete.getEventHandler().callEventsFor(new InitOrResizeScreenEvent.Pre(this.screen));
		}
	}

	//TODO übernehmen
	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
	private void afterResizeCurrentScreenFancyMenu(CallbackInfo info) {
		if (this.screen != null) {
			Konkrete.getEventHandler().callEventsFor(new InitOrResizeScreenEvent.Post(this.screen));
			Konkrete.getEventHandler().callEventsFor(new InitOrResizeScreenCompletedEvent(this.screen));
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
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onClientTickPostFancyMenu(CallbackInfo info) {
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

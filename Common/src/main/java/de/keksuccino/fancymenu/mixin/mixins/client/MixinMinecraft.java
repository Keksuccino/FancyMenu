package de.keksuccino.fancymenu.mixin.mixins.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import java.util.Objects;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	@Shadow @Nullable public Screen screen;

	@Unique private boolean lateClientInitDone = false;

	@Inject(method = "setOverlay", at = @At("HEAD"))
	private void beforeSetOverlayFancyMenu(Overlay overlay, CallbackInfo info) {
		if (!this.lateClientInitDone) {
			this.lateClientInitDone = true;
			FancyMenu.lateClientInit();
		}
	}

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

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
	private void beforeScreenTickFancyMenu(CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new ScreenTickEvent.Pre(Objects.requireNonNull(this.screen)));
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V", shift = At.Shift.AFTER))
	private void afterScreenTickFancyMenu(CallbackInfo info) {
		EventHandler.INSTANCE.postEvent(new ScreenTickEvent.Post(Objects.requireNonNull(this.screen)));
	}
	
	@Inject(at = @At(value = "HEAD"), method = "createTitle", cancellable = true)
	private void changeWindowTitleFancyMenu(CallbackInfoReturnable<String> info) {
		String title = WindowHandler.getCustomWindowTitle();
		if (title != null) {
			info.setReturnValue(title);
		}
	}

	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void headSetScreenFancyMenu(Screen screen, CallbackInfo info) {

		//Reset GUI scale in case some layout changed it
		RenderingUtils.resetGuiScale();

		//Handle Overrides
		Screen overrideWith = CustomGuiHandler.beforeSetScreen(screen);
		if (overrideWith != null) {
			info.cancel();
			Minecraft.getInstance().setScreen(overrideWith);
		}

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
			RenderingUtils.resetGuiScale();
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

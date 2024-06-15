package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.preload.ResourcePreLoader;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
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

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	@Unique private static final String DUMMY_RESOURCE_RELOAD_LISTENER_RETURN_VALUE_FANCYMENU = "PREPARE RETURN VALUE";
	@Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

	@Unique private static boolean reloadListenerRegisteredFancyMenu = false;
	@Unique private boolean lateClientInitDoneFancyMenu = false;

	@Shadow @Nullable public Screen screen;

	@Inject(method = "setOverlay", at = @At("HEAD"))
	private void beforeSetOverlayFancyMenu(Overlay overlay, CallbackInfo info) {
		if (!this.lateClientInitDoneFancyMenu) {
			this.lateClientInitDoneFancyMenu = true;
			FancyMenu.lateClientInit();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void beforeGameTickFancyMenu(CallbackInfo info) {
		for (Runnable r : MainThreadTaskExecutor.getAndClearQueue(MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK)) {
			try {
				r.run();
			} catch (Exception e) {
				LOGGER_FANCYMENU.error("[FANCYMENU] Error while executing PRE_CLIENT_TICK MainThread task!", e);
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
				LOGGER_FANCYMENU.error("[FANCYMENU] Error while executing POST_CLIENT_TICK MainThread task!", e);
			}
		}
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
	private void beforeScreenTickFancyMenu(CallbackInfo info) {
		if (this.screen == null) return;
		EventHandler.INSTANCE.postEvent(new ScreenTickEvent.Pre(this.screen));
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V", shift = At.Shift.AFTER))
	private void afterScreenTickFancyMenu(CallbackInfo info) {
		if (this.screen == null) return;
		EventHandler.INSTANCE.postEvent(new ScreenTickEvent.Post(this.screen));
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
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(screen, InitOrResizeScreenEvent.InitializationPhase.INIT));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(screen, InitOrResizeScreenEvent.InitializationPhase.INIT));
		}
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateTitle()V"))
	private void afterInitCurrentScreenFancyMenu(Screen screen, CallbackInfo info) {
		if (screen != null) {
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(screen, InitOrResizeScreenEvent.InitializationPhase.INIT));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(screen, InitOrResizeScreenEvent.InitializationPhase.INIT));
			EventHandler.INSTANCE.postEvent(new OpenScreenPostInitEvent(screen));
		}
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
	private void beforeScreenRemovedFancyMenu(Screen screen, CallbackInfo info) {
		if (this.screen == null) return;
		EventHandler.INSTANCE.postEvent(new CloseScreenEvent(this.screen));
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
	private void beforeScreenAddedFancyMenu(Screen screen, CallbackInfo info) {
		if (this.screen == null) return;
		EventHandler.INSTANCE.postEvent(new OpenScreenEvent(this.screen));
	}

	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V", shift = At.Shift.AFTER))
	private void beforeResizeCurrentScreenFancyMenu(CallbackInfo info) {
		if (this.screen != null) {
			RenderingUtils.resetGuiScale();
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(this.screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(this.screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
		}
	}

	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
	private void afterResizeCurrentScreenFancyMenu(CallbackInfo info) {
		if (this.screen != null) {
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(this.screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(this.screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
		}
	}

	//This is a hacky way to get Minecraft to register FancyMenu's reload listener as early as possible in the Minecraft.class constructor
	@Inject(method = "resizeDisplay", at = @At("HEAD"))
	private void registerResourceReloadListenerInResizeDisplayFancyMenu(CallbackInfo info) {
		if (!reloadListenerRegisteredFancyMenu) {
			reloadListenerRegisteredFancyMenu = true;
			Minecraft mc = (Minecraft)((Object)this);
			LOGGER_FANCYMENU.info("[FANCYMENU] Registering resource reload listener..");
			if (mc.getResourceManager() instanceof ReloadableResourceManager r) {
				r.registerReloadListener(new SimplePreparableReloadListener<String>() {
					@Override
					protected @NotNull String prepare(@NotNull ResourceManager var1, @NotNull ProfilerFiller var2) {
						return DUMMY_RESOURCE_RELOAD_LISTENER_RETURN_VALUE_FANCYMENU;
					}
					@Override
					protected void apply(@NotNull String prepareReturnValue, @NotNull ResourceManager var2, @NotNull ProfilerFiller var3) {
						ResourceHandlers.reloadAll();
						ResourcePreLoader.preLoadAll(120000); //waits for 120 seconds per resource
					}
				});
			}
		}
	}

}

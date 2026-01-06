package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.WelcomeScreen;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.WorldSessionTracker;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.preload.ResourcePreLoader;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import java.net.SocketAddress;
import java.nio.file.Path;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
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
    @Unique private static final String UNKNOWN_SERVER_IP_FANCYMENU = "ERROR";
	@Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

	@Unique private static boolean reloadListenerRegisteredFancyMenu = false;
	@Unique private boolean lateClientInitDoneFancyMenu = false;
	@Unique private Screen lastScreen_FancyMenu = null;
	@Unique private boolean hasActiveServerConnection_FancyMenu;
	@Unique private boolean pendingServerJoinEvent_FancyMenu;
	@Unique @Nullable private String lastServerIp_FancyMenu;
	@Unique private boolean quitListenerFired_FancyMenu;

	@Shadow @Nullable public Screen screen;
	@Shadow @Nullable public ClientLevel level;
	@Shadow @Nullable public LocalPlayer player;

	@Inject(method = "stop", at = @At("HEAD"))
	private void before_stop_FancyMenu(CallbackInfo info) {
		if (!this.quitListenerFired_FancyMenu) {
			this.quitListenerFired_FancyMenu = true;
			Listeners.ON_QUIT_MINECRAFT.onQuitMinecraft();
		}
	}

	@Inject(method = "doWorldLoad(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Z)V", at = @At("HEAD"))
	private void before_doWorldLoad_FancyMenu(LevelStorageAccess levelStorage, PackRepository packRepository, WorldStem worldStem, boolean newWorld, CallbackInfo info) {
		try {
			if (levelStorage != null && worldStem != null) {
				Path savePath = levelStorage.getLevelPath(LevelResource.ROOT).toAbsolutePath();
				String iconPath = levelStorage.getIconFile().map(path -> path.toAbsolutePath().toString()).orElse(null);
				String worldName = worldStem.worldData().getLevelName();
				WorldSessionTracker.prepareSession(worldName, savePath.toString(), iconPath, newWorld);
			} else {
				WorldSessionTracker.clearSession();
			}
		} catch (Exception ex) {
			LOGGER_FANCYMENU.error("[FANCYMENU] Failed to prepare world session data!", ex);
			WorldSessionTracker.clearSession();
		}
	}

	@Inject(method = "setOverlay", at = @At("HEAD"))
	private void beforeSetOverlayFancyMenu(Overlay overlay, CallbackInfo info) {
		if (!this.lateClientInitDoneFancyMenu) {
			this.lateClientInitDoneFancyMenu = true;
			FancyMenu.lateClientInit();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void head_tick_FancyMenu(CallbackInfo info) {

        ScreenOverlayHandler.INSTANCE.tick();

		if (this.pendingServerJoinEvent_FancyMenu && this.player != null) {
			this.fireServerJoined_FancyMenu();
		}

		if (MCEFUtil.isMCEFLoaded()) BrowserHandler.tick();

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

	@Inject(method = "setLevel", at = @At("TAIL"))
	private void afterSetLevelFancyMenu(ClientLevel clientLevel, ReceivingLevelScreen.Reason reason, CallbackInfo info) {
		Minecraft self = (Minecraft)(Object)this;

		if (clientLevel == null) {
			return;
		}
		if (self.isLocalServer()) {
			return;
		}
		if (this.hasActiveServerConnection_FancyMenu || this.pendingServerJoinEvent_FancyMenu) {
			return;
		}

		String serverIp = this.fetchCurrentServerIp_FancyMenu();
		this.lastServerIp_FancyMenu = serverIp;

		if (this.player != null) {
			this.fireServerJoined_FancyMenu();
		} else {
			this.pendingServerJoinEvent_FancyMenu = true;
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
	private void before_setScreen_FancyMenu(Screen screen, CallbackInfo info) {

//        // This routes setScreen() calls inside PipWindows through the actual window instead of normal MC
//        PiPWindow pip = PiPWindowHandler.INSTANCE.getLastClickedWindowThisTick();
//        if (pip != null) {
//            pip.setScreen(screen);
//            info.cancel();
//            return;
//        }

		// This is just for giving FM the correct screen identifiers for all possible scenarios
		if ((screen == null) && (this.level == null)) {
			screen = new TitleScreen();
		} else if ((screen == null) && ((this.player != null) && this.player.isDeadOrDying())) {
			if (this.player.shouldShowDeathScreen()) {
				screen = new DeathScreen(null, this.level.getLevelData().isHardcore());
			}
		}
		final Screen finalScreen = screen;

		if ((Minecraft.getInstance().screen instanceof LayoutEditorScreen e) && !(screen instanceof LayoutEditorScreen)) {
			e.layout.menuBackgrounds.forEach(menuBackground -> {
				menuBackground.onCloseScreen(e, finalScreen);
				menuBackground.onDisableOrRemove();
			});
			e.getAllElements().forEach(element -> {
				element.element.onCloseScreen(e, finalScreen);
				element.element.onDestroyElement();
			});
		}

		if (screen instanceof LayoutEditorScreen e) {
			e.justOpened = true;
		}

		this.lastScreen_FancyMenu = this.screen;

		//Reset GUI scale in case some layout changed it
		RenderingUtils.resetGuiScale();

		if (FancyMenu.getOptions().showWelcomeScreen.getValue() && (screen instanceof TitleScreen)) {
			info.cancel();
			Minecraft.getInstance().setScreen(new WelcomeScreen(screen));
			return;
		}

		//Handle Overrides
		Screen overrideWith = CustomGuiHandler.beforeSetScreen(screen);
		if (overrideWith != null) {
			info.cancel();
			Minecraft.getInstance().setScreen(overrideWith);
			return;
		}

		if ((screen != null) && (screen != this.screen)) {
			Screen cachedCurrent = this.screen;
			Listeners.ON_OPEN_SCREEN.onScreenOpened(screen);
			if (cachedCurrent != this.screen) {
				info.cancel();
			}
		}

	}

	@Inject(method = "setScreen", at = @At("RETURN"))
	private void after_setScreen_FancyMenu(Screen screen, CallbackInfo info) {

		boolean newScreenType = false;
		if ((this.lastScreen_FancyMenu == null) && (this.screen != null)) {
			newScreenType = true;
		} else if ((this.lastScreen_FancyMenu != null) && (this.screen == null)) {
			newScreenType = true;
		} else if ((this.lastScreen_FancyMenu != null) && (this.screen != null)) {
			String lastId = ScreenIdentifierHandler.getIdentifierOfScreen(this.lastScreen_FancyMenu);
			String newId = ScreenIdentifierHandler.getIdentifierOfScreen(this.screen);
			if (!lastId.equals(newId)) {
				newScreenType = true;
			}
		}

		if (newScreenType) ScreenCustomization.onSwitchingToNewScreenType(this.screen, this.lastScreen_FancyMenu);

	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
	private void beforeDisconnectFancyMenu(Screen screen, boolean keepDownloadedResourcePacks, CallbackInfo info) {
		this.fireServerLeft_FancyMenu();
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.BEFORE))
	private void beforeLevelClearedWorldLeftFancyMenu(Screen screen, boolean keepDownloadedResourcePacks, CallbackInfo info) {
		WorldSessionTracker.handleWorldLeft((Minecraft)(Object)this);
	}
	@Inject(method = "clearClientLevel", at = @At("HEAD"))
	private void beforeClearClientLevelFancyMenu(Screen nextScreen, CallbackInfo info) {
		WorldSessionTracker.captureSnapshot((Minecraft) (Object) this);
		this.fireServerLeft_FancyMenu();
		WorldSessionTracker.handleWorldLeft((Minecraft) (Object) this);
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
			ScrollScreenNormalizer.normalizeScrollableScreen(screen);
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(screen, InitOrResizeScreenEvent.InitializationPhase.INIT));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(screen, InitOrResizeScreenEvent.InitializationPhase.INIT));
			EventHandler.INSTANCE.postEvent(new OpenScreenPostInitEvent(screen));
		}
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
	private void beforeScreenRemovedFancyMenu(Screen screen, CallbackInfo info) {
		if (this.screen == null) return;
		EventHandler.INSTANCE.postEvent(new CloseScreenEvent(this.screen, screen));
	}

	/** @reason Fire FancyMenu close screen listeners after the screen was removed. */
	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V", shift = At.Shift.AFTER))
	private void afterScreenRemovedFancyMenu(Screen screen, CallbackInfo info) {
		if (this.lastScreen_FancyMenu != null) {
			Listeners.ON_CLOSE_SCREEN.onScreenClosed(this.lastScreen_FancyMenu);
		}
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
			ScrollScreenNormalizer.normalizeScrollableScreen(this.screen);
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

	@Unique
	private void fireServerLeft_FancyMenu() {
		if (!this.hasActiveServerConnection_FancyMenu) {
			this.pendingServerJoinEvent_FancyMenu = false;
			this.lastServerIp_FancyMenu = null;
			return;
		}

		String serverIp = (this.lastServerIp_FancyMenu != null && !this.lastServerIp_FancyMenu.isBlank())
				? this.lastServerIp_FancyMenu
				: UNKNOWN_SERVER_IP_FANCYMENU;
		Listeners.ON_SERVER_LEFT.onServerLeft(serverIp);
		this.hasActiveServerConnection_FancyMenu = false;
		this.pendingServerJoinEvent_FancyMenu = false;
		this.lastServerIp_FancyMenu = null;
	}

	@Unique
	private void fireServerJoined_FancyMenu() {
		if (this.hasActiveServerConnection_FancyMenu) {
			return;
		}
		if (this.lastServerIp_FancyMenu == null || this.lastServerIp_FancyMenu.isBlank() || UNKNOWN_SERVER_IP_FANCYMENU.equals(this.lastServerIp_FancyMenu)) {
			this.lastServerIp_FancyMenu = this.fetchCurrentServerIp_FancyMenu();
		}

		String serverIp = (this.lastServerIp_FancyMenu != null && !this.lastServerIp_FancyMenu.isBlank())
				? this.lastServerIp_FancyMenu
				: UNKNOWN_SERVER_IP_FANCYMENU;
		this.pendingServerJoinEvent_FancyMenu = false;
		this.hasActiveServerConnection_FancyMenu = true;
		Listeners.ON_SERVER_JOINED.onServerJoined(serverIp);
	}

	@Unique
	private String fetchCurrentServerIp_FancyMenu() {
		Minecraft self = (Minecraft)(Object)this;
		ServerData serverData = self.getCurrentServer();
		if (serverData != null && serverData.ip != null && !serverData.ip.isBlank()) {
			return serverData.ip;
		}

		ClientPacketListener listener = self.getConnection();
		if (listener != null) {
			Connection connection = listener.getConnection();
			if (connection != null) {
				SocketAddress address = connection.getRemoteAddress();
				if (address != null) {
					String resolved = address.toString();
					if (resolved != null) {
						resolved = resolved.trim();
						if (!resolved.isEmpty()) {
							if (resolved.startsWith("/")) {
								resolved = resolved.substring(1);
							}
							if (!resolved.isEmpty()) {
								return resolved;
							}
						}
					}
				}
			}
		}

		return UNKNOWN_SERVER_IP_FANCYMENU;
	}

}
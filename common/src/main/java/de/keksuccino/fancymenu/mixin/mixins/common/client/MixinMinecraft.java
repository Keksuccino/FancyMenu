package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.WelcomeWindowBody;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.WorldSessionTracker;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.rinku.BrowserHandler;
import de.keksuccino.fancymenu.util.rinku.RinkuUtil;
import de.keksuccino.fancymenu.util.lifecycle.ClientShutdownHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PipableScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.player.CameraRotationObserver;
import de.keksuccino.fancymenu.util.player.PlayerPositionObserver;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.window.InitialLoadingOverlayIconRefreshController;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import java.net.SocketAddress;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {

	@Shadow @Nullable public Screen screen;
	@Shadow @Nullable public ClientLevel level;
	@Shadow @Nullable public LocalPlayer player;
	@Shadow @Nullable private Overlay overlay;

    @Unique private static final String UNKNOWN_SERVER_IP_FANCYMENU = "ERROR";
	@Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();
	@Unique private boolean lateClientInitDone_FancyMenu = false;
	@Unique @Nullable private Screen lastScreen_FancyMenu = null;
	@Unique private boolean hasActiveServerConnection_FancyMenu;
	@Unique private boolean pendingServerJoinEvent_FancyMenu;
	@Unique @Nullable private String lastServerIp_FancyMenu;
	@Unique private boolean quitListenerFired_FancyMenu;
	@Unique private final InitialLoadingOverlayIconRefreshController initialLoadingOverlayIconRefreshController_FancyMenu = new InitialLoadingOverlayIconRefreshController();

	@Inject(method = "stop", at = @At("HEAD"))
	private void before_stop_FancyMenu(CallbackInfo info) {
		ClientShutdownHandler.shutdown();
		if (!this.quitListenerFired_FancyMenu) {
			this.quitListenerFired_FancyMenu = true;
			if (Listeners.ON_QUIT_MINECRAFT.hasInstancesListening()) Listeners.ON_QUIT_MINECRAFT.onQuitMinecraft();
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
	private void before_setOverlay_FancyMenu(@Nullable Overlay overlay, CallbackInfo info) {
		if ((this.overlay != overlay) && (this.overlay instanceof GameIntroOverlay gameIntroOverlay)) {
			gameIntroOverlay.onReplaced();
		}
		if (!this.lateClientInitDone_FancyMenu) {
			this.lateClientInitDone_FancyMenu = true;
			FancyMenu.lateClientInit();
		}
	}

	/** @reason The initial icon updates happen before resource loading; refreshing after the accepted loading-overlay exit makes the custom icon visible before the first unobscured screen frame. */
	@Inject(method = "setOverlay", at = @At("TAIL"))
	private void after_setOverlay_FancyMenu(@Nullable Overlay overlay, CallbackInfo info) {
		if (this.initialLoadingOverlayIconRefreshController_FancyMenu.afterOverlayAssignment(this.overlay instanceof LoadingOverlay)) {
			WindowHandler.updateCustomWindowIcon();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void head_tick_FancyMenu(CallbackInfo info) {

        MouseUtil.tick();

        ScreenOverlayHandler.INSTANCE.tick();
        CameraRotationObserver.tick();
        PlayerPositionObserver.tick();

		if (this.pendingServerJoinEvent_FancyMenu && this.player != null) {
			this.fireServerJoined_FancyMenu();
		}

		if (RinkuUtil.isRinkuLoaded()) BrowserHandler.tick();

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
	private void afterSetLevelFancyMenu(ClientLevel clientLevel, CallbackInfo info) {
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
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;tick()V"))
	private void beforeScreenTickFancyMenu(CallbackInfo info) {
		if (this.screen == null) return;
		EventHandler.INSTANCE.postEvent(new ScreenTickEvent.Pre(this.screen));
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;tick()V", shift = At.Shift.AFTER))
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

        if (ScreenUtils.areSetScreenCallsBlocked()) {
            info.cancel();
            return;
        }

//        // This routes setScreen() calls inside PipWindows through the actual window instead of normal MC
//        PiPWindow pip = PiPWindowHandler.INSTANCE.getLastClickedWindowThisTick();
//        if (pip != null) {
//            pip.setScreen(screen);
//            info.cancel();
//            return;
//        }

        if (screen instanceof PipableScreen) {
            throw new RuntimeException("[FANCYMENU] PipableScreens can't be set as normal screens! They are meant to be used only for PiPWindows! Failed to open as normal screen: " + screen);
        }

		// This is just for giving FM the correct screen identifiers for all possible scenarios
		if ((screen == null) && (this.level == null)) {
			screen = new TitleScreen();
		} else if ((screen == null) && ((this.player != null) && this.player.isDeadOrDying())) {
			if (this.player.shouldShowDeathScreen()) {
				screen = new DeathScreen(null, this.level.getLevelData().isHardcore(), this.player);
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

		// Reset GUI scale in case some layout changed it
		RenderingUtils.resetGuiScale();

        // Open Welcome window
		if (FancyMenu.getOptions().showWelcomeScreen.getValue() && !FancyMenu.getOptions().modpackMode.getValue() && (screen instanceof TitleScreen)) {
            FancyMenu.getOptions().showWelcomeScreen.setValue(false);
			WelcomeWindowBody.openInWindow();
		}

		// Handle Overrides
		Screen overrideWith = CustomGuiHandler.beforeSetScreen(screen);
		if (overrideWith != null) {
			info.cancel();
			Minecraft.getInstance().setScreen(overrideWith);
			return;
		}

		if ((screen != null) && (screen != this.screen) && Listeners.ON_OPEN_SCREEN.hasInstancesListening()) {
			Screen cachedCurrent = this.screen;
			Listeners.ON_OPEN_SCREEN.onScreenOpened(screen);
			if (cachedCurrent != this.screen) {
				info.cancel();
			}
		}

	}

	/** @reason The replacement screen must not receive the remainder of a press that started on an overlay from the old screen. */
	@Inject(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
	private void after_screenAssignment_in_setScreen_FancyMenu(Screen screen, CallbackInfo info) {
		// NeoForge can cancel its opening event before this assignment, so resetting at setScreen HEAD would detach a press from a screen that remains active.
		ScreenOverlayHandler.INSTANCE.detachMouseCaptures();
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

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
	private void beforeDisconnectFancyMenu(Screen screen, boolean keepDownloadedResourcePacks, boolean updateLevelInEngines, CallbackInfo info) {
		this.fireServerLeft_FancyMenu();
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("RETURN"))
	private void afterDisconnectFancyMenu(Screen screen, boolean keepDownloadedResourcePacks, boolean updateLevelInEngines, CallbackInfo info) {
		SeamlessWorldLoadingHandler.clearCapture();
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.BEFORE))
	private void beforeLevelClearedWorldLeftFancyMenu(Screen screen, boolean keepDownloadedResourcePacks, boolean updateLevelInEngines, CallbackInfo info) {
		WorldSessionTracker.handleWorldLeft((Minecraft)(Object)this);
	}

	@Inject(method = "clearClientLevel", at = @At("HEAD"))
	private void beforeClearClientLevelFancyMenu(Screen nextScreen, CallbackInfo info) {
		WorldSessionTracker.captureSnapshot((Minecraft) (Object) this);
		this.fireServerLeft_FancyMenu();
		WorldSessionTracker.handleWorldLeft((Minecraft) (Object) this);
	}

	@Inject(method = "clearClientLevel", at = @At("RETURN"))
	private void afterClearClientLevelFancyMenu(Screen nextScreen, CallbackInfo info) {
		SeamlessWorldLoadingHandler.clearCapture();
	}

	/** @reason Init.Pre listeners may change the GUI scale, so init needs the refreshed scaled dimensions. */
	@WrapOperation(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;init(II)V"))
	private void wrap_init_FancyMenu(Screen instance, int width, int height, Operation<Void> original) {
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(instance, InitOrResizeScreenEvent.InitializationPhase.INIT));
		EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(instance, InitOrResizeScreenEvent.InitializationPhase.INIT));
		Window window = Minecraft.getInstance().getWindow();
		original.call(instance, window.getGuiScaledWidth(), window.getGuiScaledHeight());
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
		if (this.lastScreen_FancyMenu != null && Listeners.ON_CLOSE_SCREEN.hasInstancesListening()) {
			Listeners.ON_CLOSE_SCREEN.onScreenClosed(this.lastScreen_FancyMenu);
		}
	}

	@Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;added()V"))
	private void beforeScreenAddedFancyMenu(Screen screen, CallbackInfo info) {
		if (this.screen == null) return;
		EventHandler.INSTANCE.postEvent(new OpenScreenEvent(this.screen));
	}


	@Inject(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(I)V", shift = At.Shift.AFTER))
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
	@Unique
	private void fireServerLeft_FancyMenu() {
		if (!this.hasActiveServerConnection_FancyMenu) {
			SeamlessWorldLoadingHandler.finishServerLoad();
			this.pendingServerJoinEvent_FancyMenu = false;
			this.lastServerIp_FancyMenu = null;
			return;
		}

		String serverIp = (this.lastServerIp_FancyMenu != null && !this.lastServerIp_FancyMenu.isBlank())
				? this.lastServerIp_FancyMenu
				: UNKNOWN_SERVER_IP_FANCYMENU;
		if (!UNKNOWN_SERVER_IP_FANCYMENU.equals(serverIp)) {
			SeamlessWorldLoadingHandler.saveAndClearServerCapture(serverIp);
		}
		SeamlessWorldLoadingHandler.finishServerLoad();
		if (Listeners.ON_SERVER_LEFT.hasInstancesListening()) Listeners.ON_SERVER_LEFT.onServerLeft(serverIp);
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
		if (!UNKNOWN_SERVER_IP_FANCYMENU.equals(serverIp)) {
			SeamlessWorldLoadingHandler.startServerCapture(serverIp);
		}
		SeamlessWorldLoadingHandler.finishServerLoad();
		if (Listeners.ON_SERVER_JOINED.hasInstancesListening()) Listeners.ON_SERVER_JOINED.onServerJoined(serverIp);
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

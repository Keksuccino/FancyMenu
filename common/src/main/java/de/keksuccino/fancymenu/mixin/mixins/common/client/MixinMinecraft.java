package de.keksuccino.fancymenu.mixin.mixins.common.client;

import de.keksuccino.fancymenu.customization.global.SeamlessWorldLoadingHandler;
import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtBlockListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtEntityListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStopLookingAtBlockListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStopLookingAtEntityListener;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.WorldSessionTracker;
import de.keksuccino.fancymenu.util.MouseUtil;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.events.screen.*;
import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ScreenOverlayHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer.ScrollScreenNormalizer;
import de.keksuccino.fancymenu.util.player.CameraRotationObserver;
import de.keksuccino.fancymenu.util.player.PlayerPositionObserver;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.preload.ResourcePreLoader;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.Screen;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
	@Unique private static final double ENTITY_LOOK_DISTANCE_FANCYMENU = 20.0D;
	@Unique private static final double BLOCK_LOOK_DISTANCE_FANCYMENU = OnStartLookingAtBlockListener.MAX_LOOK_DISTANCE;
	@Unique private static final Logger LOGGER_FANCYMENU = LogManager.getLogger();

	@Unique private static boolean reloadListenerRegisteredFancyMenu = false;
	@Unique private boolean hasActiveServerConnection_FancyMenu;
	@Unique private boolean pendingServerJoinEvent_FancyMenu;
	@Unique @Nullable private String lastServerIp_FancyMenu;
	@Unique private boolean quitListenerFired_FancyMenu;

	@Shadow @Nullable public ClientLevel level;
	@Shadow @Nullable public LocalPlayer player;
	@Shadow @Nullable public HitResult hitResult;

	@Inject(method = "stop", at = @At("HEAD"))
	private void before_stop_FancyMenu(CallbackInfo info) {
		if (!this.quitListenerFired_FancyMenu) {
			this.quitListenerFired_FancyMenu = true;
			Listeners.ON_QUIT_MINECRAFT.onQuitMinecraft();
		}
	}

	@Inject(method = "doWorldLoad(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/util/Optional;Z)V", at = @At("HEAD"))
	private void before_doWorldLoad_FancyMenu(LevelStorageAccess levelStorage, PackRepository packRepository, WorldStem worldStem, Optional<GameRules> gameRules, boolean newWorld, CallbackInfo info) {
		try {
			if (levelStorage != null && worldStem != null) {
				Path savePath = levelStorage.getLevelPath(LevelResource.ROOT).toAbsolutePath();
				String iconPath = levelStorage.getIconFile().map(path -> path.toAbsolutePath().toString()).orElse(null);
				String worldName = worldStem.worldDataAndGenSettings().data().getLevelName();
				WorldSessionTracker.prepareSession(worldName, savePath.toString(), iconPath, newWorld);
			} else {
				WorldSessionTracker.clearSession();
			}
		} catch (Exception ex) {
			LOGGER_FANCYMENU.error("[FANCYMENU] Failed to prepare world session data!", ex);
			WorldSessionTracker.clearSession();
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

	@Inject(method = "pick(F)V", at = @At("TAIL"))
	private void tail_onPick_FancyMenu(float partialTicks, CallbackInfo info) {
		Minecraft self = (Minecraft)(Object)this;
		HitResult hitResult = this.hitResult;
		OnStartLookingAtBlockListener startBlockListener = Listeners.ON_START_LOOKING_AT_BLOCK;
		OnStopLookingAtBlockListener stopBlockListener = Listeners.ON_STOP_LOOKING_AT_BLOCK;
		OnStartLookingAtEntityListener startLookingListener = Listeners.ON_START_LOOKING_AT_ENTITY;
		OnStopLookingAtEntityListener stopLookingListener = Listeners.ON_STOP_LOOKING_AT_ENTITY;

		if (hitResult == null) {
			stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
			stopLooking_FancyMenu(startLookingListener, stopLookingListener);
			return;
		}

		Entity cameraEntity = self.getCameraEntity();
		if (cameraEntity == null) {
			stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
			stopLooking_FancyMenu(startLookingListener, stopLookingListener);
			return;
		}

		boolean checkEntity = startLookingListener.shouldCheckLookingAt();
		boolean checkBlock = startBlockListener.shouldCheckLookingAt();

		if (!checkEntity && !checkBlock) {
			stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
			stopLooking_FancyMenu(startLookingListener, stopLookingListener);
			return;
		}

		Vec3 eyePosition = cameraEntity.getEyePosition(partialTicks);

		if (checkEntity) {
			EntityHitResult extendedEntityHit = findExtendedEntityHit_FancyMenu(cameraEntity, partialTicks);

			if (extendedEntityHit == null && hitResult instanceof EntityHitResult vanillaEntityHit) {
				extendedEntityHit = vanillaEntityHit;
			}

			if (extendedEntityHit != null) {
				Entity targetEntity = extendedEntityHit.getEntity();
				double distance = extendedEntityHit.getLocation().distanceTo(eyePosition);
				startLookingListener.onLookAtEntity(targetEntity, distance);
				stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
				return;
			}
			stopLooking_FancyMenu(startLookingListener, stopLookingListener);
		} else {
			stopLooking_FancyMenu(startLookingListener, stopLookingListener);
		}

		if (checkBlock) {
			if (!(this.level instanceof ClientLevel clientLevel)) {
				stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
				return;
			}

			HitResult blockPickResult = cameraEntity.pick(BLOCK_LOOK_DISTANCE_FANCYMENU, partialTicks, false);
			if (!(blockPickResult instanceof BlockHitResult blockHitResult) || blockHitResult.getType() != HitResult.Type.BLOCK) {
				stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
				return;
			}

			BlockPos blockPos = blockHitResult.getBlockPos();
			BlockState blockState = clientLevel.getBlockState(blockPos);
			if (blockState.isAir()) {
				stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
				return;
			}

			double distanceSqr = blockHitResult.getLocation().distanceToSqr(eyePosition);
			if (distanceSqr > BLOCK_LOOK_DISTANCE_FANCYMENU * BLOCK_LOOK_DISTANCE_FANCYMENU) {
				stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
				return;
			}

			OnStartLookingAtBlockListener.LookedBlockData previousBlock = startBlockListener.getCurrentBlockData();
			if (previousBlock != null) {
				boolean sameBlock = previousBlock.blockPos().equals(blockPos)
						&& previousBlock.blockState().equals(blockState)
						&& previousBlock.levelKey().equals(clientLevel.dimension());
				if (!sameBlock) {
					stopBlockListener.onStopLooking(previousBlock);
				}
			}

			startBlockListener.onLookAtBlock(clientLevel, blockHitResult, Math.sqrt(distanceSqr));
		} else {
			stopLookingBlock_FancyMenu(startBlockListener, stopBlockListener);
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
	@Inject(at = @At(value = "HEAD"), method = "createTitle", cancellable = true)
	private void changeWindowTitleFancyMenu(CallbackInfoReturnable<String> info) {
		String title = WindowHandler.getCustomWindowTitle();
		if (title != null) {
			info.setReturnValue(title);
		}
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
		WorldSessionTracker.handleWorldLeft(Minecraft.class.cast(this));
	}
	@Inject(method = "clearClientLevel", at = @At("HEAD"))
	private void beforeClearClientLevelFancyMenu(Screen nextScreen, CallbackInfo info) {
		WorldSessionTracker.captureSnapshot(Minecraft.class.cast(this));
		this.fireServerLeft_FancyMenu();
		WorldSessionTracker.handleWorldLeft(Minecraft.class.cast(this));
	}

	@Inject(method = "clearClientLevel", at = @At("RETURN"))
	private void afterClearClientLevelFancyMenu(Screen nextScreen, CallbackInfo info) {
		SeamlessWorldLoadingHandler.clearCapture();
	}

	@Inject(method = "resizeGui", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(I)V", shift = At.Shift.AFTER))
	private void beforeResizeCurrentScreenFancyMenu(CallbackInfo info) {
		Screen screen = ScreenUtils.getScreen();
		if (screen != null) {
			RenderingUtils.resetGuiScale();
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenStartingEvent(screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Pre(screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
		}
	}

	@Inject(method = "resizeGui", at = @At("TAIL"))
	private void afterResizeCurrentScreenFancyMenu(CallbackInfo info) {
		Screen screen = ScreenUtils.getScreen();
		if (screen != null) {
			ScrollScreenNormalizer.normalizeScrollableScreen(screen);
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenEvent.Post(screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
			EventHandler.INSTANCE.postEvent(new InitOrResizeScreenCompletedEvent(screen, InitOrResizeScreenEvent.InitializationPhase.RESIZE));
		}
	}

	//This is a hacky way to get Minecraft to register FancyMenu's reload listener as early as possible in the Minecraft.class constructor
	@Inject(method = "resizeGui", at = @At("HEAD"))
	private void registerResourceReloadListenerInResizeGuiFancyMenu(CallbackInfo info) {
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
		if (!UNKNOWN_SERVER_IP_FANCYMENU.equals(serverIp)) {
			SeamlessWorldLoadingHandler.startServerCapture(serverIp);
		}
		SeamlessWorldLoadingHandler.finishServerLoad();
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

	@Unique
	private static void stopLooking_FancyMenu(OnStartLookingAtEntityListener startListener, OnStopLookingAtEntityListener stopListener) {
		OnStartLookingAtEntityListener.LookedEntityData previousEntity = startListener.getCurrentEntityData();
		if (previousEntity != null) {
			stopListener.onStopLooking(previousEntity);
			startListener.clearCurrentEntity();
		}
	}

	@Unique
	private static void stopLookingBlock_FancyMenu(OnStartLookingAtBlockListener startListener, OnStopLookingAtBlockListener stopListener) {
		OnStartLookingAtBlockListener.LookedBlockData previousBlock = startListener.getCurrentBlockData();
		if (previousBlock != null) {
			stopListener.onStopLooking(previousBlock);
		}
		startListener.clearCurrentBlock();
	}

	@Unique
	@Nullable
	private static EntityHitResult findExtendedEntityHit_FancyMenu(Entity cameraEntity, float partialTicks) {
		Vec3 eyePosition = cameraEntity.getEyePosition(partialTicks);
		Vec3 viewVector = cameraEntity.getViewVector(partialTicks);
		Vec3 reachVector = eyePosition.add(viewVector.scale(ENTITY_LOOK_DISTANCE_FANCYMENU));
		AABB searchBox = cameraEntity.getBoundingBox().expandTowards(viewVector.scale(ENTITY_LOOK_DISTANCE_FANCYMENU)).inflate(1.0D);
		double maxDistanceSqr = ENTITY_LOOK_DISTANCE_FANCYMENU * ENTITY_LOOK_DISTANCE_FANCYMENU;

		EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
			cameraEntity,
			eyePosition,
			reachVector,
			searchBox,
			entity -> !entity.isSpectator() && entity.isPickable(),
			maxDistanceSqr
		);

		if (entityHitResult == null) {
			return null;
		}

		Vec3 hitLocation = entityHitResult.getLocation();
		double entityDistanceSqr = hitLocation.distanceToSqr(eyePosition);
		if (entityDistanceSqr > maxDistanceSqr) {
			return null;
		}

		HitResult blockHitResult = cameraEntity.pick(ENTITY_LOOK_DISTANCE_FANCYMENU, partialTicks, false);
		if (blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS) {
			double blockDistanceSqr = blockHitResult.getLocation().distanceToSqr(eyePosition);
			if (blockDistanceSqr <= entityDistanceSqr) {
				return null;
			}
		}

		return entityHitResult;
	}

}

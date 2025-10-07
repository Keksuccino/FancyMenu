package de.keksuccino.fancymenu.customization.listener.listeners.helpers;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;

/**
 * Tracks metadata for the current integrated (singleplayer) world so listener events can expose it.
 */
public final class WorldSessionTracker {

    private static @Nullable SessionData activeSession;
    private static boolean pendingWorldEntry;

    private WorldSessionTracker() {}

    public static void prepareSession(@NotNull String worldName, @NotNull String worldSavePath, @Nullable String iconPath, boolean isFirstJoin) {
        String resolvedIconPath = resolveIconPath(worldSavePath, iconPath);
        activeSession = new SessionData(worldName, worldSavePath, resolvedIconPath, isFirstJoin);
        pendingWorldEntry = true;
    }

    public static void clearSession() {
        activeSession = null;
        pendingWorldEntry = false;
    }

    public static boolean hasPendingEntry() {
        return pendingWorldEntry && activeSession != null;
    }

    public static void captureSnapshot(@NotNull Minecraft minecraft) {
        if (activeSession == null) {
            return;
        }

        ClientLevel level = minecraft.level;
        if (level != null) {
            activeSession.lastDifficultyKey = level.getDifficulty().getKey();
        }

        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            activeSession.lastCheatsAllowed = Boolean.toString(server.getWorldData().isAllowCommands());
        }
    }

    public static void handleWorldEntered(@NotNull Minecraft minecraft) {
        if (!pendingWorldEntry || activeSession == null) {
            return;
        }

        captureSnapshot(minecraft);
        SessionData session = activeSession.copy();
        pendingWorldEntry = false;
        Listeners.ON_WORLD_ENTERED.onWorldEntered(
                session.worldName,
                session.worldSavePath,
                session.lastDifficultyKey,
                session.lastCheatsAllowed,
                session.iconPath,
                session.isFirstJoin
        );
    }

    public static void handleWorldLeft(@NotNull Minecraft minecraft) {
        if (activeSession == null) {
            return;
        }

        captureSnapshot(minecraft);
        SessionData session = activeSession.copy();
        clearSession();
        Listeners.ON_WORLD_LEFT.onWorldLeft(
                session.worldName,
                session.worldSavePath,
                session.lastDifficultyKey,
                session.lastCheatsAllowed,
                session.iconPath
        );
    }

    private static @NotNull String resolveIconPath(@NotNull String worldSavePath, @Nullable String iconPath) {
        if (iconPath != null && !iconPath.isBlank()) {
            return iconPath;
        }
        return Path.of(worldSavePath, LevelResource.ICON_FILE.getId()).toString();
    }

    private static final class SessionData {
        private final String worldName;
        private final String worldSavePath;
        private final String iconPath;
        private final boolean isFirstJoin;
        private @Nullable String lastDifficultyKey;
        private @Nullable String lastCheatsAllowed;

        private SessionData(String worldName, String worldSavePath, String iconPath, boolean isFirstJoin) {
            this.worldName = worldName;
            this.worldSavePath = worldSavePath;
            this.iconPath = iconPath;
            this.isFirstJoin = isFirstJoin;
        }

        private SessionData copy() {
            SessionData copy = new SessionData(this.worldName, this.worldSavePath, this.iconPath, this.isFirstJoin);
            copy.lastDifficultyKey = this.lastDifficultyKey;
            copy.lastCheatsAllowed = this.lastCheatsAllowed;
            return copy;
        }
    }

}
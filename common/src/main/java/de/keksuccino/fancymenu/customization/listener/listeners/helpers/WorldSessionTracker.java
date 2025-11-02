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
    private static @Nullable SessionData pendingSession;

    private WorldSessionTracker() {}

    public static void prepareSession(@NotNull String worldName, @NotNull String worldSavePath, @Nullable String iconPath, boolean isFirstJoin) {
        String resolvedIconPath = resolveIconPath(worldSavePath, iconPath);
        pendingSession = new SessionData(worldName, worldSavePath, resolvedIconPath, isFirstJoin);
    }

    public static void clearSession() {
        activeSession = null;
        pendingSession = null;
    }

    public static boolean hasPendingEntry() {
        return pendingSession != null;
    }

    public static void captureSnapshot(@NotNull Minecraft minecraft) {
        captureSnapshotFor(minecraft, activeSession);
    }

    public static void handleWorldEntered(@NotNull Minecraft minecraft) {
        if (pendingSession == null) {
            return;
        }

        captureSnapshotFor(minecraft, pendingSession);
        SessionData session = pendingSession.copy();
        activeSession = pendingSession;
        pendingSession = null;
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
        activeSession = null;
        Listeners.ON_WORLD_LEFT.onWorldLeft(
                session.worldName,
                session.worldSavePath,
                session.lastDifficultyKey,
                session.lastCheatsAllowed,
                session.iconPath
        );
    }

    private static void captureSnapshotFor(@NotNull Minecraft minecraft, @Nullable SessionData session) {
        if (session == null) {
            return;
        }

        ClientLevel level = minecraft.level;
        if (level != null) {
            session.lastDifficultyKey = level.getDifficulty().getKey();
        }

        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            session.lastCheatsAllowed = Boolean.toString(server.getWorldData().getAllowCommands());
        }
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

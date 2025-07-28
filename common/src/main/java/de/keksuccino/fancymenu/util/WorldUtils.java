package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class WorldUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean isSingleplayer() {
        if (Minecraft.getInstance().level == null) return false;
        return (Minecraft.getInstance().hasSingleplayerServer()) && (Minecraft.getInstance().getSingleplayerServer() != null) && !Minecraft.getInstance().getSingleplayerServer().isPublished();
    }

    public static boolean isMultiplayer() {
        if (Minecraft.getInstance().level == null) return false;
        return !isSingleplayer();
    }

    @NotNull
    public static List<LevelSummary> getLevels() {
        Minecraft minecraft = Minecraft.getInstance();
        LevelStorageSource.LevelCandidates levelCandidates;
        CompletableFuture<List<LevelSummary>> future;
        try {
            levelCandidates = minecraft.getLevelSource().findLevelCandidates();
        } catch (LevelStorageException ex) {
            LOGGER.error("[FANCYMENU] Couldn't load level list!", ex);
            return List.of();
        }
        if (levelCandidates.isEmpty()) {
            return List.of();
        } else {
            future = minecraft.getLevelSource().loadLevelSummaries(levelCandidates).exceptionally(throwable -> List.of());
        }
        try {
            return Objects.requireNonNullElse(future.getNow(List.of()), List.of());
        } catch (Exception ignore) {
        }
        return List.of();
    }

    @NotNull
    public static List<LevelData> getLevelsAsData() {
        List<LevelData> data = new ArrayList<>();
        getLevels().forEach(summary -> data.add(LevelData.fromLevelSummary(summary)));
        return data;
    }

}

package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.util.level.LocalLevelCache;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class WorldUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final LocalLevelCache LEVEL_CACHE = new LocalLevelCache(() -> Minecraft.getInstance().getLevelSource(), WorldUtils::readLevelsAsData, System::nanoTime, TimeUnit.SECONDS.toNanos(1));

    public static boolean isSingleplayer() {
        if (Minecraft.getInstance().level == null) return false;
        return (Minecraft.getInstance().hasSingleplayerServer()) && (Minecraft.getInstance().getSingleplayerServer() != null) && !Minecraft.getInstance().getSingleplayerServer().isPublished();
    }

    public static boolean isMultiplayer() {
        if (Minecraft.getInstance().level == null) return false;
        return !isSingleplayer();
    }

    /**
     * Reads the summaries of all local worlds, which reads the level data of every world.<br>
     * Use {@link #getLevelsAsData()} for anything that runs often.
     */
    @NotNull
    public static List<LevelSummary> getLevels() {
        LevelStorageSource levelSource = Minecraft.getInstance().getLevelSource();
        try {
            return readLevels(levelSource, levelSource.findLevelCandidates());
        } catch (LevelStorageException ex) {
            LOGGER.error("[FANCYMENU] Couldn't load level list!", ex);
            return List.of();
        }
    }

    /**
     * Returns the number of local world saves, which are the folders in the saves folder that have level data, without reading it.<br>
     * The saves folder gets checked at most once per second, so the count can be up to one second old.
     */
    public static int getLevelCount() {
        return LEVEL_CACHE.getLevelCount();
    }

    /**
     * Returns the {@link LevelData} of all local worlds that can get read, in the order of the vanilla world list.<br>
     * The saves folder gets checked at most once per second and worlds only get read again after their files changed, so this is cheap
     * enough for placeholders that get replaced every few frames. The list and its entries are shared, so never change them.
     */
    @NotNull
    public static List<LevelData> getLevelsAsData() {
        return LEVEL_CACHE.getLevels();
    }

    @NotNull
    private static List<LevelData> readLevelsAsData(@NotNull LevelStorageSource levelSource, @NotNull List<LevelStorageSource.LevelDirectory> directories) {
        List<LevelSummary> summaries = readLevels(levelSource, new LevelStorageSource.LevelCandidates(directories));
        List<LevelData> data = new ArrayList<>(summaries.size());
        summaries.forEach(summary -> data.add(LevelData.fromLevelSummary(summary)));
        return data;
    }

    @NotNull
    private static List<LevelSummary> readLevels(@NotNull LevelStorageSource levelSource, @NotNull LevelStorageSource.LevelCandidates candidates) {
        if (candidates.isEmpty()) return List.of();
        try {
            // loadLevelSummaries resolves asynchronously, so getNow(...) can return empty even though worlds exist.
            return Objects.requireNonNullElse(levelSource.loadLevelSummaries(candidates).join(), List.of());
        } catch (CompletionException | CancellationException ex) {
            LOGGER.error("[FANCYMENU] Couldn't load level summaries!", ex);
            return List.of();
        }
    }

}

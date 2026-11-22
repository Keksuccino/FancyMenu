package de.keksuccino.fancymenu.util.level;

import de.keksuccino.fancymenu.util.LevelData;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Keeps the {@link LevelData} of all local world saves, so placeholders that get replaced every few frames don't read the level data of
 * every world each time.
 *
 * <p>The saves folder gets checked at most once per check interval. A check only lists the world folders and reads the file attributes
 * of their {@code level.dat}, {@code level.dat_old} and {@code session.lock} files. A world gets read again only when one of these files
 * changed: saving a world replaces its level data files, and locking a world, like opening it in a game, writes its lock file.</p>
 */
public class LocalLevelCache {

    private static final Logger LOGGER = LogManager.getLogger();

    /** The order of the vanilla world list, see {@code LevelSummary#compareTo}: last played first, then by folder name. */
    private static final Comparator<LevelData> LEVEL_ORDER = Comparator.comparingLong((LevelData data) -> data.last_played).reversed().thenComparing(data -> data.file_name);

    @NotNull
    private final Supplier<LevelStorageSource> levelSource;
    @NotNull
    private final BiFunction<LevelStorageSource, List<LevelStorageSource.LevelDirectory>, List<LevelData>> levelReader;
    @NotNull
    private final LongSupplier nanoClock;
    private final long checkIntervalNanos;

    private boolean checked = false;
    private long lastCheckNanos;
    private boolean folderErrorLogged = false;
    @NotNull
    private List<LevelStorageSource.LevelDirectory> directories = List.of();
    @NotNull
    private Map<String, LevelFileState> fileStates = Map.of();
    @NotNull
    private Map<String, CachedLevel> cachedLevels = Map.of();
    @NotNull
    private List<LevelData> levels = List.of();
    private boolean levelsOutdated = true;

    /**
     * @param levelSource The source of the saves folder.
     * @param levelReader Reads the {@link LevelData} of the given world folders. Worlds it returns nothing for count as unreadable.
     * @param nanoClock The clock for the check interval, in nanoseconds.
     * @param checkIntervalNanos How long a check of the saves folder stays valid.
     */
    public LocalLevelCache(@NotNull Supplier<LevelStorageSource> levelSource, @NotNull BiFunction<LevelStorageSource, List<LevelStorageSource.LevelDirectory>, List<LevelData>> levelReader, @NotNull LongSupplier nanoClock, long checkIntervalNanos) {
        this.levelSource = levelSource;
        this.levelReader = levelReader;
        this.nanoClock = nanoClock;
        this.checkIntervalNanos = checkIntervalNanos;
    }

    /**
     * Returns the number of world folders in the saves folder, without reading the level data of any world.
     */
    public synchronized int getLevelCount() {
        this.checkSavesFolder();
        return this.directories.size();
    }

    /**
     * Returns the {@link LevelData} of all readable worlds in the order of the vanilla world list.<br>
     * The list and its entries are shared, so never change them.
     */
    @NotNull
    public synchronized List<LevelData> getLevels() {
        LevelStorageSource source = this.checkSavesFolder();
        if (this.levelsOutdated) {
            this.updateLevels(source);
        }
        return this.levels;
    }

    @NotNull
    private LevelStorageSource checkSavesFolder() {
        LevelStorageSource source = this.levelSource.get();
        long now = this.nanoClock.getAsLong();
        if (this.checked && ((now - this.lastCheckNanos) < this.checkIntervalNanos)) return source;
        this.checked = true;
        this.lastCheckNanos = now;
        List<LevelStorageSource.LevelDirectory> found;
        try {
            found = source.findLevelCandidates().levels();
            this.folderErrorLogged = false;
        } catch (LevelStorageException ex) {
            // Gets checked again every check interval, so only the first error in a row gets logged.
            if (!this.folderErrorLogged) {
                this.folderErrorLogged = true;
                LOGGER.error("[FANCYMENU] Couldn't check the local worlds!", ex);
            }
            found = List.of();
        }
        Map<String, LevelFileState> states = new HashMap<>();
        for (LevelStorageSource.LevelDirectory directory : found) {
            states.put(directory.directoryName(), LevelFileState.of(directory));
        }
        if (!states.equals(this.fileStates)) {
            this.levelsOutdated = true;
        }
        this.directories = found;
        this.fileStates = states;
        return source;
    }

    private void updateLevels(@NotNull LevelStorageSource source) {
        Map<String, CachedLevel> updated = new HashMap<>();
        List<LevelStorageSource.LevelDirectory> outdated = new ArrayList<>();
        for (LevelStorageSource.LevelDirectory directory : this.directories) {
            String name = directory.directoryName();
            CachedLevel cached = this.cachedLevels.get(name);
            if ((cached != null) && cached.fileState().equals(this.fileStates.get(name))) {
                updated.put(name, cached);
            } else {
                outdated.add(directory);
            }
        }
        if (!outdated.isEmpty()) {
            Map<String, LevelData> read = new HashMap<>();
            try {
                for (LevelData data : this.levelReader.apply(source, List.copyOf(outdated))) {
                    read.put(data.file_name, data);
                }
            } catch (RuntimeException ex) {
                LOGGER.error("[FANCYMENU] Couldn't read the local worlds!", ex);
            }
            for (LevelStorageSource.LevelDirectory directory : outdated) {
                String name = directory.directoryName();
                // Worlds that couldn't get read stay cached without data, so they only get read again once their files change.
                updated.put(name, new CachedLevel(this.fileStates.get(name), read.get(name)));
            }
        }
        List<LevelData> levels = new ArrayList<>(updated.size());
        for (CachedLevel cached : updated.values()) {
            if (cached.level() != null) levels.add(cached.level());
        }
        levels.sort(LEVEL_ORDER);
        this.cachedLevels = updated;
        this.levels = List.copyOf(levels);
        this.levelsOutdated = false;
    }

    private record CachedLevel(@NotNull LevelFileState fileState, @Nullable LevelData level) {
    }

    private record LevelFileState(@Nullable FileState levelData, @Nullable FileState oldLevelData, @Nullable FileState lock) {

        @NotNull
        private static LevelFileState of(@NotNull LevelStorageSource.LevelDirectory directory) {
            return new LevelFileState(FileState.of(directory.dataFile()), FileState.of(directory.oldDataFile()), FileState.of(directory.lockFile()));
        }

    }

    /**
     * The file key changes whenever a file gets replaced, which is how worlds get saved, so saves get noticed even on file systems
     * that store modification times only every few seconds.
     */
    private record FileState(@NotNull FileTime lastModified, long size, @Nullable Object fileKey) {

        @Nullable
        private static FileState of(@NotNull Path file) {
            try {
                BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                return new FileState(attributes.lastModifiedTime(), attributes.size(), attributes.fileKey());
            } catch (IOException ex) {
                return null;
            }
        }

    }

}

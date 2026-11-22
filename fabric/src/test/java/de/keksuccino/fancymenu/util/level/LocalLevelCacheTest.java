package de.keksuccino.fancymenu.util.level;

import de.keksuccino.fancymenu.util.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalLevelCacheTest {

    private static final long CHECK_INTERVAL = 1000L;

    @TempDir
    Path tempDir;

    private final AtomicLong clock = new AtomicLong();
    private final List<Set<String>> reads = new ArrayList<>();
    private final Set<String> unreadableWorlds = new HashSet<>();
    private final Map<String, Long> lastPlayed = new HashMap<>();
    private boolean readerFails = false;
    private int saveCounter = 0;
    private LocalLevelCache cache;

    @BeforeEach
    void setUp() {
        LevelStorageSource levelSource = this.createLevelSource();
        this.cache = new LocalLevelCache(() -> levelSource, this::readLevels, this.clock::get, CHECK_INTERVAL);
    }

    @Test
    void worldsGetReadOnlyOnceWhileTheyDontChange() throws IOException {
        this.createWorld("Alpha", "level.dat");
        this.createWorld("Beta", "level.dat");

        assertEquals(List.of("Alpha", "Beta"), this.levelNames());
        this.passCheckInterval();
        assertEquals(List.of("Alpha", "Beta"), this.levelNames());
        assertEquals(List.of(Set.of("Alpha", "Beta")), this.reads);
    }

    @Test
    void onlySavedWorldsGetReadAgain() throws IOException {
        this.createWorld("Alpha", "level.dat");
        this.createWorld("Beta", "level.dat");
        this.cache.getLevels();

        this.saveWorld("Beta");
        this.passCheckInterval();
        this.cache.getLevels();

        assertEquals(List.of(Set.of("Alpha", "Beta"), Set.of("Beta")), this.reads);
    }

    @Test
    void lockedWorldsGetReadAgain() throws IOException {
        this.createWorld("Alpha", "level.dat");
        this.createWorld("Beta", "level.dat");
        this.cache.getLevels();

        Files.writeString(this.savesDir().resolve("Alpha").resolve("session.lock"), "lock");
        this.passCheckInterval();
        this.cache.getLevels();

        assertEquals(List.of(Set.of("Alpha", "Beta"), Set.of("Alpha")), this.reads);
    }

    @Test
    void addedAndRemovedWorldsGetNoticed() throws IOException {
        Path alphaData = this.createWorld("Alpha", "level.dat");
        this.cache.getLevels();

        this.createWorld("Beta", "level.dat");
        Files.delete(alphaData);
        this.passCheckInterval();

        assertEquals(1, this.cache.getLevelCount());
        assertEquals(List.of("Beta"), this.levelNames());
        assertEquals(List.of(Set.of("Alpha"), Set.of("Beta")), this.reads);
    }

    @Test
    void savesFolderGetsCheckedAtMostOncePerInterval() throws IOException {
        this.createWorld("Alpha", "level.dat");
        assertEquals(1, this.cache.getLevelCount());

        this.createWorld("Beta", "level.dat");
        this.clock.addAndGet(CHECK_INTERVAL - 1);
        assertEquals(1, this.cache.getLevelCount());
        assertEquals(List.of("Alpha"), this.levelNames());

        this.clock.addAndGet(1);
        assertEquals(2, this.cache.getLevelCount());
        assertEquals(List.of("Alpha", "Beta"), this.levelNames());
    }

    @Test
    void countingWorldsReadsNoLevelData() throws IOException {
        this.createWorld("World", "level.dat");
        this.createWorld("World With Only Backup Data", "level.dat_old");
        Files.createDirectories(this.savesDir().resolve("Empty Folder"));
        Files.createDirectories(this.savesDir().resolve("Folder With Level Data Folder").resolve("level.dat"));
        Files.createFile(this.savesDir().resolve("level.dat"));
        Files.createFile(this.savesDir().resolve("Notes.txt"));

        assertEquals(2, this.cache.getLevelCount());
        assertTrue(this.reads.isEmpty());
    }

    @Test
    void unreadableWorldsGetReadAgainOnlyOnceTheyChange() throws IOException {
        this.createWorld("Alpha", "level.dat");
        this.createWorld("Broken", "level.dat");
        this.unreadableWorlds.add("Broken");

        assertEquals(List.of("Alpha"), this.levelNames());
        assertEquals(2, this.cache.getLevelCount());
        this.passCheckInterval();
        this.cache.getLevels();
        assertEquals(1, this.reads.size());

        this.unreadableWorlds.clear();
        this.saveWorld("Broken");
        this.passCheckInterval();
        assertEquals(List.of("Alpha", "Broken"), this.levelNames());
        assertEquals(List.of(Set.of("Alpha", "Broken"), Set.of("Broken")), this.reads);
    }

    @Test
    void failedReadsCountAsUnreadableWorlds() throws IOException {
        this.createWorld("Alpha", "level.dat");
        this.readerFails = true;

        assertEquals(List.of(), this.levelNames());
        this.passCheckInterval();
        assertEquals(List.of(), this.levelNames());
        assertEquals(1, this.reads.size());

        this.readerFails = false;
        this.saveWorld("Alpha");
        this.passCheckInterval();
        assertEquals(List.of("Alpha"), this.levelNames());
    }

    @Test
    void worldsGetSortedLikeTheVanillaWorldList() throws IOException {
        this.lastPlayed.put("Alpha", 10L);
        this.lastPlayed.put("Beta", 30L);
        this.lastPlayed.put("Delta", 20L);
        this.lastPlayed.put("Gamma", 30L);
        for (String name : this.lastPlayed.keySet()) {
            this.createWorld(name, "level.dat");
        }

        assertEquals(List.of("Beta", "Gamma", "Delta", "Alpha"), this.levelNames());
    }

    @Test
    void unreadableSavesFolderHasNoWorlds() throws IOException {
        this.createWorld("Alpha", "level.dat");
        assertEquals(List.of("Alpha"), this.levelNames());

        Files.move(this.savesDir(), this.tempDir.resolve("moved saves"));
        Files.createFile(this.savesDir());
        this.passCheckInterval();

        assertEquals(0, this.cache.getLevelCount());
        assertEquals(List.of(), this.levelNames());
    }

    @Test
    void returnedLevelsCantGetChanged() {
        assertThrows(UnsupportedOperationException.class, () -> this.cache.getLevels().add(new LevelData()));
    }

    private List<LevelData> readLevels(LevelStorageSource levelSource, List<LevelStorageSource.LevelDirectory> directories) {
        Set<String> names = new HashSet<>();
        List<LevelData> levels = new ArrayList<>();
        for (LevelStorageSource.LevelDirectory directory : directories) {
            String name = directory.directoryName();
            names.add(name);
            if (this.unreadableWorlds.contains(name)) continue;
            LevelData data = new LevelData();
            data.file_name = name;
            data.last_played = this.lastPlayed.getOrDefault(name, 0L);
            levels.add(data);
        }
        this.reads.add(names);
        if (this.readerFails) throw new IllegalStateException("Test read failure");
        return levels;
    }

    private List<String> levelNames() {
        return this.cache.getLevels().stream().map(data -> data.file_name).toList();
    }

    private void passCheckInterval() {
        this.clock.addAndGet(CHECK_INTERVAL);
    }

    private Path createWorld(String folderName, String levelDataFileName) throws IOException {
        Path worldDir = Files.createDirectories(this.savesDir().resolve(folderName));
        return Files.writeString(worldDir.resolve(levelDataFileName), "created");
    }

    /**
     * Replaces the level data file like the game does when it saves a world.
     */
    private void saveWorld(String folderName) throws IOException {
        Path worldDir = this.savesDir().resolve(folderName);
        Path tempFile = Files.writeString(worldDir.resolve("level.dat.tmp"), "saved " + (++this.saveCounter));
        Files.move(tempFile, worldDir.resolve("level.dat"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Finding worlds never uses the data fixer of the {@link LevelStorageSource}, so the tests leave it out instead of building the
     * whole data fixer of the game.
     */
    private LevelStorageSource createLevelSource() {
        return new LevelStorageSource(this.savesDir(), this.tempDir.resolve("backups"), null);
    }

    private Path savesDir() {
        return this.tempDir.resolve("saves");
    }

}

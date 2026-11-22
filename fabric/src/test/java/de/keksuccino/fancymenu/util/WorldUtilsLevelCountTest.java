package de.keksuccino.fancymenu.util;

import net.minecraft.world.level.storage.LevelStorageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldUtilsLevelCountTest {

    @TempDir
    Path tempDir;

    @Test
    void emptySavesFolderHasNoWorlds() {
        assertEquals(0, WorldUtils.getLevelCount(this.createLevelSource()));
    }

    @Test
    void onlyFoldersWithLevelDataCountAsWorlds() throws IOException {
        LevelStorageSource levelSource = this.createLevelSource();
        this.createWorld("World", "level.dat");
        this.createWorld("World With Only Backup Data", "level.dat_old");
        Files.createDirectories(this.savesDir().resolve("Empty Folder"));
        Files.createDirectories(this.savesDir().resolve("Folder With Level Data Folder").resolve("level.dat"));
        Files.createFile(this.savesDir().resolve("level.dat"));
        Files.createFile(this.savesDir().resolve("Notes.txt"));

        assertEquals(2, WorldUtils.getLevelCount(levelSource));
    }

    @Test
    void countFollowsAddedAndRemovedWorlds() throws IOException {
        LevelStorageSource levelSource = this.createLevelSource();
        Path firstWorldData = this.createWorld("First World", "level.dat");
        assertEquals(1, WorldUtils.getLevelCount(levelSource));

        this.createWorld("Second World", "level.dat");
        assertEquals(2, WorldUtils.getLevelCount(levelSource));

        Files.delete(firstWorldData);
        assertEquals(1, WorldUtils.getLevelCount(levelSource));
    }

    @Test
    void unreadableSavesFolderHasNoWorlds() throws IOException {
        LevelStorageSource levelSource = this.createLevelSource();
        Files.delete(this.savesDir());
        Files.createFile(this.savesDir());

        assertEquals(0, WorldUtils.getLevelCount(levelSource));
        assertEquals(0, WorldUtils.getLevelCount(levelSource));
    }

    /**
     * Counting worlds never uses the data fixer of the {@link LevelStorageSource}, so the tests leave it out instead of building the
     * whole data fixer of the game.
     */
    private LevelStorageSource createLevelSource() {
        return new LevelStorageSource(this.savesDir(), this.tempDir.resolve("backups"), null);
    }

    private Path createWorld(String folderName, String levelDataFileName) throws IOException {
        Path worldDir = Files.createDirectories(this.savesDir().resolve(folderName));
        return Files.createFile(worldDir.resolve(levelDataFileName));
    }

    private Path savesDir() {
        return this.tempDir.resolve("saves");
    }

}

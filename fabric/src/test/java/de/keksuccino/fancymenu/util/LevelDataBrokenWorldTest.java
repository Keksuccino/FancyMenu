package de.keksuccino.fancymenu.util;

import net.minecraft.world.level.storage.LevelSummary;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LevelDataBrokenWorldTest {

    @Test
    void worldsWithForbiddenSymlinksKeepTheirFolderName() {
        LevelData data = LevelData.fromLevelSummary(new LevelSummary.SymlinkLevelSummary("Linked World", Path.of("saves", "Linked World", "icon.png")));

        assertEquals("Linked World", data.file_name);
        assertEquals("Linked World", data.display_name);
        assertEquals("Linked World", data.settings_level_name);
        assertEquals(-1L, data.last_played);
        assertNull(data.game_type);
        assertNull(data.difficulty);
        assertNull(data.minecraft_version_name);
        assertEquals("Linked World", LevelData.deserialize(data.serialize()).file_name);
    }

}

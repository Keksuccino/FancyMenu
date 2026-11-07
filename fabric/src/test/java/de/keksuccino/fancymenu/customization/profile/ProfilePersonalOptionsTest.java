package de.keksuccino.fancymenu.customization.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfilePersonalOptionsTest {

    @TempDir
    Path tempDir;

    @Test
    void onlyPersonalCategoriesGetCopied() throws Exception {
        Path source = Files.writeString(this.tempDir.resolve("source.txt"), String.join("\n",
                "##[window]", "", "S:custom_window_title = 'My Pack';", "",
                "##[ui]", "", "S:ui_scale_v2 = 'large';", "S:ui_theme = 'light';", "",
                "##[global_customizations]", "", "S:global_menu_music_tracks = 'song.ogg';", "",
                "##[tutorial]", "", "B:show_welcome_screen = 'false';", ""));
        Path target = this.tempDir.resolve("target.txt");

        ProfilePersonalOptions.copyPersonalCategories(source, target);

        String copied = Files.readString(target);
        assertTrue(copied.contains("ui_scale_v2 = 'large'"));
        assertTrue(copied.contains("ui_theme = 'light'"));
        assertTrue(copied.contains("show_welcome_screen = 'false'"));
        assertFalse(copied.contains("custom_window_title"));
        assertFalse(copied.contains("global_menu_music_tracks"));
    }

    @Test
    void missingSourceWritesNothing() throws Exception {
        Path target = this.tempDir.resolve("target.txt");

        ProfilePersonalOptions.copyPersonalCategories(this.tempDir.resolve("missing.txt"), target);

        assertFalse(Files.exists(target));
    }

}

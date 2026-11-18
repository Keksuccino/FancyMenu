package de.keksuccino.fancymenu.customization.aiexport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiExportFilesTest {

    private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 18, 7, 12, 3);

    @Test
    void fileNamesOnlyKeepSafeCharactersAndEndWithTheTime() {
        assertEquals("layout_title_screen_2026-11-18_07-12-03", AiExportFiles.buildFileNameBase("layout_Title Screen", TIME));
        assertEquals("listener_my_listener_2026-11-18_07-12-03", AiExportFiles.buildFileNameBase("listener_My  ../Listener!", TIME));
    }

    @Test
    void pathSeparatorsNeverEndUpInFileNames() {
        String name = AiExportFiles.buildFileNameBase("../../etc/passwd\\x", TIME);
        assertTrue(name.indexOf('/') < 0 && name.indexOf('\\') < 0 && !name.contains(".."), name);
    }

    @Test
    void basesWithoutSafeCharactersGetAFallbackName() {
        assertEquals(AiExportFiles.FALLBACK_FILE_NAME_BASE + "_2026-11-18_07-12-03", AiExportFiles.buildFileNameBase("!!!", TIME));
    }

    @Test
    void longBasesGetShortened() {
        String name = AiExportFiles.buildFileNameBase("a".repeat(500), TIME);
        assertTrue(name.length() < 100, name);
    }

    @Test
    void savingWritesTheTextAsUtf8(@TempDir Path directory) throws Exception {
        AiExportDocument document = new AiExportDocument("Title", "variables", "Grüße 🦊\n");
        File file = AiExportFiles.save(document, directory.toFile(), TIME);
        assertTrue(file.getName().endsWith(AiExportFiles.FILE_EXTENSION));
        assertEquals(document.text(), Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void savingNeverOverwritesEarlierExports(@TempDir Path directory) throws Exception {
        File first = AiExportFiles.save(new AiExportDocument("Title", "variables", "first"), directory.toFile(), TIME);
        File second = AiExportFiles.save(new AiExportDocument("Title", "variables", "second"), directory.toFile(), TIME);
        assertNotEquals(first, second);
        assertEquals("first", Files.readString(first.toPath(), StandardCharsets.UTF_8));
        assertEquals("second", Files.readString(second.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    void savingCreatesMissingDirectories(@TempDir Path directory) throws Exception {
        File target = directory.resolve("not").resolve("there").toFile();
        File file = AiExportFiles.save(new AiExportDocument("Title", "settings", "text"), target, TIME);
        assertTrue(file.isFile());
    }

}

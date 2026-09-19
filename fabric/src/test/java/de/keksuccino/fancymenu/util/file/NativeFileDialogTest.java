package de.keksuccino.fancymenu.util.file;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeFileDialogTest {

    @Test
    void convertsLegacyGlobFiltersToSdlExtensionLists() {
        assertEquals("png;jpg", NativeFileDialog.toSdlPattern(List.of("*.png", "*.jpg")));
        assertEquals("tar.gz", NativeFileDialog.toSdlPattern(List.of("*.tar.gz")));
        assertEquals("json", NativeFileDialog.toSdlPattern(List.of("json")));
    }

    @Test
    void keepsUnsupportedAndEmptyFiltersBrowsable() {
        assertEquals("*", NativeFileDialog.toSdlPattern(List.of()));
        for (String pattern : List.of("*", "*.*", "", "image?.png", "sub/file.png")) {
            assertEquals("*", NativeFileDialog.toSdlPattern(List.of("*.png", pattern)));
        }
    }

}

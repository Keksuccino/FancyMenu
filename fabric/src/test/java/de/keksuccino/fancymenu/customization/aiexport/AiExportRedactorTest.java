package de.keksuccino.fancymenu.customization.aiexport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiExportRedactorTest {

    @Test
    void unixHomeFolderBecomesTilde() {
        String redacted = AiExportRedactor.redact("file: /Users/tim/.minecraft/config/fancymenu/options.txt", "/Users/tim", "tim");
        assertEquals("file: " + AiExportRedactor.HOME_REPLACEMENT + "/.minecraft/config/fancymenu/options.txt", redacted);
    }

    @Test
    void windowsHomeFolderGetsReplacedWithBothSeparatorsAndAnyCase() {
        String text = "C:\\Users\\Tim\\AppData\\Roaming\\.minecraft and c:/users/tim/Desktop/image.png";
        String redacted = AiExportRedactor.redact(text, "C:\\Users\\Tim", "Tim");
        assertFalse(redacted.toLowerCase().contains("users\\tim"), redacted);
        assertFalse(redacted.toLowerCase().contains("users/tim"), redacted);
        assertTrue(redacted.startsWith(AiExportRedactor.HOME_REPLACEMENT + "\\AppData"), redacted);
        assertTrue(redacted.endsWith(AiExportRedactor.HOME_REPLACEMENT + "/Desktop/image.png"), redacted);
    }

    @Test
    void homeFolderWithTrailingSeparatorStillMatches() {
        assertEquals(AiExportRedactor.HOME_REPLACEMENT + "/game", AiExportRedactor.redact("/home/steve/game", "/home/steve/", "steve"));
    }

    @Test
    void foldersThatOnlyStartLikeTheHomeFolderStayUntouched() {
        String text = "/Users/timothy/game";
        assertEquals(text, AiExportRedactor.redact(text, "/Users/tim", "tim"));
    }

    @Test
    void accountNameInPathsOutsideOfTheHomeFolderGetsHidden() {
        String redacted = AiExportRedactor.redact("D:\\Users\\Tim\\Games and /home/tim/server", "/Users/someone_else", "tim");
        assertEquals("D:\\Users\\" + AiExportRedactor.USER_NAME_REPLACEMENT + "\\Games and /home/" + AiExportRedactor.USER_NAME_REPLACEMENT + "/server", redacted);
    }

    @Test
    void accountNameOutsideOfPathsStaysUntouched() {
        String text = "Tim made this layout at /Users/Public/layout.txt";
        assertEquals(text, AiExportRedactor.redact(text, "/Users/someone_else", "tim"));
    }

    @Test
    void homeFoldersThatWouldMatchEveryPathGetIgnored() {
        String text = "/opt/minecraft and C:\\Games";
        assertEquals(text, AiExportRedactor.redact(text, "/", null));
        assertEquals(text, AiExportRedactor.redact(text, "C:\\", null));
    }

    @Test
    void missingSystemPropertiesChangeNothing() {
        String text = "/Users/tim/game";
        assertEquals(text, AiExportRedactor.redact(text, null, null));
        assertEquals(text, AiExportRedactor.redact(text, "", " "));
    }

}

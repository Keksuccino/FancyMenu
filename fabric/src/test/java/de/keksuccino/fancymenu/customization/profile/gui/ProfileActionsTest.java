package de.keksuccino.fancymenu.customization.profile.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileActionsTest {

    @Test
    void fileNamesStayReadableButSafe() {
        assertEquals("My Cool Setup", ProfileActions.toFileName("My Cool Setup"));
        assertEquals("ab c", ProfileActions.toFileName("a/b\\  c?"));
        assertEquals("Version 1.2", ProfileActions.toFileName("Version 1.2..."));
        assertEquals("profile", ProfileActions.toFileName("???"));
        assertEquals(64, ProfileActions.toFileName("x".repeat(200)).length());
    }

    @Test
    void versionsGetComparedNumberByNumber() {
        assertTrue(ProfileActions.compareVersions("3.10.0", "3.9.14") > 0);
        assertTrue(ProfileActions.compareVersions("3.10.0", "3.10.0") == 0);
        assertTrue(ProfileActions.compareVersions("3.9", "3.9.1") < 0);
        assertTrue(ProfileActions.compareVersions("4.0.0-beta.2", "3.10.0") > 0);
        assertEquals(0, ProfileActions.compareVersions("abc", "0"));
    }

}

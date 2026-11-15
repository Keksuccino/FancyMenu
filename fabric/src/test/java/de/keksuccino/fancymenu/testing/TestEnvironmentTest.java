package de.keksuccino.fancymenu.testing;

import org.junit.jupiter.api.Test;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the rule that tests never show up on the screen of the user (see TESTING_GUIDELINES.md).<br>
 * The root build script gives every test JVM these properties. Without them, macOS gives the test JVM a Dock icon
 * as soon as a test loads AWT.
 */
class TestEnvironmentTest {

    @Test
    void testJvmRunsHeadless() {
        assertTrue(GraphicsEnvironment.isHeadless());
    }

    @Test
    void testJvmStaysOutOfTheDock() {
        assertEquals("true", System.getProperty("apple.awt.UIElement"));
    }

}

package de.keksuccino.fancymenu.util.window;

import de.keksuccino.fancymenu.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

/**
 * Dev-only launch mode for automated in-game tests, documented in TESTING_GUIDELINES.md in the project root.<br>
 * When the {@value #ENV_VARIABLE} environment variable is "true" in a development environment, the game window gets created
 * hidden and without focus, so test clients never show up on screen or take focus away from the user.<br><br>
 *
 * The mode also keeps the window out of fullscreen, because GLFW always shows fullscreen windows.
 * Its hooks run before Minecraft creates the window, so this class must not touch any game state.
 */
public final class OffscreenTestClient {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String ENV_VARIABLE = "FANCYMENU_TEST_OFFSCREEN";
    // Checks the environment variable first, so normal launches never load the platform services this early
    private static final boolean ENABLED = "true".equalsIgnoreCase(System.getenv(ENV_VARIABLE)) && Services.PLATFORM.isDevelopmentEnvironment();

    static {
        if (ENABLED) LOGGER.info("[FANCYMENU] Off-screen test client mode is active! The game window stays hidden and unfocused.");
    }

    private OffscreenTestClient() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Has to get called right before GLFW gets initialized.<br>
     * Disabling the macOS menu bar keeps GLFW from turning the game into a regular app, so it gets no Dock icon and can't get activated.
     */
    public static void prepareBackendInit() {
        if (!ENABLED) return;
        GLFW.glfwInitHint(GLFW.GLFW_COCOA_MENUBAR, GLFW.GLFW_FALSE);
    }

    /**
     * Has to get called after Minecraft reset the GLFW window hints and before it creates the game window.
     */
    public static void prepareWindowCreation() {
        if (!ENABLED) return;
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
    }

}

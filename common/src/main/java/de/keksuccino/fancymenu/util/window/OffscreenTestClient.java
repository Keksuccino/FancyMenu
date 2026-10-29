package de.keksuccino.fancymenu.util.window;

import de.keksuccino.fancymenu.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLVideo;

/**
 * Dev-only launch mode for automated in-game tests, documented in TESTING_GUIDELINES.md in the project root.<br>
 * When the {@value #ENV_VARIABLE} environment variable is "true" in a development environment, the game window gets created
 * hidden and without focus, so test clients never show up on screen or take focus away from the user.<br><br>
 *
 * The mode also keeps the window out of fullscreen, because SDL shows fullscreen windows.
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
     * Has to get called right before SDL gets initialized.<br>
     * Running as macOS background app keeps SDL from activating the game, so it also gets no Dock icon.
     */
    public static void prepareBackendInit() {
        if (!ENABLED) return;
        SDLHints.SDL_SetHint(SDLHints.SDL_HINT_MAC_BACKGROUND_APP, "1");
    }

    /**
     * Returns the SDL window flags Minecraft should create the game window with.
     */
    public static long modifyWindowFlags(long flags) {
        return ENABLED ? (flags | SDLVideo.SDL_WINDOW_HIDDEN) : flags;
    }

}

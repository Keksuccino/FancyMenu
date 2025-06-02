package de.keksuccino.fancymenu.util.rendering.ui.screen.scrollnormalizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ScrollScreenNormalizerHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File NORMALIZE_SCREEN_FILE = new File(GameDirectoryUtils.getGameDirectory(), "normalized_scroll_screens.json");
    private static final Map<String, Boolean> NORMALIZE_SCREEN_MAP = new HashMap<>();

    public static void setForScreen(@NotNull Screen screen, boolean normalize) {
        NORMALIZE_SCREEN_MAP.put(ScreenIdentifierHandler.getIdentifierOfScreen(screen), normalize);
    }

    public static boolean shouldNormalize(@NotNull Screen screen) {
        String id = ScreenIdentifierHandler.getIdentifierOfScreen(screen);
        if (!NORMALIZE_SCREEN_MAP.containsKey(id)) return false;
        return NORMALIZE_SCREEN_MAP.get(id);
    }

}

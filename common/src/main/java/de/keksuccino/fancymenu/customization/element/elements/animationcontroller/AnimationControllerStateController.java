package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.util.file.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.*;

public class AnimationControllerStateController {

    public static final File STATES_FILE = new File(FancyMenu.MOD_DIR, "/animation_controller_states.json");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, AnimationControllerState> STATES = new HashMap<>();

    private static boolean initialized = false;

    protected static void read() {

        STATES.clear();

        try {

            STATES_FILE.createNewFile();

            List<String> jsonList = FileUtils.readTextLinesFrom(STATES_FILE);
            StringBuilder json = new StringBuilder();
            jsonList.forEach(json::append);

            if (json.toString().isBlank()) return;

            List<AnimationControllerState> stateList = GSON.fromJson(json.toString(), new TypeToken<>() {});
            stateList.forEach(state -> STATES.put(Objects.requireNonNull(state.element_identifier), state));

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read Animation Controller states from file!", ex);
        }

    }

    protected static void write() {

        if (!initialized) read();
        initialized = true;

        try {

            STATES_FILE.createNewFile();

            List<AnimationControllerState> stateList = new ArrayList<>(STATES.values());
            String json = GSON.toJson(stateList);

            FileUtils.writeTextToFile(STATES_FILE, false, Objects.requireNonNull(json));

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to write Animation Controller states to file!", ex);
        }

    }

    public static void syncChanges() {
        if (!initialized) read();
        initialized = true;
        write();
    }

    public static boolean hasStateFor(@NotNull String elementIdentifier) {
        if (!initialized) read();
        initialized = true;
        return STATES.containsKey(elementIdentifier);
    }

    @Nullable
    public static AnimationControllerState getState(@NotNull String elementIdentifier) {
        if (!initialized) read();
        initialized = true;
        return STATES.get(elementIdentifier);
    }

    public static void putState(@NotNull String elementIdentifier, @NotNull AnimationControllerState state) {
        if (!initialized) read();
        initialized = true;
        STATES.put(elementIdentifier, state);
        syncChanges();
    }

    public static boolean isPlaying(@NotNull String elementIdentifier) {
        AnimationControllerState state = getState(elementIdentifier);
        if (state == null) {
            state = new AnimationControllerState(elementIdentifier, true);
            putState(elementIdentifier, state);
        }
        return state.playing;
    }

    public static boolean setPlaying(@NotNull String elementIdentifier, boolean playing) {
        AnimationControllerState state = getState(elementIdentifier);
        if (state == null) {
            putState(elementIdentifier, new AnimationControllerState(elementIdentifier, playing));
            return true;
        }
        if (state.playing != playing) {
            state.playing = playing;
            putState(elementIdentifier, state);
            return true;
        }
        return false;
    }

    public static boolean togglePlaying(@NotNull String elementIdentifier) {
        boolean newState = !isPlaying(elementIdentifier);
        setPlaying(elementIdentifier, newState);
        return newState;
    }

    public static class AnimationControllerState {

        public String element_identifier;
        public boolean playing;

        public AnimationControllerState(@NotNull String element_identifier, boolean playing) {
            this.element_identifier = element_identifier;
            this.playing = playing;
        }

    }

}

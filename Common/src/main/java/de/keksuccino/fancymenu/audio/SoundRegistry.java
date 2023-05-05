package de.keksuccino.fancymenu.audio;

import de.keksuccino.konkrete.sound.SoundHandler;

import java.util.ArrayList;
import java.util.List;

public class SoundRegistry {

    private static final List<String> REGISTERED_SOUNDS = new ArrayList<>();

    public static void registerSound(String key, String path) {
        if (!REGISTERED_SOUNDS.contains(key)) {
            REGISTERED_SOUNDS.add(key);
        }
        SoundHandler.registerSound(key, path);
    }

    public static void unregisterSound(String key) {
        REGISTERED_SOUNDS.remove(key);
        SoundHandler.unregisterSound(key);
    }

    public static void stopSounds() {
        for (String s : REGISTERED_SOUNDS) {
            SoundHandler.stopSound(s);
        }
    }

    public static void resetSounds() {
        for (String s : REGISTERED_SOUNDS) {
            SoundHandler.resetSound(s);
        }
    }

    public static boolean isSoundRegistered(String key) {
        return REGISTERED_SOUNDS.contains(key);
    }

    public static List<String> getSounds() {
        return REGISTERED_SOUNDS;
    }

}

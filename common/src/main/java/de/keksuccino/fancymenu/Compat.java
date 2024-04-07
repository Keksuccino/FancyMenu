package de.keksuccino.fancymenu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Compat {

    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean isOptiFineLoaded() {
        try {
            Class.forName("optifine.Installer", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isAudioExtensionLoaded() {
        try {
            Class.forName("de.keksuccino.fmaudio.FmAudio", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isVideoExtensionLoaded() {
        try {
            Class.forName("de.keksuccino.fmvideo.FmVideo", false, FancyMenu.class.getClassLoader());
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static void printInfoLog() {
        if (isOptiFineLoaded()) LOGGER.info("[FANCYMENU] OptiFine found! Will try to fix incompatibilities!");
    }

}

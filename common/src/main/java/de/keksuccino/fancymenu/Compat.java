package de.keksuccino.fancymenu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Compat {

    private static final Logger LOGGER = LogManager.getLogger();

    //TODO übernehmen
//    public static boolean isRRLSOverlay(Overlay overlay) {
//        try {
//            Class<?> c = Class.forName("com.github.dimadencep.mods.rrls.accessor.SplashAccessor");
//            if (c.isInstance(overlay)) {
//                return (boolean) c.getMethod("isAttached").invoke(overlay);
//            }
//        } catch (Exception ignore) {}
//        return false;
//    }
//
//    public static boolean isRRLSLoaded() {
//        try {
//            Class.forName("com.github.dimadencep.mods.rrls.Rrls", false, FancyMenu.class.getClassLoader());
//            return true;
//        } catch (Exception ignore) {}
//        return false;
//    }

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
        //TODO übernehmen
//        if (isRRLSLoaded()) LOGGER.warn("[FANCYMENU] \"Remove Reloading Screen\" found! FancyMenu does not fully support RRLS, but will try to fix problems as good as possible.");
    }

}

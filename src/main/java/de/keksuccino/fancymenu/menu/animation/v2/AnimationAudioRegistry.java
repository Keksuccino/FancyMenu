//package de.keksuccino.fancymenu.menu.animation.v2;
//
//import de.keksuccino.auudio.audio.AudioClip;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import javax.annotation.Nullable;
//import java.util.*;
//
//public class AnimationAudioRegistry {
//
//    private static final Logger LOGGER = LogManager.getLogger();
//
//    protected static Map<String, AudioClip> audios = new HashMap<>();
//
//    public static void registerAudio(String identifier, AudioClip audio) {
//        if ((audio != null) && (identifier != null)) {
//            audios.put(identifier, audio);
//        }
//    }
//
//    public static boolean hasAudio(String identifier) {
//        return audios.containsKey(identifier);
//    }
//
//    @Nullable
//    public static AudioClip getAudio(String identifier) {
//        return audios.get(identifier);
//    }
//
//    public static List<AudioClip> getAll() {
//        List<AudioClip> l = new ArrayList<>();
//        l.addAll(audios.values());
//        return l;
//    }
//
//    public static void stopAll() {
//        for (AudioClip c : audios.values()) {
//            c.stop();
//        }
//    }
//
//    public static void setVolumeForAll(int percentVolume) {
//    }
//
//}

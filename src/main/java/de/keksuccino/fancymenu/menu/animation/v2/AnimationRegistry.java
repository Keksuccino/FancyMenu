//package de.keksuccino.fancymenu.menu.animation.v2;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import javax.annotation.Nullable;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class AnimationRegistry {
//
//    private static Logger LOGGER = LogManager.getLogger("fancymenu/AnimationRegistry");
//
//    protected static Map<String, Animation> animations = new HashMap<>();
//
//    public static void registerAnimation(Animation animation) {
//        if (animation != null) {
//            if (!animations.containsKey(animation.name)) {
//                animations.put(animation.name, animation);
//                LOGGER.info("[FancyMenu] Registering animation: " + animation.name);
//            } else {
//                LOGGER.error("[FancyMenu] Unable to register animation '" + animation.name + "'! Animation name already in use!");
//            }
//        }
//    }
//
//    public static boolean unregisterAnimation(String name) {
//        Animation a = animations.remove(name);
//        if (a != null) {
//            return true;
//        }
//        return false;
//    }
//
//    public static List<Animation> getAnimations() {
//        List<Animation> l = new ArrayList<>();
//        l.addAll(animations.values());
//        return l;
//    }
//
//    @Nullable
//    public static Animation getAnimation(String name) {
//        return animations.get(name);
//    }
//
//    public static boolean hasAnimation(String name) {
//        return animations.containsKey(name);
//    }
//
//    public static void clearAll() {
//        animations.clear();
//    }
//
//}

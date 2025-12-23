package de.keksuccino.fancymenu.util;

import net.minecraft.util.Util;

public class OSUtils {

    public static boolean isMacOS() {
        return Util.getPlatform() == Util.OS.OSX;
    }

    public static boolean isWindows() {
        return Util.getPlatform() == Util.OS.WINDOWS;
    }

    public static boolean isLinux() {
        return Util.getPlatform() == Util.OS.LINUX;
    }

}

package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ScreenUtils {

    private static int setScreenBlockDepth = 0;

    public static void blockSetScreenCalls(boolean blocked) {
        if (blocked) {
            setScreenBlockDepth++;
        } else if (setScreenBlockDepth > 0) {
            setScreenBlockDepth--;
        }
    }

    public static boolean areSetScreenCallsBlocked() {
        return setScreenBlockDepth > 0;
    }

    @Nullable
    public static Screen getScreen() {
        return Minecraft.getInstance().screen;
    }

    public static int getScreenWidth() {
        Screen s = getScreen();
        return (s != null) ? s.width : 0;
    }

    public static int getScreenHeight() {
        Screen s = getScreen();
        return (s != null) ? s.height : 0;
    }

    public static int getScreenCenterX() {
        return Math.max(1, getScreenWidth()) / 2;
    }

    public static int getScreenCenterY() {
        return Math.max(1, getScreenHeight()) / 2;
    }

}

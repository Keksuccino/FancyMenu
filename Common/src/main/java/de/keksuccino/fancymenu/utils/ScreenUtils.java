package de.keksuccino.fancymenu.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ScreenUtils {

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

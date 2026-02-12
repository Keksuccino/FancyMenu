package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;

public class MouseUtil {

    private static final Minecraft MC = Minecraft.getInstance();

    public static boolean isMouseGrabbed() {
        return MC.mouseHandler.isMouseGrabbed();
    }

    public static boolean isLeftMouseDown() {
        return MC.mouseHandler.isLeftPressed();
    }

    public static boolean isRightMouseDown() {
        return MC.mouseHandler.isRightPressed();
    }

    public static double getGuiScaledMouseX() {
        return getMouseX() * (double)MC.getWindow().getGuiScaledWidth() / (double)MC.getWindow().getScreenWidth();
    }

    public static double getGuiScaledMouseY() {
        return getMouseY() * (double)MC.getWindow().getGuiScaledHeight() / (double)MC.getWindow().getScreenHeight();
    }

    public static double getMouseX() {
        return MC.mouseHandler.xpos();
    }

    public static double getMouseY() {
        return MC.mouseHandler.ypos();
    }

}

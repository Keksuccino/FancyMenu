package de.keksuccino.fancymenu.util.input;

import net.minecraft.client.Minecraft;

public class MouseUtils {

    public static double getScaledMouseX() {
        return Minecraft.getInstance().mouseHandler.getScaledXPos(Minecraft.getInstance().getWindow());
    }

    public static double getScaledMouseY() {
        return Minecraft.getInstance().mouseHandler.getScaledYPos(Minecraft.getInstance().getWindow());
    }

}

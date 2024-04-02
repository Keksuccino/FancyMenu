package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;

//TODO übernehmen
public class WorldUtils {

    public static boolean isSingleplayer() {
        if (Minecraft.getInstance().level == null) return false;
        return (Minecraft.getInstance().hasSingleplayerServer()) && (Minecraft.getInstance().getSingleplayerServer() != null) && !Minecraft.getInstance().getSingleplayerServer().isPublished();
    }

    public static boolean isMultiplayer() {
        if (Minecraft.getInstance().level == null) return false;
        return !isSingleplayer();
    }

}

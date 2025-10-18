package de.keksuccino.fancymenu.util.rendering.entity;

import de.keksuccino.fancymenu.platform.Services;

public class FancyEntityRendererUtils {

    public static boolean isFerLoaded() {
        return Services.PLATFORM.isModLoaded("fancy_entity_renderer");
//        try {
//            Class.forName("it.crystalnest.fancy_entity_renderer.api.entity.player.FancyPlayerWidget", false, FancyMenu.class.getClassLoader());
//            return true;
//        } catch (Exception ignored) {}
//        return false;
    }

}

//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.visibilityrequirements;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Calendar;
import java.util.Locale;

public class VisibilityRequirementHandler {

    public static boolean isSingleplayer = false;
    public static int windowWidth = 0;
    public static int windowHeight = 0;
    public static boolean worldLoaded = false;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new VisibilityRequirementHandler());
    }

    public static void update() {

        //VR: World Loaded
        worldLoaded = (Minecraft.getInstance().level != null);

        //VR: Is Singleplayer & Is Multiplayer
        isSingleplayer = Minecraft.getInstance().hasSingleplayerServer();
        if (!worldLoaded) {
            isSingleplayer = false;
        }

        //VR: Is Window Width/Height X
        windowWidth = Minecraft.getInstance().getWindow().getScreenWidth();
        windowHeight = Minecraft.getInstance().getWindow().getScreenHeight();

    }

    public static boolean isMacOS() {
        return Minecraft.ON_OSX;
    }

    public static boolean isWindows() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        return (s.contains("win"));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onButtonsCached(ButtonCachedEvent e) {
        update();
    }

}

// ----- 
package de.keksuccino.fancymenu.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

//This is needed because otherwise Drippy can crash on startup when used in combination with mods like Oculus, Iris or OptiFine
public class ScreenTitleHandler {

    public static Component getTitleOfScreen(Screen screen) {
        return screen.getTitle();
    }

    public static void setScreenTitle(Screen screen, Component title) {
        screen.title = title;
    }

}

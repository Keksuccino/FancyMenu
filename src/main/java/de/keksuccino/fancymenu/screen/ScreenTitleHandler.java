//TODO übernehmen
package de.keksuccino.fancymenu.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

//This is needed because otherwise Drippy can crash on startup when used in combination with mods like Oculus, Iris or OptiFine
public class ScreenTitleHandler {

    public static ITextComponent getTitleOfScreen(Screen screen) {
        return screen.getTitle();
    }

    public static void setScreenTitle(Screen screen, ITextComponent title) {
        screen.title = title;
    }

}

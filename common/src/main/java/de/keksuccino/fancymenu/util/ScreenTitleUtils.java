package de.keksuccino.fancymenu.util;

import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//This is needed because otherwise Drippy can crash on startup when used in combination with mods like Oculus, Iris or OptiFine
public class ScreenTitleUtils {

    @SuppressWarnings("all")
    public static Component getTitleOfScreen(@NotNull Screen screen) {
        Component c = screen.getTitle();
        if (c == null) return Components.empty();
        return c;
    }

    @Nullable
    public static String getTitleLocalizationKeyOfScreen(@NotNull Screen screen) {
        Component title = ScreenTitleUtils.getTitleOfScreen(screen);
        if (title instanceof TranslatableComponent t) {
            return t.getKey();
        }
        return null;
    }

    public static void setScreenTitle(@NotNull Screen screen, @NotNull Component title) {
        screen.title = title;
    }

}

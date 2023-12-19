package de.keksuccino.fancymenu.util;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//This is needed because otherwise Drippy can crash on startup when used in combination with mods like Oculus, Iris or OptiFine
public class ScreenTitleUtils {

    @SuppressWarnings("all")
    public static Component getTitleOfScreen(@NotNull Screen screen) {
        Component c = screen.getTitle();
        if (c == null) return Component.empty();
        return c;
    }

    @Nullable
    public static String getTitleLocalizationKeyOfScreen(@NotNull Screen screen) {
        Component title = ScreenTitleUtils.getTitleOfScreen(screen);
        if (title instanceof MutableComponent) {
            ComponentContents cc = title.getContents();
            if (cc instanceof TranslatableContents t) {
                return t.getKey();
            }
        }
        return null;
    }

    public static void setScreenTitle(@NotNull Screen screen, @NotNull Component title) {
        screen.title = title;
    }

}

package de.keksuccino.fancymenu.util.rendering.ui.dialog;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class Dialogs {

    public static void open(@NotNull Component message, @NotNull DialogStyle style) {
        // this opens a PiPWindow with a DialogBody wrapped
    }

    public static void openWithCallback(@NotNull Component message, @NotNull DialogStyle style, @NotNull Consumer<Boolean> callback) {
        // this opens a PiPWindow with a DialogBody wrapped
    }

}

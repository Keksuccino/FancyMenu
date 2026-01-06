package de.keksuccino.fancymenu.util.rendering.ui.dialog;

import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class Dialogs {

    public static void open(@NotNull Component message, @NotNull DialogStyle style) {
        openInternal(message, style, null);
    }

    public static void openWithCallback(@NotNull Component message, @NotNull DialogStyle style, @NotNull Consumer<Boolean> callback) {
        openInternal(message, style, callback);
    }

    private static void openInternal(@NotNull Component message, @NotNull DialogStyle style, @Nullable Consumer<Boolean> callback) {

        DialogBody body = new DialogBody(message, style, callback);
        PiPWindow window = new PiPWindow(style.getTitle())
                .setIcon(style.getIcon())
                .setMinSize(408, 180)
                .setSize(408, 180)
                .setScreen(body)
                .setBlockMinecraftScreenInputs(true)
                .setForceFancyMenuUiScale(true)
                .setForceFocusEnabled(true);

        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);

    }

}

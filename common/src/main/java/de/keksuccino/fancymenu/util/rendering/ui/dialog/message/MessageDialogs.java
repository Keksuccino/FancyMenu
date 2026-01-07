package de.keksuccino.fancymenu.util.rendering.ui.dialog.message;

import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class MessageDialogs {

    public static MessageDialogBody open(@NotNull Component message, @NotNull MessageDialogStyle style) {
        return openInternal(message, style, null);
    }

    public static MessageDialogBody openWithCallback(@NotNull Component message, @NotNull MessageDialogStyle style, @NotNull Consumer<Boolean> callback) {
        return openInternal(message, style, callback);
    }

    private static MessageDialogBody openInternal(@NotNull Component message, @NotNull MessageDialogStyle style, @Nullable Consumer<Boolean> callback) {

        MessageDialogBody body = new MessageDialogBody(message, style, callback);
        PiPWindow window = new PiPWindow(style.getTitle())
                .setIcon(style.getIcon())
                .setMinSize(408, 180)
                .setSize(408, 180)
                .setScreen(body)
                .setBlockMinecraftScreenInputs(true)
                .setForceFancyMenuUiScale(true)
                .setForceFocusEnabled(true);

        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        return body;

    }

}

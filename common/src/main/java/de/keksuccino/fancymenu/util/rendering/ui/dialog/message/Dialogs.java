package de.keksuccino.fancymenu.util.rendering.ui.dialog.message;

import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PipableScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class Dialogs {

    public static <D extends Screen & PipableScreen> Pair<D, PiPWindow> openGeneric(@NotNull D dialog, @Nullable Component title, @Nullable ResourceLocation icon, int width, int height) {
        PiPWindow window = PiPWindowHandler.INSTANCE.openWindowCentered(new PiPWindow(dialog), null)
                .setTitle((title != null) ? title : Component.empty())
                .setIcon(icon)
                .setSize(width, height)
                .setMinSize(width, height)
                .setAlwaysOnTop(true)
                .setForceFocus(true)
                .setBlockMinecraftScreenInputs(true);
        return Pair.of(dialog, window);
    }

    public static MessageDialogBody openMessage(@NotNull Component message, @NotNull MessageDialogStyle style) {
        return openMessageInternal(message, style, null);
    }

    public static MessageDialogBody openMessageWithCallback(@NotNull Component message, @NotNull MessageDialogStyle style, @NotNull Consumer<Boolean> callback) {
        return openMessageInternal(message, style, callback);
    }

    private static MessageDialogBody openMessageInternal(@NotNull Component message, @NotNull MessageDialogStyle style, @Nullable Consumer<Boolean> callback) {

        MessageDialogBody body = new MessageDialogBody(message, style, callback);
        PiPWindow window = new PiPWindow(style.getTitle())
                .setIcon(style.getIcon())
                .setMinSize(408, 180)
                .setSize(408, 180)
                .setScreen(body)
                .setBlockMinecraftScreenInputs(true)
                .setForceFancyMenuUiScale(true)
                .setForceFocus(true);

        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        return body;

    }

}

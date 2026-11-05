package de.keksuccino.fancymenu.util.rendering.ui.dialog;

import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogBody;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PipableScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class Dialogs {

    private static final int DEFAULT_MESSAGE_WIDTH = 408;
    private static final int DEFAULT_MESSAGE_HEIGHT = 180;

    public static <D extends Screen & PipableScreen> Pair<D, PiPWindow> openGeneric(@NotNull D dialog, @Nullable Component title, @Nullable Identifier icon, int width, int height) {
        PiPWindow window = new PiPWindow((title != null) ? title : Component.empty())
                .setScreen(dialog)
                .setIcon(icon)
                .setSize(width, height)
                .setMinSize(width, height)
                .setAlwaysOnTop(true)
                .setForceFocus(true)
                .setBlockMinecraftScreenInputs(true)
                .setForceFancyMenuUiScale(true);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        return Pair.of(dialog, window);
    }

    public static MessageDialogBody openMessage(@NotNull Component message, @NotNull MessageDialogStyle style) {
        return openMessageInternal(message, style, null, DEFAULT_MESSAGE_WIDTH, DEFAULT_MESSAGE_HEIGHT);
    }

    public static MessageDialogBody openWarningMessage(@NotNull Component message) {
        return openMessage(message, MessageDialogStyle.WARNING).setForceOkOnly(true);
    }

    public static MessageDialogBody openExpensiveFmaWarning(@NotNull String fmaName) {
        return openMessageInternal(Component.translatable("fancymenu.resources.fma.expensive.warning", Objects.requireNonNull(fmaName)), MessageDialogStyle.WARNING, null, 514, 254).setForceOkOnly(true);
    }

    public static MessageDialogBody openMessageWithCallback(@NotNull Component message, @NotNull MessageDialogStyle style, @NotNull Consumer<Boolean> callback) {
        return openMessageWithCallback(message, style, DEFAULT_MESSAGE_WIDTH, DEFAULT_MESSAGE_HEIGHT, callback);
    }

    public static MessageDialogBody openMessageWithCallback(@NotNull Component message, @NotNull MessageDialogStyle style, int width, int height, @NotNull Consumer<Boolean> callback) {
        MessageDialogBody body = openMessageInternal(message, style, Objects.requireNonNull(callback), width, height);
        if ((style == MessageDialogStyle.INFO) || (style == MessageDialogStyle.ERROR)) body.setForceOkOnly(true);
        return body;
    }

    private static MessageDialogBody openMessageInternal(@NotNull Component message, @NotNull MessageDialogStyle style, @Nullable Consumer<Boolean> callback, int width, int height) {

        MessageDialogBody body = new MessageDialogBody(message, style, callback);
        PiPWindow window = new PiPWindow(style.getTitle())
                .setIcon(style.getIcon())
                .setMinSize(width, height)
                .setSize(width, height)
                .setScreen(body)
                .setBlockMinecraftScreenInputs(true)
                .setForceFancyMenuUiScale(true)
                .setForceFocus(true);

        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
        return body;

    }

}

package de.keksuccino.fancymenu.util.rendering.ui.dialog;

import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class Dialogs {

    private static final int ICON_SIZE = 32;
    private static final int ICON_GAP = 12;
    private static final int PADDING = 16;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_AREA_HEIGHT = 40;
    private static final int LINE_SPACING = 14;
    private static final int MIN_TEXT_WIDTH = 160;
    private static final int MAX_TEXT_WIDTH = 360;
    private static final int MIN_HEIGHT = 120;
    private static final int WINDOW_MARGIN = 40;

    public static void open(@NotNull Component message, @NotNull DialogStyle style) {
        openInternal(message, style, null);
    }

    public static void openWithCallback(@NotNull Component message, @NotNull DialogStyle style, @NotNull Consumer<Boolean> callback) {
        openInternal(message, style, callback);
    }

    private static void openInternal(@NotNull Component message, @NotNull DialogStyle style, @Nullable Consumer<Boolean> callback) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int iconSpace = style.getIcon() != null ? (ICON_SIZE + ICON_GAP) : 0;
        int buttonMinWidth = callback != null ? (BUTTON_WIDTH * 2 + BUTTON_GAP) : BUTTON_WIDTH;
        int minWidth = (PADDING * 2) + buttonMinWidth;

        List<MutableComponent> linesCopy = TextFormattingUtils.lineWrapComponents(message, 100000);
        int maxLineWidth = 0;
        for (Component line : linesCopy) {
            maxLineWidth = Math.max(maxLineWidth, font.width(line));
        }
        int textWidth = Math.min(MAX_TEXT_WIDTH, Math.max(MIN_TEXT_WIDTH, maxLineWidth));
        int targetWidth = (PADDING * 2) + iconSpace + textWidth;
        targetWidth = Math.max(targetWidth, minWidth);
        int maxWidth = Math.max(minWidth, screenWidth - WINDOW_MARGIN);
        targetWidth = Math.min(targetWidth, maxWidth);

        int wrapWidth = Math.max(20, targetWidth - (PADDING * 2) - iconSpace);
        List<MutableComponent> wrappedLines = TextFormattingUtils.lineWrapComponents(linesCopy, wrapWidth);
        int textHeight = wrappedLines.size() * LINE_SPACING;
        int contentHeight = Math.max(textHeight, style.getIcon() != null ? ICON_SIZE : 0);
        int targetHeight = (PADDING * 2) + contentHeight + BUTTON_AREA_HEIGHT;
        targetHeight = Math.max(targetHeight, MIN_HEIGHT);
        int maxHeight = Math.max(MIN_HEIGHT, screenHeight - WINDOW_MARGIN);
        targetHeight = Math.min(targetHeight, maxHeight);

        double guiScale = minecraft.getWindow().getGuiScale();
        int rawWidth = guiScale > 1.0 ? (int) Math.ceil(targetWidth * guiScale) : targetWidth;
        int rawHeight = guiScale > 1.0 ? (int) Math.ceil(targetHeight * guiScale) : targetHeight;
        int rawMinWidth = guiScale > 1.0 ? (int) Math.ceil(minWidth * guiScale) : minWidth;
        int rawMinHeight = guiScale > 1.0 ? (int) Math.ceil(MIN_HEIGHT * guiScale) : MIN_HEIGHT;

        DialogBody body = new DialogBody(message, style, callback);
        PiPWindow window = new PiPWindow(style.getTitle(), rawWidth, rawHeight)
                .setIcon(style.getIcon())
                .setMinSize(rawMinWidth, rawMinHeight)
                .setScreen(body)
                .setBlockMinecraftScreenInputs(true)
                .setScreenAutoScalingEnabled(false)
                .setForceFocusEnabled(true);

        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
    }

}

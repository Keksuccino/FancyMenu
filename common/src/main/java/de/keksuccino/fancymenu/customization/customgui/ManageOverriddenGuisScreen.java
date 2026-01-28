package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManageOverriddenGuisScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 640;
    public static final int PIP_WINDOW_HEIGHT = 420;

    protected Runnable onCloseRunnable;
    protected List<String> removedOverrides = new ArrayList<>();

    public ManageOverriddenGuisScreen(@NotNull Runnable onClose) {
        super(Component.translatable("fancymenu.custom_guis.manage_overridden"));
        this.onCloseRunnable = onClose;
    }

    @Override
    protected void initCells() {

        this.addSpacerCell(20);

        boolean first = true;
        for (Map.Entry<String, String> m : CustomGuiHandler.getOverriddenScreens().entrySet()) {

            String overriddenScreen = m.getKey();
            String overriddenWith = m.getValue();

            if (!this.removedOverrides.contains(overriddenScreen)) {

                if (!first) {
                    this.addDescriptionEndSeparatorCell();
                }
                first = false;

                this.addLabelCell(Component.translatable("fancymenu.custom_guis.manage_overridden.screen", Component.literal(overriddenScreen).setStyle(Style.EMPTY.withBold(false))).setStyle(Style.EMPTY.withBold(true)));
                this.addLabelCell(Component.translatable("fancymenu.custom_guis.manage_overridden.overridden_with", Component.literal(overriddenWith).setStyle(Style.EMPTY.withBold(false))).setStyle(Style.EMPTY.withBold(true)));
                this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.custom_guis.manage_overridden.remove_override").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt())), var1 -> {
                    Dialogs.openMessageWithCallback(Component.translatable("fancymenu.custom_guis.manage_overridden.remove_override.confirm"), MessageDialogStyle.WARNING, remove -> {
                        if (remove) this.removedOverrides.add(overriddenScreen);
                    });
                }), true);

            }

        }

        this.addSpacerCell(20);

    }

    @Override
    protected void onCancel() {
        this.onCloseRunnable.run();
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        for (String s : this.removedOverrides) {
            CustomGuiHandler.removeScreenOverrideFor(s);
        }
        this.onCloseRunnable.run();
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        this.onCloseRunnable.run();
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageOverriddenGuisScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(false)
                .setForceFocus(false)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull ManageOverriddenGuisScreen screen) {
        return openInWindow(screen, null);
    }

}

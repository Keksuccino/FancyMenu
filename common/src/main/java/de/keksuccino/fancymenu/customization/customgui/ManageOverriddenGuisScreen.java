package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManageOverriddenGuisScreen extends CellScreen {

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
                this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.custom_guis.manage_overridden.remove_override").withStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())), var1 -> {
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
    }

    @Override
    protected void onDone() {
        for (String s : this.removedOverrides) {
            CustomGuiHandler.removeScreenOverrideFor(s);
        }
        this.onCloseRunnable.run();
    }

}

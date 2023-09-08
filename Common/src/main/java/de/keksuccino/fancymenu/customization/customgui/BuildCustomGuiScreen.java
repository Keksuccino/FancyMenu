package de.keksuccino.fancymenu.customization.customgui;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.text.component.ComponentWidget;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfiguratorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

public class BuildCustomGuiScreen extends ConfiguratorScreen {

    @NotNull
    protected CustomGui gui;
    protected Consumer<CustomGui> callback;
    protected boolean allSettingsValid = false;
    @NotNull
    protected ComponentWidget settingsFeedbackWidget = ComponentWidget.empty(0,0);

    protected BuildCustomGuiScreen(@Nullable CustomGui guiToEdit, @NotNull Consumer<CustomGui> callback) {
        super(Component.translatable("fancymenu.custom_guis.build"));
        this.gui = (guiToEdit != null) ? guiToEdit : new CustomGui();
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addLabelCell(Component.translatable("fancymenu.custom_guis.build.identifier"));

        this.addTextInputCell(CharacterFilter.buildBasicFilenameCharacterFilter(), false, false)
                .setEditListener(s -> this.gui.identifier = s)
                .setText(this.gui.identifier);

        this.addLabelCell(Component.translatable("fancymenu.custom_guis.build.title"));

        this.addTextInputCell(null, true, true)
                .setEditListener(s -> this.gui.title = s)
                .setText(this.gui.title);

        this.addSpacerCell(10);

        this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.custom_guis.build.allow_esc", this.gui.allowEsc), (value, button) -> {
            this.gui.allowEsc = value.getAsBoolean();
        }), true);

        this.addSpacerCell(10);

        this.addWidgetCell(this.settingsFeedbackWidget, false);

    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.gui.identifier.isEmpty()) {
            this.settingsFeedbackWidget.setText(Component.translatable("fancymenu.custom_guis.build.identifier.invalid").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())));
        } else if (CustomGuiHandler.guiExists(this.gui.identifier)) {
            this.settingsFeedbackWidget.setText(Component.translatable("fancymenu.custom_guis.build.identifier.already_in_use").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())));
        } else {
            this.settingsFeedbackWidget.setText(Component.empty());
        }

        super.render(pose, mouseX, mouseY, partial);

    }

    @Override
    protected void onCancel() {
        this.callback.accept(null);
    }

    @Override
    protected void onDone() {
        if (this.allSettingsValid) {
            this.callback.accept(this.gui);
        }
    }

}

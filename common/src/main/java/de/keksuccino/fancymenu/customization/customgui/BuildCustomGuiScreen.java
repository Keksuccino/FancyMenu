package de.keksuccino.fancymenu.customization.customgui;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfiguratorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public class BuildCustomGuiScreen extends ConfiguratorScreen {

    @NotNull
    protected CustomGui gui;
    @NotNull
    protected CustomGui guiTemp;
    protected Consumer<CustomGui> callback;
    protected boolean allSettingsValid = false;
    @Nullable
    protected String identifierOfEdit;
    protected LabelCell settingsFeedbackCell;

    protected BuildCustomGuiScreen(@Nullable CustomGui guiToEdit, @NotNull Consumer<CustomGui> callback) {
        super(Component.translatable("fancymenu.custom_guis.build"));
        this.gui = (guiToEdit != null) ? guiToEdit : new CustomGui();
        if (guiToEdit != null) this.identifierOfEdit = guiToEdit.identifier;
        this.guiTemp = (guiToEdit != null) ? this.gui.copy() : this.gui;
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addSpacerCell(20);

        this.addLabelCell(Component.translatable("fancymenu.custom_guis.build.identifier"));
        this.addTextInputCell(CharacterFilter.buildOnlyLowercaseFileNameFilter(), false, false)
                .setEditListener(s -> this.guiTemp.identifier = s)
                .setText(this.guiTemp.identifier);

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.translatable("fancymenu.custom_guis.build.title"));
        this.addTextInputCell(null, true, true)
                .setEditListener(s -> this.guiTemp.title = s)
                .setText(this.guiTemp.title);

        this.addCellGroupEndSpacerCell();

        this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.custom_guis.build.allow_esc", this.guiTemp.allowEsc), (value, button) -> {
            this.guiTemp.allowEsc = value.getAsBoolean();
        }), true);

        this.addSpacerCell(10);

        this.settingsFeedbackCell = this.addLabelCell(Component.empty());

        this.addSpacerCell(20);

    }

    @Override
    public boolean allowDone() {
        return this.allSettingsValid;
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.guiTemp.identifier.isEmpty()) {
            this.settingsFeedbackCell.setText(Component.translatable("fancymenu.custom_guis.build.identifier.invalid").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())));
            this.allSettingsValid = false;
        } else if (CustomGuiHandler.guiExists(this.guiTemp.identifier) && !Objects.equals(this.guiTemp.identifier, this.identifierOfEdit)) {
            this.settingsFeedbackCell.setText(Component.translatable("fancymenu.custom_guis.build.identifier.already_in_use").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())));
            this.allSettingsValid = false;
        } else {
            this.settingsFeedbackCell.setText(Component.empty());
            this.allSettingsValid = true;
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
            if (this.gui != this.guiTemp) {
                this.gui.identifier = this.guiTemp.identifier;
                this.gui.title = this.guiTemp.identifier;
                this.gui.allowEsc = this.guiTemp.allowEsc;
            }
            this.callback.accept(this.gui);
        }
    }

}

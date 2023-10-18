package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.v3.ConfiguratorScreen;
import de.keksuccino.fancymenu.v3.input.CharacterFilter;
import de.keksuccino.fancymenu.v3.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class BuildCustomGuiScreen extends ConfiguratorScreen {

    @NotNull
    protected CustomGui gui;
    @NotNull
    protected CustomGui guiTemp;
    protected Consumer<CustomGui> callback;
    protected boolean allSettingsValid = false;
    protected LabelCell settingsFeedbackCell;

    public BuildCustomGuiScreen(@NotNull Consumer<CustomGui> callback) {
        super(Component.literal(Locals.localize("fancymenu.custom_guis.build")));
        this.gui = new CustomGui();
        this.guiTemp = this.gui;
        this.callback = callback;
    }

    @Override
    protected void initCells() {

        this.addSpacerCell(20);

        this.addLabelCell(Component.literal(Locals.localize("fancymenu.custom_guis.build.identifier")));
        this.addTextInputCell(CharacterFilter.buildBasicFilenameCharacterFilter(), false, false)
                .setEditListener(s -> this.guiTemp.identifier = s)
                .setText(this.guiTemp.identifier);

        this.addCellGroupEndSpacerCell();

        this.addLabelCell(Component.literal(Locals.localize("fancymenu.custom_guis.build.title")));
        this.addTextInputCell(null, true, true)
                .setEditListener(s -> this.guiTemp.title = s)
                .setText(this.guiTemp.title);

        this.addCellGroupEndSpacerCell();

        this.addWidgetCell(new ExtendedButton(0, 0, 20, 20, Component.empty(), p_93751_ -> {
            this.guiTemp.allowEsc = !this.guiTemp.allowEsc;
        }).setLabelSupplier(consumes -> {
            if (this.guiTemp.allowEsc) return Component.literal(Locals.localize("fancymenu.custom_guis.build.allow_esc.enabled"));
            return Component.literal(Locals.localize("fancymenu.custom_guis.build.allow_esc.disabled"));
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.guiTemp.identifier.isEmpty()) {
            this.settingsFeedbackCell.setText(Component.literal(Locals.localize("fancymenu.custom_guis.build.identifier.invalid")).setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            this.allSettingsValid = false;
        } else if (CustomGuiLoader.getCustomGuis().contains(this.guiTemp.identifier)) {
            this.settingsFeedbackCell.setText(Component.literal(Locals.localize("fancymenu.custom_guis.build.identifier.already_in_use")).setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            this.allSettingsValid = false;
        } else {
            this.settingsFeedbackCell.setText(Component.empty());
            this.allSettingsValid = true;
        }

        super.render(graphics, mouseX, mouseY, partial);

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

    public static class CustomGui {

        @NotNull
        public String identifier = "";
        @NotNull
        public String title = "";
        public boolean allowEsc = true;

    }

}

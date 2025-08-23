package de.keksuccino.fancymenu.customization.variables;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.gui.ModernScreen;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ManageVariablesScreen extends ModernScreen {

    protected Consumer<List<Variable>> callback;

    protected ScrollArea variableListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedEditBox searchBar;

    public ManageVariablesScreen(@NotNull Consumer<List<Variable>> callback) {
        super(Components.translatable("fancymenu.overlay.menu_bar.variables.manage"));
        this.callback = callback;
    }

    @Override
    protected void init() {

        String oldSearchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        this.searchBar = new ExtendedEditBox(Minecraft.getInstance().font, 20 + 1, 50 + 15 + 1, (this.width / 2) - 40 - 2, 20 - 2, Components.empty()) {
            @Override
            public void renderButton(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {
                super.renderButton(graphics, mouseX, mouseY, partial);
                if (this.getValue().isBlank() && !this.isFocused()) {
                    GuiGraphics.currentGraphics().drawString(this.font, Components.translatable("fancymenu.variables.manage_variables.screen.search_variable"), this.getX() + 4, this.getY() + (this.getHeight() / 2) - (this.font.lineHeight / 2), UIBase.getUIColorTheme().edit_box_text_color_uneditable.getColorInt(), false);
                }
            }
        };
        this.searchBar.setValue(oldSearchValue);
        this.searchBar.setResponder(s -> this.updateVariablesList());
        this.addRenderableWidget(this.searchBar);
        UIBase.applyDefaultWidgetSkinTo(this.searchBar);

        // Set positions for scroll area
        this.variableListScrollArea.setWidth((this.width / 2) - 40, true);
        this.variableListScrollArea.setHeight(this.height - 85 - 25, true);
        this.variableListScrollArea.setX(20, true);
        this.variableListScrollArea.setY(50 + 15 + 25, true);

        // Calculate button width
        int buttonWidth = (this.width - 20) - ((this.variableListScrollArea.getXWithBorder() + (this.variableListScrollArea.getWidthWithBorder() + 20)));
        if (buttonWidth < 150) buttonWidth = 150;
        if (buttonWidth > 220) buttonWidth = 220;

        // Calculate button positions
        int doneButtonX = this.width - 20 - buttonWidth;
        int doneButtonY = this.height - 20 - 20;
        int toggleResetButtonX = doneButtonX;
        int toggleResetButtonY = doneButtonY - 15 - 20;
        int deleteButtonX = doneButtonX;
        int deleteButtonY = toggleResetButtonY - 5 - 20;
        int setValueButtonX = doneButtonX;
        int setValueButtonY = deleteButtonY - 5 - 20;
        int addButtonX = doneButtonX;
        int addButtonY = setValueButtonY - 15 - 20;

        // Create buttons with positions in constructors
        ExtendedButton addVariableButton = new ExtendedButton(addButtonX, addButtonY, buttonWidth, 20, Components.translatable("fancymenu.overlay.menu_bar.variables.manage.add_variable"), (button) -> {
            TextInputScreen s = new TextInputScreen(Components.translatable("fancymenu.overlay.menu_bar.variables.manage.add_variable.input_name"), CharacterFilter.buildOnlyLowercaseFileNameFilter(), (call) -> {
                if (call != null) {
                    if (!VariableHandler.variableExists(call)) {
                        VariableHandler.setVariable(call, "");
                        updateVariablesList();
                    }
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addRenderableWidget(addVariableButton);
        UIBase.applyDefaultWidgetSkinTo(addVariableButton);

        ExtendedButton setValueButton = new ExtendedButton(setValueButtonX, setValueButtonY, buttonWidth, 20, Components.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"), (button) -> {
            VariableScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                TextInputScreen s = new TextInputScreen(Components.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"), null, (call) -> {
                    if (call != null) {
                        e.variable.setValue(call);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                s.setText(e.variable.getValue());
                Minecraft.getInstance().setScreen(s);
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addRenderableWidget(setValueButton);
        UIBase.applyDefaultWidgetSkinTo(setValueButton);

        ExtendedButton deleteVariableButton = new ExtendedButton(deleteButtonX, deleteButtonY, buttonWidth, 20, Components.translatable("fancymenu.overlay.menu_bar.variables.manage.delete_variable"), (button) -> {
            VariableScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings(call -> {
                    if (call) {
                        VariableHandler.removeVariable(e.variable.getName());
                        this.updateVariablesList();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.overlay.menu_bar.variables.manage.delete_variable.confirm")));
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addRenderableWidget(deleteVariableButton);
        UIBase.applyDefaultWidgetSkinTo(deleteVariableButton);

        LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> resetOnLaunchDisabled = CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.variables.manage.clear_on_launch", false);
        ExtendedButton toggleResetOnLaunchButton = new ExtendedButton(toggleResetButtonX, toggleResetButtonY, buttonWidth, 20, Components.empty(), (button) -> {
            VariableScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                e.variable.setResetOnLaunch(!e.variable.isResetOnLaunch());
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null))
                .setLabelSupplier(consumes -> {
                    VariableScrollEntry e = this.getSelectedEntry();
                    if (e != null) {
                        LocalizedEnumValueCycle<CommonCycles.CycleEnabledDisabled> enabledDisabled = CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.variables.manage.clear_on_launch");
                        enabledDisabled.setCurrentValue(CommonCycles.CycleEnabledDisabled.getByBoolean(e.variable.isResetOnLaunch()));
                        return enabledDisabled.getCycleComponent();
                    }
                    return resetOnLaunchDisabled.getCycleComponent();
                });
        this.addRenderableWidget(toggleResetOnLaunchButton);
        UIBase.applyDefaultWidgetSkinTo(toggleResetOnLaunchButton);

        ExtendedButton doneButton = new ExtendedButton(doneButtonX, doneButtonY, buttonWidth, 20, Components.translatable("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept(VariableHandler.getVariables());
        });
        this.addRenderableWidget(doneButton);
        UIBase.applyDefaultWidgetSkinTo(doneButton);

        this.updateVariablesList();

    }

    @Override
    public void onClose() {
        this.callback.accept(VariableHandler.getVariables());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        graphics.drawString(this.font, titleComp, 20, 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        graphics.drawString(this.font, Components.translatable("fancymenu.overlay.menu_bar.variables.manage.variables"), 20, 50, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.variableListScrollArea.render(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    @Nullable
    protected ManageVariablesScreen.VariableScrollEntry getSelectedEntry() {
        for (ScrollAreaEntry e : this.variableListScrollArea.getEntries()) {
            if (e instanceof VariableScrollEntry s) {
                if (s.isSelected()) return s;
            }
        }
        return null;
    }

    protected boolean variableFitsSearchValue(@NotNull Variable variable, @Nullable String s) {
        if ((s == null) || s.isBlank()) return true;
        s = s.toLowerCase();
        if (variable.getName().toLowerCase().contains(s)) return true;
        return variable.getValue().toLowerCase().contains(s);
    }

    protected void updateVariablesList() {

        String searchValue = (this.searchBar != null) ? this.searchBar.getValue() : "";
        if (searchValue.isBlank()) searchValue = null;

        this.variableListScrollArea.clearEntries();

        for (Variable v : VariableHandler.getVariables()) {
            if (!this.variableFitsSearchValue(v, searchValue)) continue;
            VariableScrollEntry e = new VariableScrollEntry(this.variableListScrollArea, v, (entry) -> {
            });
            this.variableListScrollArea.addEntry(e);
        }
        if (this.variableListScrollArea.getEntries().isEmpty()) {
            this.variableListScrollArea.addEntry(new TextScrollAreaEntry(this.variableListScrollArea, Components.translatable("fancymenu.overlay.menu_bar.variables.manage.no_variables").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().error_text_color.getColorInt())), (entry) -> {}));
        }

    }

    public static class VariableScrollEntry extends TextListScrollAreaEntry {

        public Variable variable;

        public VariableScrollEntry(ScrollArea parent, @NotNull Variable variable, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(variable.name).setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().description_area_text_color.getColorInt())).append(Component.literal(" (" + variable.getValue() + ")").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()))), UIBase.getUIColorTheme().listing_dot_color_1.getColor(), onClick);
            this.variable = variable;
        }

    }

}
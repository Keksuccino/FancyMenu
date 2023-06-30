package de.keksuccino.fancymenu.customization.variables;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedValueCycle;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfirmationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.TextInputScreen;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.TextListScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ManageVariablesScreen extends Screen {

    protected Consumer<List<Variable>> callback;

    protected ScrollArea variableListScrollArea = new ScrollArea(0, 0, 0, 0);
    protected ExtendedButton doneButton;
    protected ExtendedButton setValueButton;
    protected ExtendedButton deleteVariableButton;
    protected ExtendedButton addVariableButton;
    protected ExtendedButton toggleResetOnLaunchButton;

    public ManageVariablesScreen(@NotNull Consumer<List<Variable>> callback) {

        super(Component.translatable("fancymenu.overlay.menu_bar.variables.manage"));

        this.callback = callback;
        this.updateVariableScrollArea();

    }

    @Override
    protected void init() {

        super.init();

        this.addVariableButton = new ExtendedButton(0, 0, 220, 20, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.add_variable"), (button) -> {
            TextInputScreen s = new TextInputScreen(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.add_variable.input_name"), CharacterFilter.getBasicFilenameCharacterFilter(), (call) -> {
                if (call != null) {
                    if (!VariableHandler.variableExists(call)) {
                        VariableHandler.setVariable(call, "");
                        updateVariableScrollArea();
                    }
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.addVariableButton);
        UIBase.applyDefaultWidgetSkinTo(this.addVariableButton);

        this.setValueButton = new ExtendedButton(0, 0, 220, 20, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"), (button) -> {
            VariableScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                TextInputScreen s = new TextInputScreen(Component.translatable("fancymenu.overlay.menu_bar.variables.manage.set_value"), null, (call) -> {
                    if (call != null) {
                        e.variable.setValue(call);
                    }
                    Minecraft.getInstance().setScreen(this);
                });
                s.setText(e.variable.getValue());
                Minecraft.getInstance().setScreen(s);
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addWidget(this.setValueButton);
        UIBase.applyDefaultWidgetSkinTo(this.setValueButton);

        this.deleteVariableButton = new ExtendedButton(0, 0, 220, 20, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.delete_variable"), (button) -> {
            VariableScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                Minecraft.getInstance().setScreen(ConfirmationScreen.ofStrings(call -> {
                    if (call) {
                        VariableHandler.removeVariable(e.variable.getName());
                        this.updateVariableScrollArea();
                    }
                    Minecraft.getInstance().setScreen(this);
                }, LocalizationUtils.splitLocalizedStringLines("fancymenu.overlay.menu_bar.variables.manage.delete_variable.confirm")));
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null));
        this.addWidget(this.deleteVariableButton);
        UIBase.applyDefaultWidgetSkinTo(this.deleteVariableButton);

        LocalizedValueCycle<CommonCycles.CycleEnabledDisabled> resetOnLaunchDisabled = CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.variables.manage.clear_on_launch", false);
        this.toggleResetOnLaunchButton = new ExtendedButton(0, 0, 220, 20, Component.empty(), (button) -> {
            VariableScrollEntry e = this.getSelectedEntry();
            if (e != null) {
                e.variable.setResetOnLaunch(!e.variable.isResetOnLaunch());
            }
        }).setIsActiveSupplier(consumes -> (this.getSelectedEntry() != null))
                .setLabelSupplier(consumes -> {
                    VariableScrollEntry e = this.getSelectedEntry();
                    if (e != null) {
                        LocalizedValueCycle<CommonCycles.CycleEnabledDisabled> enabledDisabled = CommonCycles.cycleEnabledDisabled("fancymenu.overlay.menu_bar.variables.manage.clear_on_launch");
                        enabledDisabled.setCurrentValue(CommonCycles.CycleEnabledDisabled.getByBoolean(e.variable.isResetOnLaunch()));
                        return enabledDisabled.getCycleComponent();
                    }
                    return resetOnLaunchDisabled.getCycleComponent();
                });
        this.addWidget(this.toggleResetOnLaunchButton);
        UIBase.applyDefaultWidgetSkinTo(this.toggleResetOnLaunchButton);

        this.doneButton = new ExtendedButton(0, 0, 220, 20, Component.translatable("fancymenu.guicomponents.done"), (button) -> {
            this.callback.accept(VariableHandler.getVariables());
        });
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

    }

    @Override
    public void onClose() {
        this.callback.accept(VariableHandler.getVariables());
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screen_background_color.getColorInt());

        Component titleComp = this.title.copy().withStyle(Style.EMPTY.withBold(true));
        this.font.draw(pose, titleComp, 20, 20, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.font.draw(pose, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.variables"), 20, 50, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());

        this.variableListScrollArea.setWidth((this.width / 2) - 40, true);
        this.variableListScrollArea.setHeight(this.height - 85, true);
        this.variableListScrollArea.setX(20, true);
        this.variableListScrollArea.setY(50 + 15, true);
        this.variableListScrollArea.render(pose, mouseX, mouseY, partial);

        int buttonWidth = (this.width - 20) - ((this.variableListScrollArea.getXWithBorder() + (this.variableListScrollArea.getWidthWithBorder() + 20)));
        if (buttonWidth < 150) buttonWidth = 150;
        if (buttonWidth > 220) buttonWidth = 220;

        this.doneButton.setWidth(buttonWidth);
        this.doneButton.setX(this.width - 20 - this.doneButton.getWidth());
        this.doneButton.setY(this.height - 20 - 20);
        this.doneButton.render(pose, mouseX, mouseY, partial);

        this.toggleResetOnLaunchButton.setWidth(buttonWidth);
        this.toggleResetOnLaunchButton.setX(this.width - 20 - this.toggleResetOnLaunchButton.getWidth());
        this.toggleResetOnLaunchButton.setY(this.doneButton.getY() - 15 - 20);
        this.toggleResetOnLaunchButton.render(pose, mouseX, mouseY, partial);

        this.deleteVariableButton.setWidth(buttonWidth);
        this.deleteVariableButton.setX(this.width - 20 - this.deleteVariableButton.getWidth());
        this.deleteVariableButton.setY(this.toggleResetOnLaunchButton.getY() - 5 - 20);
        this.deleteVariableButton.render(pose, mouseX, mouseY, partial);

        this.setValueButton.setWidth(buttonWidth);
        this.setValueButton.setX(this.width - 20 - this.setValueButton.getWidth());
        this.setValueButton.setY(this.deleteVariableButton.getY() - 5 - 20);
        this.setValueButton.render(pose, mouseX, mouseY, partial);

        this.addVariableButton.setWidth(buttonWidth);
        this.addVariableButton.setX(this.width - 20 - this.addVariableButton.getWidth());
        this.addVariableButton.setY(this.setValueButton.getY() - 15 - 20);
        this.addVariableButton.render(pose, mouseX, mouseY, partial);

        super.render(pose, mouseX, mouseY, partial);

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

    protected void updateVariableScrollArea() {
        this.variableListScrollArea.clearEntries();
        for (Variable v : VariableHandler.getVariables()) {
            VariableScrollEntry e = new VariableScrollEntry(this.variableListScrollArea, v, (entry) -> {
            });
            this.variableListScrollArea.addEntry(e);
        }
        if (this.variableListScrollArea.getEntries().isEmpty()) {
            this.variableListScrollArea.addEntry(new TextScrollAreaEntry(this.variableListScrollArea, Component.translatable("fancymenu.overlay.menu_bar.variables.manage.no_variables").setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().error_text_color.getColorInt())), (entry) -> {}));
        }
    }

    public static class VariableScrollEntry extends TextListScrollAreaEntry {

        public Variable variable;

        public VariableScrollEntry(ScrollArea parent, @NotNull Variable variable, @NotNull Consumer<TextListScrollAreaEntry> onClick) {
            super(parent, Component.literal(variable.name).setStyle(Style.EMPTY.withColor(UIBase.getUIColorScheme().description_area_text_color.getColorInt())), UIBase.getUIColorScheme().listing_dot_color_1.getColor(), onClick);
            this.variable = variable;
        }

    }

}

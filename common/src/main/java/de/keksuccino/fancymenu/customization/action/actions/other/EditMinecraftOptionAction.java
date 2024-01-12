package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.minecraftoptions.MinecraftOptions;
import de.keksuccino.fancymenu.util.minecraftoptions.MinecraftOption;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class EditMinecraftOptionAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public EditMinecraftOptionAction() {
        super("edit_minecraft_option");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {

        if ((value != null) && value.contains(":")) {

            String name = value.split(":", 2)[0];
            String setTo = value.split(":", 2)[1];

            MinecraftOption instance = MinecraftOptions.getOption(name);
            if (instance == null) {
                LOGGER.error("[FANCYMENU] EditMinecraftOptionAction was unable to find Minecraft config option: " + name);
                return;
            }
            try {
                instance.set(setTo);
                Minecraft.getInstance().options.save();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] EditMinecraftOptionAction failed to set Minecraft config option value '" + setTo + "' to option '" + name + "'.", ex);
            }

        }

    }

    @NotNull
    protected static List<String> getSupportedOptionNames() {
        List<String> names = new ArrayList<>();
        MinecraftOptions.getOptions().values().forEach(optionInstance -> names.add(optionInstance.getName()));
        return names;
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.edit_minecraft_option");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.edit_minecraft_option.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValueExample() {
        return "option_name:set_to_value";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {
        EditMinecraftOptionActionValueScreen s = new EditMinecraftOptionActionValueScreen(Objects.requireNonNullElse(instance.value, this.getValueExample()), value -> {
            if (value != null) {
                instance.value = value;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public static class EditMinecraftOptionActionValueScreen extends StringBuilderScreen {

        @NotNull
        protected String name = "";
        @NotNull
        protected String setTo = "";
        protected EditBoxSuggestions optionNameSuggestions;
        @Nullable
        protected LabelCell currentOptionValueLabelCell = null;

        @SuppressWarnings("all")
        protected EditMinecraftOptionActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.editor.actions.generic_edit_value"), callback);
            if (value.contains(":")) {
                this.name = value.split(":", 2)[0];
                this.setTo = value.split(":", 2)[1];
            }
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.edit_minecraft_option.edit.option_name"));
            TextInputCell nameCell = this.addTextInputCell(null, true, true).setText(this.name);

            this.optionNameSuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, nameCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, getSupportedOptionNames());
            UIBase.applyDefaultWidgetSkinTo(this.optionNameSuggestions);
            nameCell.editBox.setResponder(s -> {
                this.optionNameSuggestions.updateCommandInfo();
                this.name = s;
                if (this.currentOptionValueLabelCell != null) {
                    this.currentOptionValueLabelCell.setText(this.buildCurrentOptionValueComponent());
                }
            });

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable("fancymenu.actions.edit_minecraft_option.edit.set_to_value"));
            this.addTextInputCell(null, true, true).setEditListener(s -> this.setTo = s).setText(this.setTo);

            this.addCellGroupEndSpacerCell();

            this.currentOptionValueLabelCell = this.addLabelCell(this.buildCurrentOptionValueComponent());

            this.addSpacerCell(20);

        }

        @NotNull
        protected Component buildCurrentOptionValueComponent() {
            MinecraftOption instance = MinecraftOptions.getOption(this.name);
            String current = (instance != null) ? instance.get() : "-----";
            if (current == null) current = "-----";
            Component curComp = Component.literal(current).setStyle(Style.EMPTY.withBold(false));
            return Component.translatable("fancymenu.actions.edit_minecraft_option.edit.current_value", curComp).setStyle(Style.EMPTY.withBold(true));
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);
            this.optionNameSuggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.optionNameSuggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaX, double scrollDeltaY) {
            if (this.optionNameSuggestions.mouseScrolled(scrollDeltaY)) return true;
            return super.mouseScrolled($$0, $$1, scrollDeltaX, scrollDeltaY);
        }

        @Override
        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (this.optionNameSuggestions.mouseClicked($$0, $$1, $$2)) return true;
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public @NotNull String buildString() {
            return this.name + ":" + this.setTo;
        }

    }

}

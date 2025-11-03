package de.keksuccino.fancymenu.customization.loadingrequirement.requirements;

import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import de.keksuccino.konkrete.Konkrete;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsModLoadedRequirement extends LoadingRequirement {

    public IsModLoadedRequirement() {
        super("fancymenu_loading_requirement_is_mod_loaded");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            List<String> l = this.parseStrings(value);
            if (!l.isEmpty()) {
                for (String s : l) {
                    if (s.equalsIgnoreCase("optifine")) {
                        if (!Konkrete.isOptifineLoaded) {
                            return false;
                        }
                    } else {
                        if (!Services.PLATFORM.isModLoaded(s)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }

        return false;

    }

    protected List<String> parseStrings(String value) {
        List<String> l = new ArrayList<>();
        if (value != null) {
            if (value.contains(",")) {
                l.addAll(Arrays.asList(value.replace(" ", "").split(",")));
            } else {
                l.add(value.replace(" ", ""));
            }
        }
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.is_mod_loaded");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_mod_loaded.desc"));
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getValueDisplayName() {
        return "";
    }

    @Override
    public String getValuePreset() {
        return "optifine";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull LoadingRequirementInstance requirementInstance) {
        IsModLoadedValueConfigScreen s = new IsModLoadedValueConfigScreen(Objects.requireNonNullElse(requirementInstance.value, ""),callback -> {
            if (callback != null) {
                requirementInstance.value = callback;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public static class IsModLoadedValueConfigScreen extends StringBuilderScreen {

        @NotNull
        protected String modId;

        protected TextInputCell modIdCell;
        protected EditBoxSuggestions modIdSuggestions;

        protected IsModLoadedValueConfigScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.requirements.is_mod_loaded.value_name"), callback);
            this.modId = value;
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            String id = this.getModIdString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_mod_loaded.mod_id"));
            this.modIdCell = this.addTextInputCell(null, true, true).setText(id);

            this.modIdSuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.modIdCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, Services.PLATFORM.getLoadedModIds());
            UIBase.applyDefaultWidgetSkinTo(this.modIdSuggestions);
            this.modIdCell.editBox.setResponder(s -> this.modIdSuggestions.updateCommandInfo());

            this.addSpacerCell(20);

        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);
            this.modIdSuggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.modIdSuggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double $$2) {
            if (this.modIdSuggestions.mouseScrolled($$2)) return true;
            return super.mouseScrolled($$0, $$1, $$2);
        }

        @Override
        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (this.modIdSuggestions.mouseClicked($$0, $$1, $$2)) return true;
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public @NotNull String buildString() {
            return this.getModIdString();
        }

        @NotNull
        protected String getModIdString() {
            if (this.modIdCell != null) {
                return this.modIdCell.getText();
            }
            return this.modId;
        }

    }

}

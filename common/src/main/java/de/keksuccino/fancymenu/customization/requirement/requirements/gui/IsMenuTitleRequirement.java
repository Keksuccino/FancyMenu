package de.keksuccino.fancymenu.customization.requirement.requirements.gui;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ScreenTitleUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsMenuTitleRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsMenuTitleRequirement() {
        super("is_menu_title");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {
            Screen s = Minecraft.getInstance().screen;
            if (s != null) {
                String key = null;
                String text;
                Component title = ScreenTitleUtils.getTitleOfScreen(s);
                if (title instanceof MutableComponent) {
                    ComponentContents cc = title.getContents();
                    if (cc instanceof TranslatableContents t) {
                        key = t.getKey();
                    }
                }
                text = title.getString();
                if ((key != null) && !I18n.get(value).equals(value)) {
                    return key.equals(value);
                }
                return text.equals(value);
            }
        }

        return false;

    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.is_menu_title");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_menu_title.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public String getValueDisplayName() {
        return "";
    }

    @Override
    public String getValuePreset() {
        return "Example Title";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull RequirementInstance instance, @NotNull RequirementEditingCompletedFeedback onEditingCompleted, @NotNull RequirementEditingCanceledFeedback onEditingCanceled) {
        boolean[] handled = {false};
        final Runnable[] closeAction = new Runnable[] {() -> {}};
        IsMenuTitleValueConfigScreen s = new IsMenuTitleValueConfigScreen(Objects.requireNonNullElse(instance.value, ""), callback -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            if (callback != null) {
                String oldValue = instance.value;
                instance.value = callback;
                onEditingCompleted.accept(instance, oldValue, callback);
            } else {
                onEditingCanceled.accept(instance);
            }
            closeAction[0].run();
        });
        closeAction[0] = Requirement.openRequirementValueEditor(s, () -> {
            if (handled[0]) {
                return;
            }
            handled[0] = true;
            onEditingCanceled.accept(instance);
        });
    }

    public static class IsMenuTitleValueConfigScreen extends Requirement.RequirementValueEditScreen {

        @NotNull
        protected String menuTitleOrKey;

        protected TextInputCell menuTitleOrKeyCell;
        protected EditBoxSuggestions localizationKeySuggestions;

        protected IsMenuTitleValueConfigScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.elements.requirements.edit_value"), callback);
            this.menuTitleOrKey = value;
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            String id = this.getMenuTitleOrKeyString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_menu_title.value"));
            this.menuTitleOrKeyCell = this.addTextInputCell(null, true, true).setText(id);

            this.localizationKeySuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.menuTitleOrKeyCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, LocalizationUtils.getLocalizationKeys());
            UIBase.applyDefaultWidgetSkinTo(this.localizationKeySuggestions);
            this.menuTitleOrKeyCell.editBox.setResponder(s -> this.localizationKeySuggestions.updateCommandInfo());

            this.addSpacerCell(10);

            String key = "[UNKNOWN OR UNIVERSAL LAYOUT]";
            String plain = "[UNKNOWN OR UNIVERSAL LAYOUT]";
            LayoutEditorScreen editor = LayoutEditorScreen.getCurrentInstance();
            if (editor != null) {
                if (editor.layoutTargetScreen != null) {
                    String k = ScreenTitleUtils.getTitleLocalizationKeyOfScreen(editor.layoutTargetScreen);
                    if (k != null) key = k;
                    plain = ScreenTitleUtils.getTitleOfScreen(editor.layoutTargetScreen).getString();
                }
            }
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_menu_title.value.current_menu_title_key", Component.literal(key).setStyle(Style.EMPTY.withBold(false))).setStyle(Style.EMPTY.withBold(true)));
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_menu_title.value.current_menu_title_plain", Component.literal(plain).setStyle(Style.EMPTY.withBold(false))).setStyle(Style.EMPTY.withBold(true)));

            this.addSpacerCell(20);

        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);
            this.localizationKeySuggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.localizationKeySuggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaX, double scrollDeltaY) {
            if (this.localizationKeySuggestions.mouseScrolled(scrollDeltaY)) return true;
            return super.mouseScrolled($$0, $$1, scrollDeltaX, scrollDeltaY);
        }

        @Override
        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (this.localizationKeySuggestions.mouseClicked($$0, $$1, $$2)) return true;
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public @NotNull String buildString() {
            return this.getMenuTitleOrKeyString();
        }

        @NotNull
        protected String getMenuTitleOrKeyString() {
            if (this.menuTitleOrKeyCell != null) {
                return this.menuTitleOrKeyCell.getText();
            }
            return this.menuTitleOrKey;
        }

    }

}

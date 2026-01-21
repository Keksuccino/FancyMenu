package de.keksuccino.fancymenu.customization.requirement.requirements.world.player;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsGameModeRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsGameModeRequirement() {
        super("is_gamemode");
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
        try {
            if ((value == null) || value.trim().isEmpty()) return false;
            ClientLevel level = Minecraft.getInstance().level;
            LocalPlayer player = Minecraft.getInstance().player;
            if ((level != null) && (player != null) && (Minecraft.getInstance().gameMode != null)) {
                GameType gameMode = Minecraft.getInstance().gameMode.getPlayerMode();
                if (gameMode != null) return gameMode.getName().equals(value);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
        }
        return false;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.world.is_gamemode");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.world.is_gamemode.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.requirements.world.is_gamemode.value");
    }

    @Override
    public String getValuePreset() {
        return "creative";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull RequirementInstance instance, @NotNull RequirementEditingCompletedFeedback onEditingCompleted, @NotNull RequirementEditingCanceledFeedback onEditingCanceled) {
        boolean[] handled = {false};
        final Runnable[] closeAction = new Runnable[] {() -> {}};
        IsGameModeValueConfigScreen s = new IsGameModeValueConfigScreen(Objects.requireNonNullElse(instance.value, this.getValuePreset()), callback -> {
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

    private static @NotNull List<String> getGameModeNames() {
        List<String> keys = new ArrayList<>();
        try {
            for (GameType type : GameType.values()) {
                keys.add(type.getName());
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get game mode names for 'Is Game Mode' loading requirement!", ex);
        }
        return keys;
    }

    public static class IsGameModeValueConfigScreen extends Requirement.RequirementValueEditScreen {

        @NotNull
        protected String value;

        protected TextInputCell textInput;
        protected EditBoxSuggestions suggestions;

        protected IsGameModeValueConfigScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.elements.requirements.edit_value"), callback);
            this.value = value;
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            String id = this.getValueString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.world.is_gamemode.value"));
            this.textInput = this.addTextInputCell(null, true, true).setText(id);

            this.addCellGroupEndSpacerCell();

            List<String> suggestionValues = new ArrayList<>(getGameModeNames());
            this.suggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.textInput.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, suggestionValues);
            UIBase.applyDefaultWidgetSkinTo(this.suggestions);
            this.textInput.editBox.setResponder(s -> this.suggestions.updateCommandInfo());

            this.addSpacerCell(20);

        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);
            this.suggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.suggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaX, double scrollDeltaY) {
            if (this.suggestions.mouseScrolled(scrollDeltaY)) return true;
            return super.mouseScrolled($$0, $$1, scrollDeltaX, scrollDeltaY);
        }

        @Override
        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (this.suggestions.mouseClicked($$0, $$1, $$2)) return true;
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public @NotNull String buildString() {
            return this.getValueString();
        }

        @NotNull
        protected String getValueString() {
            if (this.textInput != null) {
                return this.textInput.getText();
            }
            return this.value;
        }

    }

}

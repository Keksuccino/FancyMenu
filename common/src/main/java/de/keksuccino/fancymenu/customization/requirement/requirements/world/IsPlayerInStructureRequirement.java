package de.keksuccino.fancymenu.customization.requirement.requirements.world;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.networking.packets.structure.playerpos.PlayerPosStructuresPacket;
import de.keksuccino.fancymenu.networking.packets.structure.structures.StructuresPacket;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class IsPlayerInStructureRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static long lastStructureUpdate = -1;

    public IsPlayerInStructureRequirement() {
        super("is_player_in_structure");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {

        if (value != null) {

            try {

                long now = System.currentTimeMillis();
                if ((lastStructureUpdate + 1000) < now) {
                    PacketHandler.sendToServer(new PlayerPosStructuresPacket());
                    lastStructureUpdate = now;
                }

                return PlayerPosStructuresPacket.CACHED_CURRENT_STRUCTURES.contains(value);

            } catch (Exception ex) {
                LOGGER.error("[SPIFFY HUD] Failed to check for 'Is Player In Structure' requirement!", ex);
            }

        }

        return false;

    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.is_player_in_structure");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.is_player_in_structure.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    @Override
    public String getValueDisplayName() {
        return "";
    }

    @Override
    public String getValuePreset() {
        return "minecraft:village";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull RequirementInstance requirementInstance) {
        IsPlayerInStructureValueConfigScreen s = new IsPlayerInStructureValueConfigScreen(Objects.requireNonNullElse(requirementInstance.value, this.getValuePreset()), callback -> {
            if (callback != null) {
                requirementInstance.value = callback;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public static class IsPlayerInStructureValueConfigScreen extends StringBuilderScreen {

        @NotNull
        protected String oldStructureKey;

        protected TextInputCell structureKeyCell;
        protected EditBoxSuggestions structureKeySuggestions;

        protected IsPlayerInStructureValueConfigScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.requirements.screens.build_screen.edit_value"), callback);
            this.oldStructureKey = value;
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            String id = this.getStructureKeyString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_player_in_structure.key"));
            this.structureKeyCell = this.addTextInputCell(null, true, true).setText(id);

            this.addCellGroupEndSpacerCell();

            this.structureKeySuggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.structureKeyCell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, new ArrayList<>(StructuresPacket.CACHED_SERVER_STRUCTURE_KEYS));
            UIBase.applyDefaultWidgetSkinTo(this.structureKeySuggestions);
            this.structureKeyCell.editBox.setResponder(s -> this.structureKeySuggestions.updateCommandInfo());

            this.addSpacerCell(20);

        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);
            this.structureKeySuggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.structureKeySuggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double $$2, double d) {
            if (this.structureKeySuggestions.mouseScrolled(d)) return true;
            return super.mouseScrolled($$0, $$1, $$2, d);
        }

        @Override
        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (this.structureKeySuggestions.mouseClicked($$0, $$1, $$2)) return true;
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public @NotNull String buildString() {
            return this.getStructureKeyString();
        }

        @NotNull
        protected String getStructureKeyString() {
            if (this.structureKeyCell != null) {
                return this.structureKeyCell.getText();
            }
            return this.oldStructureKey;
        }

    }

}

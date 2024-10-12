package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.player;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

//TODO übernehmen
public class IsEffectActiveRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsEffectActiveRequirement() {
        super("is_effect_active");
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
            if ((level != null) && (player != null)) {
                for (MobEffectInstance effect : player.getActiveEffects()) {
                    ResourceLocation loc = Services.PLATFORM.getEffectKey(effect.getEffect());
                    if ((loc != null) && loc.toString().equals(value)) return true;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
        }
        return false;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.world.is_effect_active");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.world.is_effect_active.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.editor.loading_requirement.category.world");
    }

    @Override
    public String getValueDisplayName() {
        return I18n.get("fancymenu.requirements.world.is_effect_active.value");
    }

    @Override
    public String getValuePreset() {
        return "minecraft:bad_omen";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull LoadingRequirementInstance requirementInstance) {
        IsEffectActiveValueConfigScreen s = new IsEffectActiveValueConfigScreen(Objects.requireNonNullElse(requirementInstance.value, this.getValuePreset()), callback -> {
            if (callback != null) {
                requirementInstance.value = callback;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    private static @NotNull List<ResourceLocation> getEffectKeys() {
        List<ResourceLocation> keys = new ArrayList<>();
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Registry.MOB_EFFECT.asHolderIdMap().forEach(biomeHolder -> biomeHolder.unwrap().map(key -> keys.add(key.location()), biome -> ""));
                //level.registryAccess().lookupOrThrow(Registries.MOB_EFFECT).listElementIds().forEach(key -> keys.add(key.location()));
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get effect keys for 'Is Effect Active' loading requirement!", ex);
        }
        return keys;
    }

    public static class IsEffectActiveValueConfigScreen extends StringBuilderScreen {

        @NotNull
        protected String value;

        protected TextInputCell textInput;
        protected EditBoxSuggestions suggestions;

        protected IsEffectActiveValueConfigScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Components.translatable("fancymenu.editor.elements.visibilityrequirements.edit_value"), callback);
            this.value = value;
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            String id = this.getValueString();
            this.addLabelCell(Components.translatable("fancymenu.requirements.world.is_effect_active.value"));
            this.textInput = this.addTextInputCell(null, true, true).setText(id);

            this.addCellGroupEndSpacerCell();

            List<String> suggestionValues = new ArrayList<>();
            getEffectKeys().forEach(location -> suggestionValues.add(location.toString()));
            if (suggestionValues.isEmpty()) {
                suggestionValues.add(I18n.get("fancymenu.requirements.world.is_effect_active.suggestions.error"));
            }

            this.suggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.textInput.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, suggestionValues);
            UIBase.applyDefaultWidgetSkinTo(this.suggestions);
            this.textInput.editBox.setResponder(s -> this.suggestions.updateCommandInfo());

            this.addSpacerCell(20);

        }

        @Override
        public void render(@NotNull PoseStack graphics, int mouseX, int mouseY, float partial) {
            super.render(graphics, mouseX, mouseY, partial);
            this.suggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.suggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaY) {
            if (this.suggestions.mouseScrolled(scrollDeltaY)) return true;
            return super.mouseScrolled($$0, $$1, scrollDeltaY);
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

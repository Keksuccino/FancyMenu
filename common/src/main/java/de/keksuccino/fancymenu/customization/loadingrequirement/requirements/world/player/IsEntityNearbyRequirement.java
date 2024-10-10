package de.keksuccino.fancymenu.customization.loadingrequirement.requirements.world.player;

import de.keksuccino.fancymenu.customization.loadingrequirement.LoadingRequirement;
import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementInstance;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

//TODO übernehmen
public class IsEntityNearbyRequirement extends LoadingRequirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsEntityNearbyRequirement() {
        super("is_entity_nearby");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            LocalPlayer player = Minecraft.getInstance().player;
            if ((level != null) && (player != null)) {
                if ((value == null) || value.trim().isEmpty() || !value.contains(":")) return false;
                String[] valsRaw = value.split(":", 2);
                int radius = SerializationUtils.deserializeNumber(Integer.class, 1, valsRaw[0]);
                String entityKey = valsRaw[1];
                for (Entity entity : getEntitiesAroundPlayer(player, level, radius)) {
                    ResourceLocation loc = Services.PLATFORM.getEntityKey(entity.getType());
                    if (loc != null) {
                        if (loc.toString().equals(entityKey)) return true;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
        }
        return false;
    }

    @NotNull
    private static List<Entity> getEntitiesAroundPlayer(@NotNull Player player, @NotNull ClientLevel level, double radius) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        // Create an axis-aligned bounding box (AABB) around the player
        AABB boundingBox = new AABB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );
        // Get all entities within the bounding box
        return level.getEntities(player, boundingBox, entity -> true);
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.requirements.world.is_entity_nearby");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.requirements.world.is_entity_nearby.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.editor.loading_requirement.category.world");
    }

    @Override
    public String getValueDisplayName() {
        return "";
    }

    @Override
    public String getValuePreset() {
        return "10:minecraft:pig";
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull LoadingRequirementInstance requirementInstance) {
        IsEntityNearbyValueConfigScreen s = new IsEntityNearbyValueConfigScreen(Objects.requireNonNullElse(requirementInstance.value, this.getValuePreset()), callback -> {
            if (callback != null) {
                requirementInstance.value = callback;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    private static @NotNull List<ResourceLocation> getEntityKeys() {
        List<ResourceLocation> types = new ArrayList<>();
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).listElementIds().forEach(key -> types.add(key.location()));
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get entity types for 'Is Entity Nearby' loading requirement!", ex);
        }
        return types;
    }

    public static class IsEntityNearbyValueConfigScreen extends StringBuilderScreen {

        @NotNull
        protected String radius;
        @NotNull
        protected String entityKey;

        protected TextInputCell radiusTextInput;
        protected TextInputCell entityKeyTextInput;
        protected EditBoxSuggestions suggestions;

        protected IsEntityNearbyValueConfigScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.editor.elements.visibilityrequirements.edit_value"), callback);
            if (value.contains(":")) {
                this.radius = value.split(":", 2)[0];
                this.entityKey = value.split(":", 2)[1];
            } else {
                this.radius = "10";
                this.entityKey = "minecraft:pig";
            }
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            //ENTITY KEY INPUT
            String entityId = this.getEntityKeyString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.world.is_entity_nearby.value.key"));
            this.entityKeyTextInput = this.addTextInputCell(null, true, true).setText(entityId);
            List<String> suggestionValues = new ArrayList<>();
            getEntityKeys().forEach(location -> suggestionValues.add(location.toString()));
            if (suggestionValues.isEmpty()) {
                suggestionValues.add(I18n.get("fancymenu.requirements.world.is_entity_nearby.suggestions.error"));
            }
            this.suggestions = EditBoxSuggestions.createWithCustomSuggestions(this, this.entityKeyTextInput.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, suggestionValues);
            UIBase.applyDefaultWidgetSkinTo(this.suggestions);
            this.entityKeyTextInput.editBox.setResponder(s -> this.suggestions.updateCommandInfo());

            //SEPARATOR
            this.addCellGroupEndSpacerCell();

            //RADIUS INPUT
            String radiusString = this.getRadiusString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.world.is_entity_nearby.value.radius"));
            this.radiusTextInput = this.addTextInputCell(null, true, true).setText(radiusString);

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
            return this.getRadiusString() + ":" + this.getEntityKeyString();
        }

        @NotNull
        protected String getRadiusString() {
            if (this.radiusTextInput != null) {
                return this.radiusTextInput.getText();
            }
            return this.radius;
        }

        @NotNull
        protected String getEntityKeyString() {
            if (this.entityKeyTextInput != null) {
                return this.entityKeyTextInput.getText();
            }
            return this.entityKey;
        }

    }

}

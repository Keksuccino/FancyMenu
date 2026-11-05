package de.keksuccino.fancymenu.customization.requirement.requirements.world;

import de.keksuccino.fancymenu.customization.requirement.Requirement;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementInstance;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.ILocalizedValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.EditBoxSuggestions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class IsInTagRequirement extends Requirement {

    private static final Logger LOGGER = LogManager.getLogger();

    public IsInTagRequirement() {
        super("is_in_tag");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public boolean isRequirementMet(@Nullable String value) {
        TagCheck check = TagCheck.parse(value);
        if (check == null) return false;
        try {
            return check.type().isInTag(check.key(), check.tag());
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle '" + this.getIdentifier() + "' loading requirement!", ex);
        }
        return false;
    }

    /**
     * Checks if the registry entry with the given key is part of the given tag.<br>
     * Keys and tags without a namespace default to {@code minecraft}, and tags can start with an optional {@code #}.<br><br>
     *
     * Returns false for unknown keys and invalid keys or tags, instead of falling back to a registry's default entry (like air).
     */
    public static <T> boolean isInTag(@NotNull Registry<T> registry, @NotNull String key, @NotNull String tag) {
        Identifier keyId = parseIdentifier(key);
        Identifier tagId = parseIdentifier(stripTagPrefix(tag));
        if ((keyId == null) || (tagId == null)) return false;
        Optional<Holder.Reference<T>> holder = registry.get(ResourceKey.create(registry.key(), keyId));
        return holder.isPresent() && holder.get().is(TagKey.create(registry.key(), tagId));
    }

    @Nullable
    private static Identifier parseIdentifier(@NotNull String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;
        return Identifier.tryParse(normalized);
    }

    @NotNull
    private static String stripTagPrefix(@NotNull String tag) {
        String trimmed = tag.trim();
        return trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    }

    @NotNull
    private static <T> List<String> getSortedKeys(@NotNull Registry<T> registry) {
        return registry.keySet().stream().map(Identifier::toString).sorted().toList();
    }

    @NotNull
    private static <T> List<String> getSortedTags(@NotNull Registry<T> registry) {
        return registry.listTagIds().map(tagKey -> tagKey.location().toString()).sorted().toList();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.requirements.is_in_tag");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("fancymenu.requirements.is_in_tag.desc");
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.world");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.literal("");
    }

    @Override
    public String getValuePreset() {
        return new TagCheck(TagType.BLOCK, "minecraft:logs", "minecraft:oak_log").serialize();
    }

    @Override
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    @Override
    public void editValue(@NotNull RequirementInstance instance, @NotNull RequirementEditingCompletedFeedback onEditingCompleted, @NotNull RequirementEditingCanceledFeedback onEditingCanceled) {
        boolean[] handled = {false};
        final Runnable[] closeAction = new Runnable[] {() -> {}};
        TagCheck check = Objects.requireNonNullElseGet(TagCheck.parse(instance.value), () -> Objects.requireNonNull(TagCheck.parse(this.getValuePreset())));
        IsInTagValueConfigScreen s = new IsInTagValueConfigScreen(check, callback -> {
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

    /**
     * The parsed requirement value.<br><br>
     *
     * Serialized as {@code type|tag|key}. The key is the last part, so it can contain anything after placeholders got replaced.
     */
    public record TagCheck(@NotNull TagType type, @NotNull String tag, @NotNull String key) {

        private static final String SEPARATOR = "|";

        @Nullable
        public static TagCheck parse(@Nullable String value) {
            if (value == null) return null;
            String[] parts = value.split("\\|", 3);
            if (parts.length < 3) return null;
            TagType type = TagType.getByKey(parts[0].trim());
            if (type == null) return null;
            return new TagCheck(type, parts[1], parts[2]);
        }

        @NotNull
        public String serialize() {
            return this.type.key + SEPARATOR + this.tag + SEPARATOR + this.key;
        }

    }

    public enum TagType {

        BLOCK("block"),
        ITEM("item");

        public final String key;

        TagType(@NotNull String key) {
            this.key = key;
        }

        /**
         * Resolved lazily instead of in the constructor, so parsing values never needs bootstrapped registries.
         */
        @NotNull
        public Registry<?> getRegistry() {
            return (this == ITEM) ? BuiltInRegistries.ITEM : BuiltInRegistries.BLOCK;
        }

        public boolean isInTag(@NotNull String key, @NotNull String tag) {
            return IsInTagRequirement.isInTag(this.getRegistry(), key, tag);
        }

        @NotNull
        public List<String> getKeySuggestions() {
            return getSortedKeys(this.getRegistry());
        }

        @NotNull
        public List<String> getTagSuggestions() {
            try {
                return getSortedTags(this.getRegistry());
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get tags for 'Is Block/Item In Tag' loading requirement!", ex);
            }
            return List.of();
        }

        @NotNull
        public String getLocalizationKey() {
            return "fancymenu.requirements.is_in_tag.type." + this.key;
        }

        @Nullable
        public static TagType getByKey(@NotNull String key) {
            for (TagType type : TagType.values()) {
                if (type.key.equals(key)) return type;
            }
            return null;
        }

    }

    public static class IsInTagValueConfigScreen extends Requirement.RequirementValueEditScreen {

        @NotNull
        protected TagType type;
        @NotNull
        protected String tag;
        @NotNull
        protected String key;

        protected TextInputCell keyCell;
        protected TextInputCell tagCell;
        protected EditBoxSuggestions keySuggestions;
        protected EditBoxSuggestions tagSuggestions;

        protected IsInTagValueConfigScreen(@NotNull TagCheck check, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.elements.requirements.edit_value"), callback);
            this.type = check.type();
            this.tag = check.tag();
            this.key = check.key();
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            ILocalizedValueCycle<TagType> typeCycle = CommonCycles.cycleOrangeValue("fancymenu.requirements.is_in_tag.type", Arrays.asList(TagType.values()), this.type)
                    .setValueNameSupplier(type -> I18n.get(type.getLocalizationKey()));
            this.addCycleButtonCell(typeCycle, true, (value, button) -> {
                this.type = value;
                this.updateSuggestionLists();
            });

            this.addCellGroupEndSpacerCell();

            String keyString = this.getKeyString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_in_tag.key"));
            this.keyCell = this.addTextInputCell(null, true, true).setText(keyString);

            this.addCellGroupEndSpacerCell();

            String tagString = this.getTagString();
            this.addLabelCell(Component.translatable("fancymenu.requirements.is_in_tag.tag"));
            this.tagCell = this.addTextInputCell(null, true, true).setText(tagString);

            this.keySuggestions = this.createSuggestions(this.keyCell, this.type.getKeySuggestions());
            this.tagSuggestions = this.createSuggestions(this.tagCell, this.type.getTagSuggestions());

            this.addSpacerCell(20);

        }

        @NotNull
        protected EditBoxSuggestions createSuggestions(@NotNull TextInputCell cell, @NotNull List<String> suggestionValues) {
            EditBoxSuggestions suggestions = EditBoxSuggestions.createWithCustomSuggestions(this, cell.editBox, EditBoxSuggestions.SuggestionsRenderPosition.ABOVE_EDIT_BOX, suggestionValues);
            UIBase.applyDefaultWidgetSkinTo(suggestions);
            cell.editBox.setResponder(s -> suggestions.updateCommandInfo());
            return suggestions;
        }

        protected void updateSuggestionLists() {
            this.keySuggestions.setCustomSuggestions(this.type.getKeySuggestions());
            this.keySuggestions.updateCommandInfo();
            this.tagSuggestions.setCustomSuggestions(this.type.getTagSuggestions());
            this.tagSuggestions.updateCommandInfo();
        }

        @Override
        public void renderLateBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            this.keySuggestions.render(graphics, mouseX, mouseY);
            this.tagSuggestions.render(graphics, mouseX, mouseY);
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
            return this.keyPressed(event.key(), event.scancode(), event.modifiers());
        }

        public boolean keyPressed(int $$0, int $$1, int $$2) {
            if (this.keySuggestions.keyPressed($$0, $$1, $$2)) return true;
            if (this.tagSuggestions.keyPressed($$0, $$1, $$2)) return true;
            return super.keyPressed($$0, $$1, $$2);
        }

        @Override
        public boolean mouseScrolled(double $$0, double $$1, double scrollDeltaX, double scrollDeltaY) {
            if (this.keySuggestions.mouseScrolled(scrollDeltaY)) return true;
            if (this.tagSuggestions.mouseScrolled(scrollDeltaY)) return true;
            return super.mouseScrolled($$0, $$1, scrollDeltaX, scrollDeltaY);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
            return this.mouseClicked(event.x(), event.y(), event.button());
        }

        public boolean mouseClicked(double $$0, double $$1, int $$2) {
            if (this.keySuggestions.mouseClicked($$0, $$1, $$2)) return true;
            if (this.tagSuggestions.mouseClicked($$0, $$1, $$2)) return true;
            return super.mouseClicked($$0, $$1, $$2);
        }

        @Override
        public @NotNull String buildString() {
            return new TagCheck(this.type, this.getTagString(), this.getKeyString()).serialize();
        }

        @NotNull
        protected String getKeyString() {
            if (this.keyCell != null) {
                return this.keyCell.getText();
            }
            return this.key;
        }

        @NotNull
        protected String getTagString() {
            if (this.tagCell != null) {
                return this.tagCell.getText();
            }
            return this.tag;
        }

    }

}

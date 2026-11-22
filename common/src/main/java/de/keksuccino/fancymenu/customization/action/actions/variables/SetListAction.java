package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableLists;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Replaces all entries of a list variable, with the parts of a text split at a separator, or with the entries of a JSON array, like the
 * text form of another list. Splitting text is also how text with separators from before list variables existed becomes a real list.
 */
public class SetListAction extends AbstractListAction {

    private static final String LOCALIZATION_KEY = LOCALIZATION_KEY_PREFIX + "set_list";
    /** What a typed-in line break looks like. Fields of action values have one line, so line breaks get typed in as "\n". */
    private static final String ESCAPED_LINE_BREAK = "\\n";

    public SetListAction() {
        this(VARIABLE_HANDLER);
    }

    SetListAction(@NotNull ListVariableAccess variables) {
        super("set_list", variables);
    }

    @Override
    public void execute(@Nullable String value) {
        Config config = Config.parse(value);
        if (config == null) return;
        String listName = Variable.normalizeReferenceName(config.listName);
        if (listName.isEmpty()) return;
        this.logFailedEdit(listName, this.variables.setList(listName, config.buildEntries()));
    }

    @Override
    public @NotNull String getValuePreset() {
        Config config = new Config();
        config.listName = "my_list";
        config.text = "apple, banana, cherry";
        return config.serialize();
    }

    @Override
    public @Nullable Component getValueSummary(@NotNull String value) {
        Config config = Objects.requireNonNull(Config.parse(value));
        String text = quoteForSummary(config.text);
        if (config.source == Source.JSON_ARRAY) return Component.translatable(LOCALIZATION_KEY + ".summary.json_array", config.listName, text);
        if (config.separator.isEmpty() || config.separator.equals(ESCAPED_LINE_BREAK)) return Component.translatable(LOCALIZATION_KEY + ".summary.lines", config.listName, text);
        return Component.translatable(LOCALIZATION_KEY + ".summary.split_text", config.listName, text, quoteForSummary(config.separator));
    }

    @Override
    protected @NotNull ListActionValueWindowBody createValueEditor(@NotNull String value, @NotNull Consumer<String> callback) {
        return new SetListActionValueScreen(value, callback);
    }

    /**
     * Where the new entries of the list come from.
     */
    public enum Source {

        /** The parts of a text, split at a separator. */
        SPLIT_TEXT,
        /** The entries of a JSON array, see {@link VariableLists#fromAssignedText(String)}. */
        JSON_ARRAY;

        /** The name the source gets saved with. Changing it breaks saved layouts. */
        private final String serializedName = this.name().toLowerCase(Locale.ROOT);

        @NotNull
        private static Source parse(@NotNull String serializedName) {
            String trimmed = serializedName.strip();
            for (Source source : values()) {
                if (source.serializedName.equals(trimmed)) return source;
            }
            return SPLIT_TEXT;
        }

    }

    /**
     * What the action does, which is what its value holds. Both sources share the text, so switching the source in the window loses nothing.
     */
    public static final class Config {

        @NotNull
        public String listName = "";
        @NotNull
        public Source source = Source.SPLIT_TEXT;
        /** Plain text, no regex. Empty (or a typed-in "\n") splits the text at its line breaks. */
        @NotNull
        public String separator = ",";
        public boolean trimEntries = true;
        /** If entries that are empty after trimming them get left out. */
        public boolean skipEmptyEntries = true;
        /** The text to split, or the JSON array. */
        @NotNull
        public String text = "";

        /**
         * Returns the entries the list gets, from the text as it is. The action gets its value with all placeholders replaced, so its text
         * is the final one.
         */
        @NotNull
        public List<String> buildEntries() {
            if (this.source == Source.JSON_ARRAY) return VariableLists.fromAssignedText(this.text);
            if (this.separator.isEmpty() || this.separator.equals(ESCAPED_LINE_BREAK)) {
                // Line breaks of placeholders are real ones, typed-in ones are "\n"
                return VariableLists.split(this.text.replace(ESCAPED_LINE_BREAK, "\n"), "", this.trimEntries, this.skipEmptyEntries);
            }
            return VariableLists.split(this.text, this.separator, this.trimEntries, this.skipEmptyEntries);
        }

        /**
         * The separator gets encoded (see {@link #encodeSeparator(String)}), and the text comes last: it is the only part that can hold
         * anything, the separator of the value included.
         */
        @NotNull
        public String serialize() {
            return this.listName + VALUE_SEPARATOR + this.source.serializedName + VALUE_SEPARATOR + encodeSeparator(this.separator) + VALUE_SEPARATOR + this.trimEntries + VALUE_SEPARATOR + this.skipEmptyEntries + VALUE_SEPARATOR + this.text;
        }

        /**
         * Reads a value. Parts a value does not have, or can't be read, keep what a new action starts with.
         */
        @Nullable
        public static Config parse(@Nullable String value) {
            if (value == null) return null;
            String[] parts = splitValue(value, 6);
            Config config = new Config();
            config.listName = parts[0];
            if (parts.length >= 2) config.source = Source.parse(parts[1]);
            if (parts.length >= 3) config.separator = decodeSeparator(parts[2]);
            if (parts.length >= 4) config.trimEntries = parseBoolean(parts[3], config.trimEntries);
            if (parts.length >= 5) config.skipEmptyEntries = parseBoolean(parts[4], config.skipEmptyEntries);
            if (parts.length >= 6) config.text = parts[5];
            return config;
        }

        /**
         * Writes the "%" and "|" characters of the separator as "%25" and "%7C". Separators like "|" are common in text that holds a list,
         * and the parts of a value get told apart by "|||", which they would run into: a part that ends with "|" makes the value end
         * that part one character early.
         */
        @NotNull
        static String encodeSeparator(@NotNull String separator) {
            if ((separator.indexOf('%') < 0) && (separator.indexOf('|') < 0)) return separator;
            StringBuilder builder = new StringBuilder(separator.length() + 8);
            int length = separator.length();
            for (int i = 0; i < length; i++) {
                char c = separator.charAt(i);
                if (c == '%') {
                    builder.append("%25");
                } else if (c == '|') {
                    builder.append("%7C");
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

        /**
         * Reads a separator of {@link #encodeSeparator(String)}. A "%" that starts no code stays as it is.
         */
        @NotNull
        static String decodeSeparator(@NotNull String encoded) {
            if (encoded.indexOf('%') < 0) return encoded;
            StringBuilder builder = new StringBuilder(encoded.length());
            int length = encoded.length();
            for (int i = 0; i < length; i++) {
                char c = encoded.charAt(i);
                if ((c == '%') && encoded.regionMatches(true, i, "%7C", 0, 3)) {
                    builder.append('|');
                    i += 2;
                } else if ((c == '%') && encoded.startsWith("%25", i)) {
                    builder.append('%');
                    i += 2;
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }

    }

    /**
     * The window the value of the action gets edited in. It only shows the inputs the chosen source needs, and a preview of the entries
     * the list would get right now.
     */
    public static class SetListActionValueScreen extends ListActionValueWindowBody {

        private static final int PREVIEW_MAX_ENTRIES = 10;
        private static final int PREVIEW_MAX_LENGTH = 300;

        @NotNull
        protected final Config config;
        @Nullable
        protected HintCell previewHint;

        protected SetListActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable(LOCALIZATION_KEY + ".edit_value"), callback);
            this.config = Objects.requireNonNullElseGet(Config.parse(value), Config::new);
        }

        @Override
        protected void initCells() {

            // The fields get their text before their listeners, so the preview only gets built once, at the end
            this.previewHint = new HintCell();

            this.addStartEndSpacerCell();

            this.addListNameCells(this.config.listName, s -> this.config.listName = s);

            this.addCellGroupEndSpacerCell();

            LocalizedGenericValueCycle<Source> sourceCycle = LocalizedGenericValueCycle.of(LOCALIZATION_KEY + ".edit.source", Source.values());
            sourceCycle.setValueNameSupplier(source -> Component.translatable(LOCALIZATION_KEY + ".edit.source." + source.serializedName).getString());
            sourceCycle.setValueComponentStyleSupplier(source -> Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
            sourceCycle.setCurrentValue(this.config.source);
            CycleButton<Source> sourceButton = new CycleButton<>(0, 0, 20, 20, sourceCycle, (source, button) -> {
                this.config.source = source;
                // Every source has its own inputs
                this.rebuild();
            });
            sourceButton.setUITooltip(UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.source.desc")));
            this.addWidgetCell(sourceButton, true);

            this.addCellGroupEndSpacerCell();

            if (this.config.source == Source.SPLIT_TEXT) {
                this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.text"));
                this.addTextInputCell(null, true, true)
                        .setEditorMultiLineMode(true)
                        .setText(this.config.text)
                        .setEditListener(s -> {
                            this.config.text = s;
                            this.updatePreview();
                        })
                        .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.text.desc")));
                this.addCellGroupEndSpacerCell();
                this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.separator"));
                TextInputCell separatorCell = this.addTextInputCell(null, false, false)
                        .setText(this.config.separator)
                        .setEditListener(s -> {
                            this.config.separator = s;
                            this.updatePreview();
                        })
                        .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.separator.desc")));
                Component emptySeparatorHint = Component.translatable(LOCALIZATION_KEY + ".edit.separator.empty");
                separatorCell.editBox.setHintFancyMenu(consumes -> emptySeparatorHint);
                this.addCellGroupEndSpacerCell();
                this.addToggleCell(".edit.trim_entries", this.config.trimEntries, enabled -> this.config.trimEntries = enabled);
                this.addCellGroupEndSpacerCell();
                this.addToggleCell(".edit.skip_empty_entries", this.config.skipEmptyEntries, enabled -> this.config.skipEmptyEntries = enabled);
                // The preview is about all inputs above, not only the last toggle
                this.addCellGroupEndSpacerCell();
            } else {
                this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.json"));
                this.addTextInputCell(null, true, true)
                        .setText(this.config.text)
                        .setEditListener(s -> {
                            this.config.text = s;
                            this.updatePreview();
                        })
                        .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.json.desc")));
            }

            this.addCell(this.previewHint);
            this.updatePreview();

            this.addStartEndSpacerCell();

        }

        private void addToggleCell(@NotNull String localizationKeySuffix, boolean enabled, @NotNull Consumer<Boolean> listener) {
            CycleButton<CommonCycles.CycleEnabledDisabled> button = new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled(LOCALIZATION_KEY + localizationKeySuffix, enabled), (value, b) -> {
                listener.accept(value.getAsBoolean());
                this.updatePreview();
            });
            button.setUITooltip(UITooltip.of(Component.translatable(LOCALIZATION_KEY + localizationKeySuffix + ".desc")));
            this.addWidgetCell(button, true);
        }

        /**
         * Shows the entries the list would get if the action ran right now, with the placeholders of the value replaced like they get
         * replaced for the action. A JSON array that isn't one gets a warning instead, because its whole text would become the only entry.
         */
        protected void updatePreview() {
            HintCell hint = this.previewHint;
            if (hint == null) return;
            Config resolved = Objects.requireNonNull(Config.parse(PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(this.config.serialize())));
            if ((resolved.source == Source.JSON_ARRAY) && !resolved.text.isBlank() && (VariableLists.parseText(resolved.text) == null)) {
                hint.setHint(Component.translatable(LOCALIZATION_KEY + ".edit.json.not_an_array"), true);
                return;
            }
            List<String> entries = resolved.buildEntries();
            String preview = VariableLists.toText((entries.size() > PREVIEW_MAX_ENTRIES) ? entries.subList(0, PREVIEW_MAX_ENTRIES) : entries);
            if (entries.size() > PREVIEW_MAX_ENTRIES) preview = preview.substring(0, preview.length() - 1) + ",…]";
            if (preview.length() > PREVIEW_MAX_LENGTH) preview = preview.substring(0, PREVIEW_MAX_LENGTH) + "…";
            hint.setHint(Component.translatable(LOCALIZATION_KEY + ".edit.preview", entries.size(), preview), false);
        }

        @Override
        public boolean allowDone() {
            return !this.config.listName.isBlank();
        }

        @Override
        protected @NotNull String serialize() {
            return this.config.serialize();
        }

    }

}

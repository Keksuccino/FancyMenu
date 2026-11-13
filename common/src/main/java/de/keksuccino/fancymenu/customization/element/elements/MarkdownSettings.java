package de.keksuccino.fancymenu.customization.element.elements;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.properties.PropertyHolder;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.BooleanOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.ColorOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.EnumOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.FloatOption;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyleOption.FontOption;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Markdown settings of an element that shows its text with a {@link MarkdownRenderer}: the scale of the text, if the text gets read as Markdown,
 * which optional Markdown syntax it has, the look that got applied last, and one setting for every option of the {@link MarkdownStyle} of the renderer.<br>
 * All of them are properties of the element, so they get saved, loaded and stacked like its other properties,
 * and their numbers, switches and colors take placeholders.<br><br>
 *
 * The properties are what the element saves, and the renderer is what draws. {@link #tick()} carries the values over: all of them after
 * a property got set, and from then on only the ones that come from placeholders, because only those change without getting set.<br><br>
 *
 * A look is nothing but a value for every style property, see {@link #applyLook(MarkdownLook)}. Every style property gets saved with its own value,
 * so an element keeps its appearance when a later version of the mod gives a look other values.
 * A property that is missing in saved data has the value of the classic look, which is how text looked before it had a style.
 */
public final class MarkdownSettings {

    /** The start of the property keys of all style options. Changing it breaks saved layouts. */
    public static final String STYLE_KEY_PREFIX = "markdown_style_";
    /** The property key of the look, which every element has that got saved with its style. Changing it breaks saved layouts. */
    public static final String LOOK_KEY = "markdown_look";
    private static final String LOCALIZATION_KEY_BASE = "fancymenu.elements.text.markdown.";
    private static final String STYLE_LOCALIZATION_KEY_BASE = LOCALIZATION_KEY_BASE + "style.";
    /** The types of callouts whose options are named after them, like "callout_note_color". */
    private static final String[] CALLOUT_TYPES = {"note", "tip", "important", "warning", "caution"};
    /** The arrows of the number picker change factors, like the size of a headline, by this. */
    private static final Property.NumericInputBehavior<Float> FACTOR_INPUT = Property.NumericInputBehavior.<Float>builder().freeInput().step(0.1F).build();
    /** The arrows of the number picker change the thickness of lines by this, because half a pixel is a usual thickness. */
    private static final Property.NumericInputBehavior<Float> THICKNESS_INPUT = Property.NumericInputBehavior.<Float>builder().freeInput().step(0.5F).build();
    /** The names of all style options by their key. They are the same for every element, so they get built only once. */
    private static final Map<String, OptionNames> OPTION_NAMES = new ConcurrentHashMap<>();
    private static final Map<Enum<?>, String> VALUE_LOCALIZATION_KEYS = new ConcurrentHashMap<>();
    private static final Runnable[] NO_DYNAMIC_VALUES = new Runnable[0];

    private final PropertyHolder holder;
    private final MarkdownStyle style;
    private final List<Binding<?>> bindings = new ArrayList<>(176);
    /** Carry over the values of the properties that come from placeholders right now. */
    private Runnable[] dynamicValues = NO_DYNAMIC_VALUES;
    /** A property got set since the renderer got all values the last time. A new renderer has none of them yet. */
    private boolean outdated = true;
    /** One listener for all properties, instead of one for each of them. */
    private final Property.ValueSetListener<Object> changeListener = (oldValue, newValue) -> this.outdated = true;
    @Nullable
    private Consumer<MarkdownLook> lookListener;

    public final Property.FloatProperty textScale;
    public final Property.BooleanProperty parseMarkdown;
    /** If "+ ", "* " and "~ " start list items like "- " does. */
    public final Property.BooleanProperty extendedListMarkers;
    public final Property.BooleanProperty numberedLists;
    public final Property.BooleanProperty callouts;
    /** The look that got applied last. It only tells where the values of the style properties came from, the values themselves are their own. */
    public final Property<MarkdownLook> look;

    /**
     * @param holder The element that owns the settings.
     * @param renderer The renderer of the element, with a style nobody changed yet.
     */
    public MarkdownSettings(@NotNull PropertyHolder holder, @NotNull MarkdownRenderer renderer) {
        this.holder = Objects.requireNonNull(holder);
        this.style = renderer.getStyle();
        Property.FloatProperty textScale = Property.floatProperty("scale", 1.0F, "fancymenu.elements.text.scale");
        this.textScale = this.bind(textScale, () -> renderer.setTextBaseScale(Math.max(0.0F, textScale.getFloat())), null);
        Property.BooleanProperty parseMarkdown = Property.booleanProperty("parse_markdown", true, LOCALIZATION_KEY_BASE + "toggle");
        this.parseMarkdown = this.bind(parseMarkdown, () -> renderer.setParseMarkdown(parseMarkdown.getBoolean()), null);
        this.look = this.bind(Property.enumProperty(LOOK_KEY, MarkdownLook.CLASSIC, LOCALIZATION_KEY_BASE + "look", MarkdownSettings::getValueLocalizationKey), null, null);
        Property.BooleanProperty extendedListMarkers = Property.booleanProperty("markdown_extended_list_markers", false, LOCALIZATION_KEY_BASE + "extended_list_markers");
        this.extendedListMarkers = this.bind(extendedListMarkers, () -> renderer.setExtendedBulletListMarkersEnabled(extendedListMarkers.getBoolean()), null);
        Property.BooleanProperty numberedLists = Property.booleanProperty("markdown_numbered_lists", false, LOCALIZATION_KEY_BASE + "numbered_lists");
        this.numberedLists = this.bind(numberedLists, () -> renderer.setNumberedListsEnabled(numberedLists.getBoolean()), null);
        Property.BooleanProperty callouts = Property.booleanProperty("markdown_callouts", false, LOCALIZATION_KEY_BASE + "callouts");
        this.callouts = this.bind(callouts, () -> renderer.setCalloutsEnabled(callouts.getBoolean()), null);
        List<MarkdownStyleOption<?>> options = this.style.getOptions();
        for (int i = 0; i < options.size(); i++) {
            this.bindOption(options.get(i));
        }
    }

    /**
     * Gives the renderer the values of the properties. Has to run before the element draws or measures its text.<br>
     * Costs nothing while no property got set and no property has placeholders.
     */
    public void tick() {
        if (this.outdated) {
            this.outdated = false;
            int dynamicCount = 0;
            for (int i = 0; i < this.bindings.size(); i++) {
                Binding<?> binding = this.bindings.get(i);
                if (binding.apply == null) continue;
                binding.apply.run();
                if (binding.isDynamic()) dynamicCount++;
            }
            this.dynamicValues = this.collectDynamicValues(dynamicCount);
        } else {
            for (Runnable dynamicValue : this.dynamicValues) {
                dynamicValue.run();
            }
        }
    }

    @NotNull
    private Runnable[] collectDynamicValues(int count) {
        if (count == 0) return NO_DYNAMIC_VALUES;
        Runnable[] dynamicValues = new Runnable[count];
        int index = 0;
        for (int i = 0; (i < this.bindings.size()) && (index < count); i++) {
            Binding<?> binding = this.bindings.get(i);
            if ((binding.apply != null) && binding.isDynamic()) dynamicValues[index++] = binding.apply;
        }
        return dynamicValues;
    }

    /**
     * Gives every style property the value the given look has for it, and makes that value the one the property resets to.<br>
     * Colors that follow the UI theme in that look get the color the theme has right now, so the element keeps it when the theme changes.
     */
    public void applyLook(@NotNull MarkdownLook look) {
        this.look.set(Objects.requireNonNull(look));
        this.adoptLook(look, true);
        if (this.lookListener != null) this.lookListener.accept(look);
    }

    /**
     * Makes the values of the look of the element the ones its style properties reset to. Without it, they reset to the values of the classic look.<br>
     * Only editors need it, right before they offer to reset properties. Must not run before the properties got loaded,
     * because a property that is missing in saved data becomes the value it resets to, which has to be the one of the classic look.
     */
    public void updateResetValues() {
        this.adoptLook(this.getLook(), false);
    }

    private void adoptLook(@NotNull MarkdownLook look, boolean setValues) {
        for (int i = 0; i < this.bindings.size(); i++) {
            this.bindings.get(i).adoptLook(look, setValues);
        }
    }

    /**
     * Turns the element into what a new element is: one in the pixel look that reads all optional Markdown syntax.
     */
    public void applyDefaultsOfNewElements() {
        this.applyLook(MarkdownLook.PIXEL);
        this.extendedListMarkers.set(true);
        this.numberedLists.set(true);
        this.callouts.set(true);
    }

    @NotNull
    public MarkdownLook getLook() {
        return this.look.tryGetNonNullElse(MarkdownLook.CLASSIC);
    }

    /**
     * Sets who gets told when a look gets applied, for settings of the element that belong to a look without being part of the style.
     */
    public void setLookListener(@Nullable Consumer<MarkdownLook> lookListener) {
        this.lookListener = lookListener;
    }

    @NotNull
    public MarkdownStyle getStyle() {
        return this.style;
    }

    /**
     * Returns the property of the given style option.
     */
    @NotNull
    public Property<?> getProperty(@NotNull MarkdownStyleOption<?> option) {
        return Objects.requireNonNull(this.holder.getProperty(getPropertyKey(option)));
    }

    /**
     * Returns the key of the property of the given style option, which is the same for every element.
     */
    @NotNull
    public static String getPropertyKey(@NotNull MarkdownStyleOption<?> option) {
        return OptionNames.of(option).propertyKey;
    }

    /**
     * Returns the localization key of the name of a value of a style option, like a marker shape, or of a look.
     */
    @NotNull
    public static String getValueLocalizationKey(@NotNull Enum<?> value) {
        String key = VALUE_LOCALIZATION_KEYS.get(value);
        if (key == null) {
            // "MarkdownMarkerShape.CURSOR_SMALL" becomes "marker_shape.cursor_small"
            String type = value.getDeclaringClass().getSimpleName().replaceFirst("^Markdown", "").replaceAll("(?<=[a-z])(?=[A-Z])", "_").toLowerCase(Locale.ROOT);
            key = STYLE_LOCALIZATION_KEY_BASE + "value." + type + "." + value.name().toLowerCase(Locale.ROOT);
            VALUE_LOCALIZATION_KEYS.put(value, key);
        }
        return key;
    }

    private void bindOption(@NotNull MarkdownStyleOption<?> option) {
        OptionNames names = OptionNames.of(option);
        switch (option.getType()) {
            case BOOLEAN: {
                BooleanOption booleanOption = (BooleanOption) option;
                Property.BooleanProperty property = Property.booleanProperty(names.propertyKey, booleanOption.getLook(MarkdownLook.CLASSIC), names.localizationKey);
                this.bind(property, () -> booleanOption.set(property.getBoolean()), booleanOption::getLookValue);
                break;
            }
            case FLOAT: {
                FloatOption floatOption = (FloatOption) option;
                Property.FloatProperty property = Property.floatProperty(names.propertyKey, floatOption.getLook(MarkdownLook.CLASSIC), names.localizationKey, names.inputBehavior);
                float minimum = floatOption.getMinimum();
                // Keeps the value of the property the one the renderer uses. Values of placeholders skip this, and the option holds them back itself.
                if (minimum != Float.NEGATIVE_INFINITY) property.setValueSetProcessor(value -> Math.max(minimum, value));
                this.bind(property, () -> {
                    float value = property.getFloat();
                    // "NaN" and "Infinity" are numbers to the parser of the property, and no layout survives them
                    if (Float.isFinite(value)) floatOption.set(value);
                }, floatOption::getLookValue);
                break;
            }
            case COLOR: {
                ColorOption colorOption = (ColorOption) option;
                Property.ColorProperty property = colorOption.isOptional()
                        ? Property.optionalHexColorProperty(names.propertyKey, names.getClassicColor(colorOption), true, names.localizationKey)
                        : Property.hexColorProperty(names.propertyKey, names.getClassicColor(colorOption), true, names.localizationKey);
                this.bind(property, new ColorValue(property, colorOption), look -> toPropertyValue(colorOption.getLookValue(look)));
                break;
            }
            case ENUM: {
                this.bindEnumOption((EnumOption<?>) option, names);
                break;
            }
            case FONT: {
                FontOption fontOption = (FontOption) option;
                Property.StringProperty property = Property.stringProperty(names.propertyKey, toPropertyValue(fontOption.getLookValue(MarkdownLook.CLASSIC)), false, false, names.localizationKey);
                this.bind(property, new FontValue(property, fontOption), look -> toPropertyValue(fontOption.getLookValue(look)));
                break;
            }
        }
    }

    private <E extends Enum<E>> void bindEnumOption(@NotNull EnumOption<E> option, @NotNull OptionNames names) {
        Property<E> property = Property.enumProperty(names.propertyKey, option.getLookValue(MarkdownLook.CLASSIC), names.localizationKey, MarkdownSettings::getValueLocalizationKey);
        this.bind(property, () -> {
            E value = property.get();
            if (value != null) option.set(value);
        }, option::getLookValue);
    }

    /**
     * Makes the given property one of the element and connects it with the renderer.
     *
     * @param apply Gives the renderer the value of the property. NULL if the renderer has nothing to do with the property.
     * @param lookValue Returns the value a look gives the property. NULL if the property is no part of the style.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private <T, P extends Property<T>> P bind(@NotNull P property, @Nullable Runnable apply, @Nullable Function<MarkdownLook, T> lookValue) {
        this.bindings.add(new Binding<>(property, apply, lookValue));
        // The listener only notes that something changed, so it works for the values of every type
        property.addValueSetListener((Property.ValueSetListener<T>) this.changeListener);
        return this.holder.putProperty(property);
    }

    /** A color as the value of a color property. A property of an optional color without a color has {@link Property.ColorProperty#NO_COLOR}. */
    @NotNull
    private static String toPropertyValue(@Nullable DrawableColor color) {
        return (color != null) ? color.getHex() : Property.ColorProperty.NO_COLOR;
    }

    /** A font as the value of a font property, which is empty for no font. */
    @NotNull
    private static String toPropertyValue(@Nullable ResourceLocation font) {
        return (font != null) ? font.toString() : "";
    }

    /**
     * One property with what connects it to the renderer and to the looks.
     */
    private static final class Binding<T> {

        private final Property<T> property;
        @Nullable
        private final Runnable apply;
        @Nullable
        private final Function<MarkdownLook, T> lookValue;

        private Binding(@NotNull Property<T> property, @Nullable Runnable apply, @Nullable Function<MarkdownLook, T> lookValue) {
            this.property = property;
            this.apply = apply;
            this.lookValue = lookValue;
        }

        /**
         * Returns if the value of the property comes from placeholders, which makes it change without the property getting set.
         */
        private boolean isDynamic() {
            if (this.property instanceof Property.ManualInputProperty<?> manualInputProperty) return PlaceholderParser.containsPlaceholders(manualInputProperty.getManualInput());
            return (this.property.get() instanceof String string) && PlaceholderParser.containsPlaceholders(string);
        }

        private void adoptLook(@NotNull MarkdownLook look, boolean setValue) {
            if (this.lookValue == null) return;
            T value = this.lookValue.apply(look);
            this.property.setDefault(value);
            if (setValue) this.property.set(value);
        }

    }

    /**
     * Gives a color option the color of its property. Reading a color out of text is work, so it only happens when the text changed.
     */
    private static final class ColorValue implements Runnable {

        private final Property.ColorProperty property;
        private final ColorOption option;
        @Nullable
        private String lastText;

        private ColorValue(@NotNull Property.ColorProperty property, @NotNull ColorOption option) {
            this.property = property;
            this.option = option;
        }

        @Override
        public void run() {
            String text = this.property.get();
            if (text == null) text = Property.ColorProperty.NO_COLOR;
            if (PlaceholderParser.containsPlaceholders(text)) text = PlaceholderParser.replacePlaceholders(text);
            if (text.equals(this.lastText)) return;
            this.lastText = text;
            // Text that is no color becomes white, like it does everywhere colors get read from text
            this.option.set((this.option.isOptional() && text.isBlank()) ? null : DrawableColor.of(text));
        }

    }

    /**
     * Gives a font option the font of its property. Text that is no identifier of a font means no font.
     */
    private static final class FontValue implements Runnable {

        private final Property.StringProperty property;
        private final FontOption option;
        @Nullable
        private String lastText;
        private boolean applied = false;

        private FontValue(@NotNull Property.StringProperty property, @NotNull FontOption option) {
            this.property = property;
            this.option = option;
        }

        @Override
        public void run() {
            String text = this.property.get();
            if (this.applied && Objects.equals(text, this.lastText)) return;
            this.applied = true;
            this.lastText = text;
            this.option.set(((text == null) || text.isBlank()) ? null : ResourceLocation.tryParse(text.trim()));
        }

    }

    /**
     * The names of one style option, which are the same for all elements.
     */
    private static final class OptionNames {

        private final String propertyKey;
        private final String localizationKey;
        @Nullable
        private final Property.NumericInputBehavior<Float> inputBehavior;
        /** The color of the classic look as value of a property, built when the first element needs it. Only color options have it. */
        @Nullable
        private volatile String classicColor;

        private OptionNames(@NotNull String optionKey) {
            this.propertyKey = STYLE_KEY_PREFIX + optionKey;
            this.localizationKey = STYLE_LOCALIZATION_KEY_BASE + getSharedName(optionKey);
            if (optionKey.startsWith("headline_") && optionKey.endsWith("_scale")) {
                this.inputBehavior = FACTOR_INPUT;
            } else if (optionKey.endsWith("_thickness")) {
                this.inputBehavior = THICKNESS_INPUT;
            } else {
                this.inputBehavior = null;
            }
        }

        @NotNull
        private static OptionNames of(@NotNull MarkdownStyleOption<?> option) {
            OptionNames names = OPTION_NAMES.get(option.getKey());
            if (names == null) {
                names = new OptionNames(option.getKey());
                OPTION_NAMES.put(option.getKey(), names);
            }
            return names;
        }

        @NotNull
        private String getClassicColor(@NotNull ColorOption option) {
            String color = this.classicColor;
            if (color == null) {
                color = toPropertyValue(option.getLookValue(MarkdownLook.CLASSIC));
                this.classicColor = color;
            }
            return color;
        }

        /**
         * Returns the name the option shares with the same option of the other headline levels, list levels or callout types,
         * because those options mean the same for every level and type: "headline_2_color" becomes "headline.color".
         * Options that exist only once keep their key as name.
         */
        @NotNull
        private static String getSharedName(@NotNull String optionKey) {
            if (optionKey.startsWith("headline_") && isLevel(optionKey, 9)) return "headline." + optionKey.substring(11);
            if (optionKey.startsWith("list_level_") && isLevel(optionKey, 11)) return "list_level." + optionKey.substring(13);
            if (optionKey.startsWith("callout_")) {
                for (String type : CALLOUT_TYPES) {
                    if (optionKey.startsWith(type + "_", 8)) return "callout_type." + optionKey.substring(9 + type.length());
                }
            }
            return optionKey;
        }

        private static boolean isLevel(@NotNull String optionKey, int index) {
            return (optionKey.length() > (index + 1)) && Character.isDigit(optionKey.charAt(index)) && (optionKey.charAt(index + 1) == '_');
        }

    }

}

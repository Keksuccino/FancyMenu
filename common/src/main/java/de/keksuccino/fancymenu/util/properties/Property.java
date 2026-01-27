package de.keksuccino.fancymenu.util.properties;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorWindowBody;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.Pair;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ColorPickerWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NumberPickerWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import de.keksuccino.fancymenu.FancyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A typed, serializable property identified by a unique {@link #getKey() key}.
 * <p>
 * A {@link Property} stores a {@link #getDefault() default value} and a mutable {@link #get() current value}.
 * It is typically owned by a {@link PropertyHolder} and persisted via a {@link PropertyContainer}.
 * The class also integrates with {@link ContextMenuBuilder} to build rich, stack-aware UI entries.
 * </p>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * Property<String> title = Property.stringProperty(
 *     "title",
 *     "Hello",
 *     false,     // multiLine
 *     true,      // placeholders
 *     "fancymenu.menu.entry.title"
 * );
 *
 * holder.putProperty(title);
 * title.set("New Title");
 *
 * PropertyContainer container = new PropertyContainer("my_type");
 * title.serialize(container);
 * }</pre>
 *
 * <h3>Context menu integration and stacking</h3>
 * Each property can provide a {@link ContextMenuEntrySupplier} that builds a
 * {@link ContextMenu.ClickableContextMenuEntry}. The static factory methods in this class already wire
 * entries using {@link ContextMenuBuilder}'s stack-aware helpers, which means multi-selection in UIs
 * works automatically (all selected objects are updated when the user changes the value).
 * <p>
 * If you provide a custom {@link #setContextMenuEntrySupplier(ContextMenuEntrySupplier) supplier}, prefer
 * using the stack-aware builder helpers (for example
 * {@link ContextMenuBuilder#buildGenericToggleContextMenuEntry}) so the entry behaves correctly in
 * stacked/multi-select context menus.
 * </p>
 *
 * @param <T> the property value type
 */
@SuppressWarnings({"unchecked", "unused"})
public class Property<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    protected final String key;
    @Nullable
    protected T defaultValue;
    @Nullable
    protected T currentValue;
    @Nullable
    protected ConsumingSupplier<String, T> deserializationCodec;
    @Nullable
    protected ConsumingSupplier<T, String> serializationCodec = consumes -> {
        if (consumes == null) return null;
        return consumes.toString();
    };
    @Nullable
    protected ContextMenuEntrySupplier<? extends PropertyHolder, T> contextMenuEntrySupplier;
    @NotNull
    protected String contextMenuEntryLocalizationKeyBase;
    @Nullable
    protected ConsumingSupplier<String, Boolean> userInputTextValidator = null;
    @Nullable
    protected ConsumingSupplier<T, T> valueSetProcessor = null;
    @Nullable
    protected ConsumingSupplier<T, T> valueGetProcessor = null;
    protected final List<ValueSetListener<T>> valueSetListeners = new ArrayList<>();
    protected boolean disabled = false;

    /**
     * Creates a string {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes as a raw string and uses a generic text input entry in
     * context menus. The entry is stack-aware, so editing a multi-selection updates all selected
     * objects that contain the property.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * StringProperty label = Property.stringProperty(
     *     "label",
     *     "Button",
     *     "Button",
     *     false, // multiLine
     *     true,  // placeholders
     *     "fancymenu.menu.entry.label"
     * );
     *
     * String s = label.getString(); // ready-to-use with placeholders replaced already
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value (can be {@code null})
     * @param multiLine whether the input dialog supports line breaks
     * @param placeholders whether placeholder tokens are allowed in input
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured string property
     */
    @NotNull
    public static StringProperty stringProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue, boolean multiLine, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        StringProperty p = new StringProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = consumes -> consumes;
        p.contextMenuEntrySupplier = (property, builder, menu) -> builder.buildGenericStringInputContextMenuEntry(menu, "menu_entry_" + key,
                consumes -> consumes.getProperty(key) != null,
                consumes -> {
                    StringProperty resolved = (StringProperty) consumes.getProperty(key);
                    if (resolved != null) return resolved.get();
                    return defaultValue;
                },
                (b, s) -> {
                    StringProperty resolved = (StringProperty) b.getProperty(key);
                    if (resolved != null) resolved.set(s);
                },
                null, multiLine, placeholders, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, property.getDefault(), property.userInputTextValidator, null)
                .setIcon(MaterialIcons.TEXT_FIELDS);
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #stringProperty(String, String, String, boolean, boolean, String)
     */
    @NotNull
    public static StringProperty stringProperty(@NotNull String key, @Nullable String defaultValue, boolean multiLine, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return stringProperty(key, defaultValue, defaultValue, multiLine, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    public static final class NumericInputBehavior<N extends Number> {

        public enum Mode {
            FREE_INPUT,
            RANGE_INPUT,
            CYCLE_INPUT
        }

        @NotNull
        private final Mode mode;
        @Nullable
        private final N minValue;
        @Nullable
        private final N maxValue;
        @Nullable
        private final List<N> cycleValues;

        private NumericInputBehavior(@NotNull Mode mode, @Nullable N minValue, @Nullable N maxValue, @Nullable List<N> cycleValues) {
            this.mode = mode;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.cycleValues = (cycleValues != null) ? List.copyOf(cycleValues) : null;
        }

        @NotNull
        public Mode getMode() {
            return this.mode;
        }

        @Nullable
        public N getMinValue() {
            return this.minValue;
        }

        @Nullable
        public N getMaxValue() {
            return this.maxValue;
        }

        @Nullable
        public List<N> getCycleValues() {
            return this.cycleValues;
        }

        @NotNull
        public static <N extends Number> Builder<N> builder() {
            return new Builder<>();
        }

        public static final class Builder<N extends Number> {

            @NotNull
            private Mode mode = Mode.FREE_INPUT;
            @Nullable
            private N minValue;
            @Nullable
            private N maxValue;
            @Nullable
            private List<N> cycleValues;

            @NotNull
            public Builder<N> freeInput() {
                this.mode = Mode.FREE_INPUT;
                this.minValue = null;
                this.maxValue = null;
                this.cycleValues = null;
                return this;
            }

            @NotNull
            public Builder<N> rangeInput(@NotNull N minValue, @NotNull N maxValue) {
                this.mode = Mode.RANGE_INPUT;
                this.minValue = minValue;
                this.maxValue = maxValue;
                this.cycleValues = null;
                return this;
            }

            @NotNull
            public Builder<N> cycleInput(@NotNull List<N> cycleValues) {
                this.mode = Mode.CYCLE_INPUT;
                this.cycleValues = new ArrayList<>(cycleValues);
                this.minValue = null;
                this.maxValue = null;
                return this;
            }

            @NotNull
            public NumericInputBehavior<N> build() {
                return new NumericInputBehavior<>(this.mode, this.minValue, this.maxValue, this.cycleValues);
            }
        }
    }

    @NotNull
    private static <N extends Number> NumericInputBehavior<N> resolveNumericInputBehavior(@Nullable NumericInputBehavior<N> behavior) {
        if (behavior != null) return behavior;
        return NumericInputBehavior.<N>builder().freeInput().build();
    }

    private static <N extends Number> void restoreNumericSnapshots(@NotNull List<? extends PropertyHolder> selectedObjects, @NotNull List<Pair<String, N>> snapshots, @NotNull String key) {
        for (int i = 0; i < selectedObjects.size(); i++) {
            PropertyHolder holder = selectedObjects.get(i);
            ManualInputProperty<N> resolved = (ManualInputProperty<N>) holder.getProperty(key);
            if (resolved == null) continue;
            Pair<String, N> snapshot = snapshots.get(i);
            resolved.restoreSnapshot(snapshot.getFirst(), snapshot.getSecond());
        }
    }

    private static <N extends Number> void openNumberPickerWindow(@NotNull ManualInputProperty<N> property, @NotNull ContextMenuBuilder<? extends PropertyHolder> builder, @NotNull ContextMenu contextMenu, @NotNull ContextMenu.ClickableContextMenuEntry<?> entry, @NotNull String key, @Nullable NumericInputBehavior<N> behavior, @NotNull CharacterFilter inputFilter, @NotNull NumberPickerWindowBody.ValueAdapter<N> adapter, @NotNull N fallbackValue) {
        ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
        if (!stack.isPrimary() || stack.isEmpty()) {
            return;
        }
        List<? extends PropertyHolder> selectedObjects = stack.getObjects();
        List<Pair<String, N>> originalValues = new ArrayList<>();
        List<N> targetValuesOfSelected = new ArrayList<>();
        for (PropertyHolder holder : selectedObjects) {
            ManualInputProperty<N> resolved = (ManualInputProperty<N>) holder.getProperty(key);
            String manualInput = (resolved != null) ? resolved.getManualInput() : null;
            N rawValue = (resolved != null) ? resolved.currentValue : null;
            N value = (resolved != null) ? resolved.get() : null;
            N resolvedValue = (value != null) ? value : property.getDefault();
            if (resolvedValue == null) resolvedValue = fallbackValue;
            originalValues.add(Pair.of(manualInput, rawValue));
            targetValuesOfSelected.add(resolvedValue);
        }
        N presetValue;
        if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
            ManualInputProperty<N> resolved = (ManualInputProperty<N>) builder.self().getProperty(key);
            presetValue = (resolved != null) ? resolved.get() : property.getDefault();
        } else {
            presetValue = property.getDefault();
        }
        if (presetValue == null) presetValue = fallbackValue;

        NumericInputBehavior<N> resolvedBehavior = resolveNumericInputBehavior(behavior);
        NumberPickerWindowBody.InputMode inputMode = NumberPickerWindowBody.InputMode.FREE_INPUT;
        N minValue = null;
        N maxValue = null;
        List<N> cycleValues = null;
        switch (resolvedBehavior.getMode()) {
            case RANGE_INPUT -> {
                minValue = resolvedBehavior.getMinValue();
                maxValue = resolvedBehavior.getMaxValue();
                if (minValue != null && maxValue != null) {
                    inputMode = NumberPickerWindowBody.InputMode.RANGE_INPUT;
                }
            }
            case CYCLE_INPUT -> {
                cycleValues = resolvedBehavior.getCycleValues();
                if (cycleValues != null && !cycleValues.isEmpty()) {
                    inputMode = NumberPickerWindowBody.InputMode.CYCLE_INPUT;
                }
            }
            default -> {
            }
        }

        NumberPickerWindowBody<N> picker = new NumberPickerWindowBody<>(inputMode, minValue, maxValue, cycleValues, presetValue, inputFilter, adapter,
                value -> builder.applyStackAppliers(entry, value),
                value -> {
                    builder.saveSnapshot();
                    builder.applyStackAppliers(entry, value);
                },
                value -> restoreNumericSnapshots(selectedObjects, originalValues, key));

        contextMenu.closeMenuChain();
        PiPWindow window = new PiPWindow(Component.translatable(property.getContextMenuEntryLocalizationKeyBase()))
                .setScreen(picker)
                .setForceFancyMenuUiScale(true)
                .setMinSize(NumberPickerWindowBody.PIP_WINDOW_WIDTH, NumberPickerWindowBody.PIP_WINDOW_HEIGHT)
                .setSize(NumberPickerWindowBody.PIP_WINDOW_WIDTH, NumberPickerWindowBody.PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
    }

    /**
     * Creates an integer {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Integer#valueOf(String)} and uses a custom
     * number picker plus plain text entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * IntegerProperty width = Property.integerProperty(
     *     "width",
     *     100,
     *     100,
     *     "fancymenu.menu.entry.width"
     * );
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured integer property
     */
    @NotNull
    public static IntegerProperty integerProperty(@NotNull String key, int defaultValue, int currentValue, @NotNull String contextMenuEntryLocalizationKeyBase, @Nullable NumericInputBehavior<Integer> inputBehavior) {
        IntegerProperty p = new IntegerProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Integer::valueOf;
        CharacterFilter inputFilter = CharacterFilter.buildIntegerFilter();
        NumberPickerWindowBody.ValueAdapter<Integer> adapter = new NumberPickerWindowBody.ValueAdapter<>() {
            @Override
            public @Nullable Integer parseInput(@NotNull String text) {
                if (MathUtils.isInteger(text)) return Integer.parseInt(text);
                return null;
            }

            @Override
            public @NotNull String formatValue(@Nullable Integer value) {
                return (value != null) ? String.valueOf(value) : "";
            }

            @Override
            public double toSliderValue(@NotNull Integer value) {
                return value.doubleValue();
            }

            @Override
            public @NotNull Integer fromSliderValue(double sliderValue) {
                return (int)Math.round(sliderValue);
            }

            @Override
            public boolean showAsInteger() {
                return true;
            }

            @Override
            public int getRoundingDecimalPlaces() {
                return 0;
            }
        };
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            ContextMenu subMenu = new ContextMenu();
            ContextMenu.StackApplier stackApplier = (stackEntry, value) -> {
                IntegerProperty resolved = (IntegerProperty) builder.self().getProperty(key);
                if (resolved == null) return;
                if (value instanceof String stringValue) {
                    resolved.setManualInput(stringValue);
                } else if (value instanceof Number numberValue) {
                    resolved.set(numberValue.intValue());
                } else if (value == null) {
                    resolved.set(property.getDefault());
                }
            };

            subMenu.addClickableEntry("input_via_number_picker", Component.translatable("fancymenu.context_menu.entries.number.input_via_number_picker"),
                            (contextMenu, entry) -> openNumberPickerWindow((ManualInputProperty<Integer>) property, builder, contextMenu, entry, key, inputBehavior, inputFilter, adapter, defaultValue))
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.SLIDERS);

            subMenu.addClickableEntry("input_as_string", Component.translatable("fancymenu.context_menu.entries.number.input_as_string"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                List<? extends PropertyHolder> selectedObjects = stack.getObjects();
                                String defaultText = null;
                                List<String> targetValuesOfSelected = new ArrayList<>();
                                for (PropertyHolder holder : selectedObjects) {
                                    IntegerProperty resolved = (IntegerProperty) holder.getProperty(key);
                                    String value = (resolved != null) ? resolved.getRawInputOrFormattedValue() : null;
                                    targetValuesOfSelected.add(value);
                                }
                                if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                                    IntegerProperty resolved = (IntegerProperty) builder.self().getProperty(key);
                                    defaultText = (resolved != null) ? resolved.getRawInputOrFormattedValue() : String.valueOf(defaultValue);
                                }
                                TextEditorWindowBody s = new TextEditorWindowBody(Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null, call -> {
                                    if (call != null) {
                                        builder.saveSnapshot();
                                        builder.applyStackAppliers(entry, call);
                                    }
                                });
                                if (property.userInputTextValidator != null) {
                                    s.setTextValidator(consumes -> property.userInputTextValidator.get(consumes.getText()));
                                }
                                s.setText(defaultText);
                                s.setMultilineMode(false);
                                s.setPlaceholdersAllowed(true);
                                contextMenu.closeMenuChain();
                                Dialogs.openGeneric(s, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null,
                                        TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                                        .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                            })
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.TEXT_FIELDS);

            subMenu.addSeparatorEntry("separator_before_reset");

            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.common_components.reset"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                builder.saveSnapshot();
                                builder.applyStackAppliers(entry, null);
                            })
                    .setStackable(true)
                    .setStackApplier((stackEntry, value) -> {
                        IntegerProperty resolved = (IntegerProperty) builder.self().getProperty(key);
                        if (resolved != null) resolved.set(property.getDefault());
                    })
                    .setIcon(MaterialIcons.UNDO);

            return new ContextMenu.SubMenuContextMenuEntry("menu_entry_" + key, menu, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), subMenu)
                    .setIcon(MaterialIcons.NUMBERS)
                    .setStackable(true);
        };
        return p;
    }

    /**
     * Convenience overload that defaults to free input behavior.
     *
     * @see #integerProperty(String, int, int, String, NumericInputBehavior)
     */
    @NotNull
    public static IntegerProperty integerProperty(@NotNull String key, int defaultValue, int currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return integerProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase, null);
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #integerProperty(String, int, int, String, NumericInputBehavior)
     */
    @NotNull
    public static IntegerProperty integerProperty(@NotNull String key, int defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase, @Nullable NumericInputBehavior<Integer> inputBehavior) {
        return integerProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, inputBehavior);
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #integerProperty(String, int, int, String)
     */
    @NotNull
    public static IntegerProperty integerProperty(@NotNull String key, int defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return integerProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, null);
    }

    /**
     * Creates a double {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Double#valueOf(String)} and uses a custom
     * number picker plus plain text entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * DoubleProperty opacity = Property.doubleProperty(
     *     "opacity",
     *     1.0,
     *     0.8,
     *     "fancymenu.menu.entry.opacity"
     * );
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured double property
     */
    @NotNull
    public static DoubleProperty doubleProperty(@NotNull String key, double defaultValue, double currentValue, @NotNull String contextMenuEntryLocalizationKeyBase, @Nullable NumericInputBehavior<Double> inputBehavior) {
        DoubleProperty p = new DoubleProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Double::valueOf;
        CharacterFilter inputFilter = CharacterFilter.buildDecimalFiler();
        NumberPickerWindowBody.ValueAdapter<Double> adapter = new NumberPickerWindowBody.ValueAdapter<>() {
            @Override
            public @Nullable Double parseInput(@NotNull String text) {
                if (MathUtils.isDouble(text)) return Double.parseDouble(text);
                return null;
            }

            @Override
            public @NotNull String formatValue(@Nullable Double value) {
                return (value != null) ? String.valueOf(value) : "";
            }

            @Override
            public double toSliderValue(@NotNull Double value) {
                return value;
            }

            @Override
            public @NotNull Double fromSliderValue(double sliderValue) {
                return sliderValue;
            }

            @Override
            public boolean showAsInteger() {
                return false;
            }

            @Override
            public int getRoundingDecimalPlaces() {
                return 2;
            }
        };
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            ContextMenu subMenu = new ContextMenu();
            ContextMenu.StackApplier stackApplier = (stackEntry, value) -> {
                DoubleProperty resolved = (DoubleProperty) builder.self().getProperty(key);
                if (resolved == null) return;
                if (value instanceof String stringValue) {
                    resolved.setManualInput(stringValue);
                } else if (value instanceof Number numberValue) {
                    resolved.set(numberValue.doubleValue());
                } else if (value == null) {
                    resolved.set(property.getDefault());
                }
            };

            subMenu.addClickableEntry("input_via_number_picker", Component.translatable("fancymenu.context_menu.entries.number.input_via_number_picker"),
                            (contextMenu, entry) -> openNumberPickerWindow((ManualInputProperty<Double>) property, builder, contextMenu, entry, key, inputBehavior, inputFilter, adapter, defaultValue))
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.SLIDERS);

            subMenu.addClickableEntry("input_as_string", Component.translatable("fancymenu.context_menu.entries.number.input_as_string"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                List<? extends PropertyHolder> selectedObjects = stack.getObjects();
                                String defaultText = null;
                                List<String> targetValuesOfSelected = new ArrayList<>();
                                for (PropertyHolder holder : selectedObjects) {
                                    DoubleProperty resolved = (DoubleProperty) holder.getProperty(key);
                                    String value = (resolved != null) ? resolved.getRawInputOrFormattedValue() : null;
                                    targetValuesOfSelected.add(value);
                                }
                                if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                                    DoubleProperty resolved = (DoubleProperty) builder.self().getProperty(key);
                                    defaultText = (resolved != null) ? resolved.getRawInputOrFormattedValue() : String.valueOf(defaultValue);
                                }
                                TextEditorWindowBody s = new TextEditorWindowBody(Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null, call -> {
                                    if (call != null) {
                                        builder.saveSnapshot();
                                        builder.applyStackAppliers(entry, call);
                                    }
                                });
                                if (property.userInputTextValidator != null) {
                                    s.setTextValidator(consumes -> property.userInputTextValidator.get(consumes.getText()));
                                }
                                s.setText(defaultText);
                                s.setMultilineMode(false);
                                s.setPlaceholdersAllowed(true);
                                contextMenu.closeMenuChain();
                                Dialogs.openGeneric(s, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null,
                                        TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                                        .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                            })
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.TEXT_FIELDS);

            subMenu.addSeparatorEntry("separator_before_reset");

            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.common_components.reset"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                builder.saveSnapshot();
                                builder.applyStackAppliers(entry, null);
                            })
                    .setStackable(true)
                    .setStackApplier((stackEntry, value) -> {
                        DoubleProperty resolved = (DoubleProperty) builder.self().getProperty(key);
                        if (resolved != null) resolved.set(property.getDefault());
                    })
                    .setIcon(MaterialIcons.UNDO);

            return new ContextMenu.SubMenuContextMenuEntry("menu_entry_" + key, menu, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), subMenu)
                    .setIcon(MaterialIcons.FUNCTIONS)
                    .setStackable(true);
        };
        return p;
    }

    /**
     * Convenience overload that defaults to free input behavior.
     *
     * @see #doubleProperty(String, double, double, String, NumericInputBehavior)
     */
    @NotNull
    public static DoubleProperty doubleProperty(@NotNull String key, double defaultValue, double currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return doubleProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase, null);
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #doubleProperty(String, double, double, String, NumericInputBehavior)
     */
    @NotNull
    public static DoubleProperty doubleProperty(@NotNull String key, double defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase, @Nullable NumericInputBehavior<Double> inputBehavior) {
        return doubleProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, inputBehavior);
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #doubleProperty(String, double, double, String)
     */
    @NotNull
    public static DoubleProperty doubleProperty(@NotNull String key, double defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return doubleProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, null);
    }

    /**
     * Creates a long {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Long#valueOf(String)} and uses a custom
     * number picker plus plain text entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * LongProperty seed = Property.longProperty(
     *     "seed",
     *     0L,
     *     123456L,
     *     "fancymenu.menu.entry.seed"
     * );
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured long property
     */
    @NotNull
    public static LongProperty longProperty(@NotNull String key, long defaultValue, long currentValue, @NotNull String contextMenuEntryLocalizationKeyBase, @Nullable NumericInputBehavior<Long> inputBehavior) {
        LongProperty p = new LongProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Long::valueOf;
        CharacterFilter inputFilter = CharacterFilter.buildIntegerFilter();
        NumberPickerWindowBody.ValueAdapter<Long> adapter = new NumberPickerWindowBody.ValueAdapter<>() {
            @Override
            public @Nullable Long parseInput(@NotNull String text) {
                if (MathUtils.isLong(text)) return Long.parseLong(text);
                return null;
            }

            @Override
            public @NotNull String formatValue(@Nullable Long value) {
                return (value != null) ? String.valueOf(value) : "";
            }

            @Override
            public double toSliderValue(@NotNull Long value) {
                return value.doubleValue();
            }

            @Override
            public @NotNull Long fromSliderValue(double sliderValue) {
                return Math.round(sliderValue);
            }

            @Override
            public boolean showAsInteger() {
                return true;
            }

            @Override
            public int getRoundingDecimalPlaces() {
                return 0;
            }
        };
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            ContextMenu subMenu = new ContextMenu();
            ContextMenu.StackApplier stackApplier = (stackEntry, value) -> {
                LongProperty resolved = (LongProperty) builder.self().getProperty(key);
                if (resolved == null) return;
                if (value instanceof String stringValue) {
                    resolved.setManualInput(stringValue);
                } else if (value instanceof Number numberValue) {
                    resolved.set(numberValue.longValue());
                } else if (value == null) {
                    resolved.set(property.getDefault());
                }
            };

            subMenu.addClickableEntry("input_via_number_picker", Component.translatable("fancymenu.context_menu.entries.number.input_via_number_picker"),
                            (contextMenu, entry) -> openNumberPickerWindow((ManualInputProperty<Long>) property, builder, contextMenu, entry, key, inputBehavior, inputFilter, adapter, defaultValue))
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.SLIDERS);

            subMenu.addClickableEntry("input_as_string", Component.translatable("fancymenu.context_menu.entries.number.input_as_string"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                List<? extends PropertyHolder> selectedObjects = stack.getObjects();
                                String defaultText = null;
                                List<String> targetValuesOfSelected = new ArrayList<>();
                                for (PropertyHolder holder : selectedObjects) {
                                    LongProperty resolved = (LongProperty) holder.getProperty(key);
                                    String value = (resolved != null) ? resolved.getRawInputOrFormattedValue() : null;
                                    targetValuesOfSelected.add(value);
                                }
                                if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                                    LongProperty resolved = (LongProperty) builder.self().getProperty(key);
                                    defaultText = (resolved != null) ? resolved.getRawInputOrFormattedValue() : String.valueOf(defaultValue);
                                }
                                TextEditorWindowBody s = new TextEditorWindowBody(Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null, call -> {
                                    if (call != null) {
                                        builder.saveSnapshot();
                                        builder.applyStackAppliers(entry, call);
                                    }
                                });
                                if (property.userInputTextValidator != null) {
                                    s.setTextValidator(consumes -> property.userInputTextValidator.get(consumes.getText()));
                                }
                                s.setText(defaultText);
                                s.setMultilineMode(false);
                                s.setPlaceholdersAllowed(true);
                                contextMenu.closeMenuChain();
                                Dialogs.openGeneric(s, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null,
                                        TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                                        .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                            })
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.TEXT_FIELDS);

            subMenu.addSeparatorEntry("separator_before_reset");

            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.common_components.reset"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                builder.saveSnapshot();
                                builder.applyStackAppliers(entry, null);
                            })
                    .setStackable(true)
                    .setStackApplier((stackEntry, value) -> {
                        LongProperty resolved = (LongProperty) builder.self().getProperty(key);
                        if (resolved != null) resolved.set(property.getDefault());
                    })
                    .setIcon(MaterialIcons.UNDO);

            return new ContextMenu.SubMenuContextMenuEntry("menu_entry_" + key, menu, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), subMenu)
                    .setIcon(MaterialIcons.NUMBERS)
                    .setStackable(true);
        };
        return p;
    }

    /**
     * Convenience overload that defaults to free input behavior.
     *
     * @see #longProperty(String, long, long, String, NumericInputBehavior)
     */
    @NotNull
    public static LongProperty longProperty(@NotNull String key, long defaultValue, long currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return longProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase, null);
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #longProperty(String, long, long, String, NumericInputBehavior)
     */
    @NotNull
    public static LongProperty longProperty(@NotNull String key, long defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase, @Nullable NumericInputBehavior<Long> inputBehavior) {
        return longProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, inputBehavior);
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #longProperty(String, long, long, String)
     */
    @NotNull
    public static LongProperty longProperty(@NotNull String key, long defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return longProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, null);
    }

    /**
     * Creates a float {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Float#valueOf(String)} and uses a custom
     * number picker plus plain text entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * FloatProperty scale = Property.floatProperty(
     *     "scale",
     *     1.0f,
     *     1.0f,
     *     "fancymenu.menu.entry.scale"
     * );
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured float property
     */
    @NotNull
    public static FloatProperty floatProperty(@NotNull String key, float defaultValue, float currentValue, @NotNull String contextMenuEntryLocalizationKeyBase, @Nullable NumericInputBehavior<Float> inputBehavior) {
        FloatProperty p = new FloatProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Float::valueOf;
        CharacterFilter inputFilter = CharacterFilter.buildDecimalFiler();
        NumberPickerWindowBody.ValueAdapter<Float> adapter = new NumberPickerWindowBody.ValueAdapter<>() {
            @Override
            public @Nullable Float parseInput(@NotNull String text) {
                if (MathUtils.isFloat(text)) return Float.parseFloat(text);
                return null;
            }

            @Override
            public @NotNull String formatValue(@Nullable Float value) {
                return (value != null) ? String.valueOf(value) : "";
            }

            @Override
            public double toSliderValue(@NotNull Float value) {
                return value.doubleValue();
            }

            @Override
            public @NotNull Float fromSliderValue(double sliderValue) {
                return (float)sliderValue;
            }

            @Override
            public boolean showAsInteger() {
                return false;
            }

            @Override
            public int getRoundingDecimalPlaces() {
                return 2;
            }
        };
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            ContextMenu subMenu = new ContextMenu();
            ContextMenu.StackApplier stackApplier = (stackEntry, value) -> {
                FloatProperty resolved = (FloatProperty) builder.self().getProperty(key);
                if (resolved == null) return;
                if (value instanceof String stringValue) {
                    resolved.setManualInput(stringValue);
                } else if (value instanceof Number numberValue) {
                    resolved.set(numberValue.floatValue());
                } else if (value == null) {
                    resolved.set(property.getDefault());
                }
            };

            subMenu.addClickableEntry("input_via_number_picker", Component.translatable("fancymenu.context_menu.entries.number.input_via_number_picker"),
                            (contextMenu, entry) -> openNumberPickerWindow((ManualInputProperty<Float>) property, builder, contextMenu, entry, key, inputBehavior, inputFilter, adapter, defaultValue))
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.SLIDERS);

            subMenu.addClickableEntry("input_as_string", Component.translatable("fancymenu.context_menu.entries.number.input_as_string"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                List<? extends PropertyHolder> selectedObjects = stack.getObjects();
                                String defaultText = null;
                                List<String> targetValuesOfSelected = new ArrayList<>();
                                for (PropertyHolder holder : selectedObjects) {
                                    FloatProperty resolved = (FloatProperty) holder.getProperty(key);
                                    String value = (resolved != null) ? resolved.getRawInputOrFormattedValue() : null;
                                    targetValuesOfSelected.add(value);
                                }
                                if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                                    FloatProperty resolved = (FloatProperty) builder.self().getProperty(key);
                                    defaultText = (resolved != null) ? resolved.getRawInputOrFormattedValue() : String.valueOf(defaultValue);
                                }
                                TextEditorWindowBody s = new TextEditorWindowBody(Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null, call -> {
                                    if (call != null) {
                                        builder.saveSnapshot();
                                        builder.applyStackAppliers(entry, call);
                                    }
                                });
                                if (property.userInputTextValidator != null) {
                                    s.setTextValidator(consumes -> property.userInputTextValidator.get(consumes.getText()));
                                }
                                s.setText(defaultText);
                                s.setMultilineMode(false);
                                s.setPlaceholdersAllowed(true);
                                contextMenu.closeMenuChain();
                                Dialogs.openGeneric(s, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null,
                                        TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                                        .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                            })
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.TEXT_FIELDS);

            subMenu.addSeparatorEntry("separator_before_reset");

            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.common_components.reset"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                builder.saveSnapshot();
                                builder.applyStackAppliers(entry, null);
                            })
                    .setStackable(true)
                    .setStackApplier((stackEntry, value) -> {
                        FloatProperty resolved = (FloatProperty) builder.self().getProperty(key);
                        if (resolved != null) resolved.set(property.getDefault());
                    })
                    .setIcon(MaterialIcons.UNDO);

            return new ContextMenu.SubMenuContextMenuEntry("menu_entry_" + key, menu, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), subMenu)
                    .setIcon(MaterialIcons.FUNCTIONS)
                    .setStackable(true);
        };
        return p;
    }

    /**
     * Convenience overload that defaults to free input behavior.
     *
     * @see #floatProperty(String, float, float, String, NumericInputBehavior)
     */
    @NotNull
    public static FloatProperty floatProperty(@NotNull String key, float defaultValue, float currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return floatProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase, null);
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #floatProperty(String, float, float, String, NumericInputBehavior)
     */
    @NotNull
    public static FloatProperty floatProperty(@NotNull String key, float defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase, @Nullable NumericInputBehavior<Float> inputBehavior) {
        return floatProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, inputBehavior);
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #floatProperty(String, float, float, String)
     */
    @NotNull
    public static FloatProperty floatProperty(@NotNull String key, float defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return floatProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, null);
    }

    /**
     * Creates a boolean {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Boolean#valueOf(String)} and uses a toggle
     * entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * BooleanProperty enabled = Property.booleanProperty(
     *     "enabled",
     *     true,
     *     true,
     *     "fancymenu.menu.entry.display_name"
     * );
     * }</pre>
     *
     * <h3>Localization format</h3>
     * The localization value for {@code contextMenuEntryLocalizationKeyBase} must include a
     * {@code %s} placeholder (for example {@code "Display Name: %s"}). The mod replaces
     * {@code %s} dynamically with the current state text (Enabled/Disabled).
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured boolean property
     */
    @NotNull
    public static BooleanProperty booleanProperty(@NotNull String key, boolean defaultValue, boolean currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        BooleanProperty p = new BooleanProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Boolean::valueOf;
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            ContextMenu subMenu = new ContextMenu();
            ContextMenu.StackApplier stackApplier = (stackEntry, value) -> {
                BooleanProperty resolved = (BooleanProperty) builder.self().getProperty(key);
                if (resolved == null) return;
                if (value instanceof String stringValue) {
                    resolved.setManualInput(stringValue);
                } else if (value instanceof Boolean boolValue) {
                    resolved.set(boolValue);
                } else if (value == null) {
                    resolved.set(property.getDefault());
                }
            };

            subMenu.addClickableEntry("input_as_string", Component.translatable("fancymenu.context_menu.entries.number.input_as_string"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                List<? extends PropertyHolder> selectedObjects = stack.getObjects();
                                String defaultText = null;
                                List<String> targetValuesOfSelected = new ArrayList<>();
                                for (PropertyHolder holder : selectedObjects) {
                                    BooleanProperty resolved = (BooleanProperty) holder.getProperty(key);
                                    String value = (resolved != null) ? resolved.getRawInputOrFormattedValue() : null;
                                    targetValuesOfSelected.add(value);
                                }
                                if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                                    BooleanProperty resolved = (BooleanProperty) builder.self().getProperty(key);
                                    defaultText = (resolved != null) ? resolved.getRawInputOrFormattedValue() : String.valueOf(defaultValue);
                                }
                                TextEditorWindowBody s = new TextEditorWindowBody(Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null, call -> {
                                    if (call != null) {
                                        builder.saveSnapshot();
                                        builder.applyStackAppliers(entry, call);
                                    }
                                });
                                if (property.userInputTextValidator != null) {
                                    s.setTextValidator(consumes -> property.userInputTextValidator.get(consumes.getText()));
                                }
                                s.setText(defaultText);
                                s.setMultilineMode(false);
                                s.setPlaceholdersAllowed(true);
                                contextMenu.closeMenuChain();
                                Dialogs.openGeneric(s, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null,
                                        TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                                        .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                            })
                    .setStackable(true)
                    .setStackApplier(stackApplier)
                    .setIcon(MaterialIcons.TEXT_FIELDS);

            ContextMenu.SubMenuContextMenuEntry entry = new ContextMenu.SubMenuContextMenuEntry("menu_entry_" + key, menu, Component.empty(), subMenu) {
                private boolean isManualMode() {
                    BooleanProperty resolved = (BooleanProperty) builder.self().getProperty(key);
                    return (resolved != null) && resolved.hasManualInput();
                }

                @Override
                protected void renderSubMenuArrow(GuiGraphics graphics) {
                    if (this.isManualMode()) {
                        super.renderSubMenuArrow(graphics);
                    }
                }

                @Override
                protected void tickEntry() {
                    if (!this.isManualMode()) {
                        if (this.subContextMenu.isOpen()) {
                            this.subContextMenu.closeMenu();
                        }
                        return;
                    }
                    super.tickEntry();
                }

                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    if ((button == 0) && this.isHovered() && this.isActive() && !this.parent.isSubMenuHovered() && !this.tooltipIconHovered) {
                        if (this.getStackMeta().isPartOfStack() && !this.getStackMeta().isFirstInStack()) {
                            return true;
                        }
                        if (FancyMenu.getOptions().playUiClickSounds.getValue() && this.enableClickSound) {
                            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        ContextMenuBuilder.StackValue<Boolean> currentValue = builder.resolveStackValue(this);
                        if (!currentValue.isEmpty()) {
                            boolean baseValue = currentValue.hasCommonValue() ? currentValue.getValue() : false;
                            boolean nextValue = !baseValue;
                            builder.saveSnapshot();
                            builder.applyStackAppliers(this, nextValue);
                        }
                        return true;
                    }
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            };

            entry.setLabelSupplier((contextMenu, entryRef) -> {
                BooleanProperty resolved = (BooleanProperty) builder.self().getProperty(key);
                if ((resolved != null) && resolved.hasManualInput()) {
                    Component manual = Component.translatable("fancymenu.general.cycle.enabled_disabled.manual")
                            .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_text_color.getColorInt()));
                    return Component.translatable(property.getContextMenuEntryLocalizationKeyBase(), manual);
                }
                Boolean value = (resolved != null) ? resolved.get() : property.getDefault();
                if (value != null && value) {
                    Component enabled = Component.translatable("fancymenu.general.cycle.enabled_disabled.enabled")
                            .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_text_color.getColorInt()));
                    return Component.translatable(property.getContextMenuEntryLocalizationKeyBase(), enabled);
                }
                Component disabled = Component.translatable("fancymenu.general.cycle.enabled_disabled.disabled")
                        .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()));
                return Component.translatable(property.getContextMenuEntryLocalizationKeyBase(), disabled);
            });
            entry.setStackApplier(stackApplier);
            entry.setStackValueSupplier(stackEntry -> {
                BooleanProperty resolved = (BooleanProperty) builder.self().getProperty(key);
                return (resolved != null) ? resolved.get() : property.getDefault();
            });
            entry.setStackable(true);
            entry.setStackGroupKey(builder.self().getClass());
            entry.setIcon(MaterialIcons.TOGGLE_ON);
            return entry;
        };
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #booleanProperty(String, boolean, boolean, String)
     */
    @NotNull
    public static BooleanProperty booleanProperty(@NotNull String key, boolean defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return booleanProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Creates a {@link ResourceSource} property with an explicit default and current value.
     * <p>
     * This property stores a serialized {@link ResourceSource} string (via
     * {@link ResourceSource#getSerializationSource()}) and uses a generic resource chooser entry in
     * context menus. The chooser entry is stack-aware and applies changes to all selected objects
     * that contain the property.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Property<ResourceSource> background = Property.resourceSourceProperty(
     *     "background",
     *     ResourceSource.of("local:my_pack/background.png"),
     *     "fancymenu.menu.entry.background",
     *     true,  // allowLocal
     *     true,  // allowWeb
     *     true,  // allowLocation
     *     null,  // fileFilter
     *     null,  // fileTypes
     *     FileMediaType.IMAGE
     * );
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @param allowLocal allow local resource selection in the chooser
     * @param allowWeb allow web resource selection in the chooser
     * @param allowLocation allow resource location selection in the chooser
     * @param fileFilter optional file filter for local selection
     * @param fileTypes optional file type group for filtering/labeling
     * @param fileMediaType media type used to interpret the selected resource
     * @return a configured resource source property
     */
    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue, @Nullable ResourceSource currentValue, @NotNull String contextMenuEntryLocalizationKeyBase, boolean allowLocal, boolean allowWeb, boolean allowLocation, @Nullable FileFilter fileFilter, @Nullable FileTypeGroup<FileType<Resource>> fileTypes, @NotNull FileMediaType fileMediaType) {
        Property<ResourceSource> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = ResourceSource::of;
        p.serializationCodec = consumes -> {
            if (consumes == null) return null;
            return consumes.getSerializationSource();
        };
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            ResourceSupplier<Resource> defaultSupplier = null;
            ResourceSource defaultSource = property.getDefault();
            if (defaultSource != null) {
                defaultSupplier = new ResourceSupplier<>(Resource.class, fileMediaType, defaultSource.getSourceWithPrefix());
            }
            return builder.buildGenericResourceChooserContextMenuEntry(menu, "menu_entry_" + key, consumes -> consumes.getProperty(key) != null,
                    () -> ResourceChooserWindowBody.generic(null, null, file -> {}),
                    source -> new ResourceSupplier<>(Resource.class, fileMediaType, source),
                    defaultSupplier,
                    consumes -> {
                        Property<ResourceSource> resolved = (Property<ResourceSource>) consumes.getProperty(key);
                        ResourceSource value = (resolved != null) ? resolved.get() : defaultSource;
                        if (value == null) return null;
                        return new ResourceSupplier<>(Resource.class, fileMediaType, value.getSourceWithPrefix());
                    },
                    (b, supplier) -> {
                        Property<ResourceSource> resolved = (Property<ResourceSource>) b.getProperty(key);
                        if (resolved == null) return;
                        if (supplier == null) {
                            resolved.set(null);
                        } else {
                            resolved.set(ResourceSource.of(supplier.getSourceWithPrefix()));
                        }
                    },
                    Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb)
                    .setIcon(MaterialIcons.FOLDER_OPEN);
        };
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #resourceSourceProperty(String, ResourceSource, ResourceSource, String, boolean, boolean, boolean, FileFilter, FileTypeGroup, FileMediaType)
     */
    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase, boolean allowLocal, boolean allowWeb, boolean allowLocation, @Nullable FileFilter fileFilter, @Nullable FileTypeGroup<FileType<Resource>> fileTypes, @NotNull FileMediaType fileMediaType) {
        return resourceSourceProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, allowLocal, allowWeb, allowLocation, fileFilter, fileTypes, fileMediaType);
    }

    /**
     * Creates a {@link ResourceSupplier} property with an explicit default and current value.
     * <p>
     * The {@code resourceType} controls which resource chooser entry is created:
     * image, audio, video, or text. The value serializes to a source string via
     * {@link ResourceSupplier#getSourceWithPrefix()}. The chooser entry is stack-aware and applies
     * changes to all selected objects that contain the property.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Property<ResourceSupplier<ITexture>> icon = Property.resourceSupplierProperty(
     *     ITexture.class,
     *     "icon",
     *     ResourceSupplier.image("local:my_pack/icon.png"),
     *     ResourceSupplier.image("local:my_pack/icon.png"),
     *     "fancymenu.menu.entry.icon",
     *     true,
     *     true,
     *     true,
     *     null
     * );
     * }</pre>
     *
     * @param resourceType the resource type; selects the correct chooser UI
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @param allowLocal allow local resource selection in the chooser
     * @param allowWeb allow web resource selection in the chooser
     * @param allowLocation allow resource location selection in the chooser
     * @param fileFilter optional file filter for local selection
     * @param <R> resource value type
     * @return a configured resource supplier property
     */
    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue, @Nullable ResourceSupplier<R> currentValue, @NotNull String contextMenuEntryLocalizationKeyBase, boolean allowLocal, boolean allowWeb, boolean allowLocation, @Nullable FileFilter fileFilter) {
        Property<ResourceSupplier<R>> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = consumes -> {
            if (ITexture.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.image(consumes);
            }
            if (IAudio.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.audio(consumes);
            }
            if (IVideo.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.video(consumes);
            }
            if (IText.class.isAssignableFrom(resourceType)) {
                return (ResourceSupplier<R>) ResourceSupplier.text(consumes);
            }
            throw new IllegalArgumentException("Unknown resource format! Unable to deserialize ResourceSupplier property!");
        };
        p.serializationCodec = consumes -> {
            if (consumes == null) return null;
            return consumes.getSourceWithPrefix();
        };
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            if (ITexture.class.isAssignableFrom(resourceType)) {
                return builder.buildImageResourceChooserContextMenuEntry(menu, "menu_entry_" + key, consumes -> consumes.getProperty(key) != null,
                        (ResourceSupplier<ITexture>) property.getDefault(),
                        consumes -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) consumes.getProperty(key);
                            ResourceSupplier<R> value = (resolved != null) ? resolved.get() : property.getDefault();
                            return (ResourceSupplier<ITexture>) value;
                        },
                        (b, supplier) -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) b.getProperty(key);
                            if (resolved != null) resolved.set((ResourceSupplier<R>) supplier);
                        },
                        Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileFilter, allowLocation, allowLocal, allowWeb)
                        .setIcon(MaterialIcons.IMAGE);
            }
            if (IAudio.class.isAssignableFrom(resourceType)) {
                return builder.buildAudioResourceChooserContextMenuEntry(menu, "menu_entry_" + key, consumes -> consumes.getProperty(key) != null,
                        (ResourceSupplier<IAudio>) property.getDefault(),
                        consumes -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) consumes.getProperty(key);
                            ResourceSupplier<R> value = (resolved != null) ? resolved.get() : property.getDefault();
                            return (ResourceSupplier<IAudio>) value;
                        },
                        (b, supplier) -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) b.getProperty(key);
                            if (resolved != null) resolved.set((ResourceSupplier<R>) supplier);
                        },
                        Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileFilter, allowLocation, allowLocal, allowWeb)
                        .setIcon(MaterialIcons.MUSIC_NOTE);
            }
            if (IVideo.class.isAssignableFrom(resourceType)) {
                return builder.buildVideoResourceChooserContextMenuEntry(menu, "menu_entry_" + key, consumes -> consumes.getProperty(key) != null,
                        (ResourceSupplier<IVideo>) property.getDefault(),
                        consumes -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) consumes.getProperty(key);
                            ResourceSupplier<R> value = (resolved != null) ? resolved.get() : property.getDefault();
                            return (ResourceSupplier<IVideo>) value;
                        },
                        (b, supplier) -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) b.getProperty(key);
                            if (resolved != null) resolved.set((ResourceSupplier<R>) supplier);
                        },
                        Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileFilter, allowLocation, allowLocal, allowWeb)
                        .setIcon(MaterialIcons.VIDEOCAM);
            }
            if (IText.class.isAssignableFrom(resourceType)) {
                return builder.buildTextResourceChooserContextMenuEntry(menu, "menu_entry_" + key, consumes -> consumes.getProperty(key) != null,
                        (ResourceSupplier<IText>) property.getDefault(),
                        consumes -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) consumes.getProperty(key);
                            ResourceSupplier<R> value = (resolved != null) ? resolved.get() : property.getDefault();
                            return (ResourceSupplier<IText>) value;
                        },
                        (b, supplier) -> {
                            Property<ResourceSupplier<R>> resolved = (Property<ResourceSupplier<R>>) b.getProperty(key);
                            if (resolved != null) resolved.set((ResourceSupplier<R>) supplier);
                        },
                        Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileFilter, allowLocation, allowLocal, allowWeb)
                        .setIcon(MaterialIcons.TEXT_FIELDS);
            }
            throw new IllegalArgumentException("Unknown resource format! Unable to build ResourceSupplier context menu entry!");
        };
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #resourceSupplierProperty(Class, String, ResourceSupplier, ResourceSupplier, String, boolean, boolean, boolean, FileFilter)
     */
    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase, boolean allowLocal, boolean allowWeb, boolean allowLocation, @Nullable FileFilter fileFilter) {
        return resourceSupplierProperty(resourceType, key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, allowLocal, allowWeb, allowLocation, fileFilter);
    }

    /**
     * Creates a hex color {@link String} property with an explicit default and current value.
     * <p>
     * Values serialize as raw strings, and the context menu entry provides both string input and a
     * color picker. No validation is applied so placeholders can remain unresolved.
     * The entry is stack-aware for multi-selection.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * ColorProperty color = Property.hexColorProperty(
     *     "color",
     *     "#ffffff",
     *     "#ff8800",
     *     true, // placeholders
     *     "fancymenu.menu.entry.color"
     * );
     *
     * String hex = color.getHex(); // ready-to-use with placeholders replaced already
     * DrawableColor drawable = color.getDrawable(); // ready-to-use with placeholders replaced already
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param placeholders whether placeholder tokens are allowed in input
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured hex color string property
     */
    @NotNull
    public static ColorProperty hexColorProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        ColorProperty p = new ColorProperty(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = consumes -> consumes;
        p.serializationCodec = consumes -> consumes;
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            ContextMenu subMenu = new ContextMenu();

            if (placeholders) {

                subMenu.addClickableEntry("input_as_string", Component.translatable("fancymenu.context_menu.entries.color.input_as_string"),
                                (contextMenu, entry) -> {
                                    ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                    if (!stack.isPrimary() || stack.isEmpty()) {
                                        return;
                                    }
                                    List<? extends PropertyHolder> selectedObjects = stack.getObjects();
                                    String defaultText = null;
                                    List<String> targetValuesOfSelected = new ArrayList<>();
                                    for (PropertyHolder holder : selectedObjects) {
                                        ColorProperty resolved = (ColorProperty) holder.getProperty(key);
                                        String value = (resolved != null) ? resolved.get() : property.getDefault();
                                        targetValuesOfSelected.add(value);
                                    }
                                    if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                                        ColorProperty resolved = (ColorProperty) builder.self().getProperty(key);
                                        defaultText = (resolved != null) ? resolved.get() : property.getDefault();
                                    }
                                    TextEditorWindowBody s = new TextEditorWindowBody(Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null, call -> {
                                        if (call != null) {
                                            builder.saveSnapshot();
                                            builder.applyStackAppliers(entry, call);
                                        }
                                    });
                                    s.setText(defaultText);
                                    s.setMultilineMode(false);
                                    s.setPlaceholdersAllowed(true);
                                    contextMenu.closeMenuChain();
                                    Dialogs.openGeneric(s, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null,
                                            TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT)
                                            .getSecond().setIcon(MaterialIcons.TEXT_FIELDS);
                                })
                        .setStackable(true)
                        .setStackApplier((stackEntry, value) -> {
                            String call = null;
                            if (value instanceof String stringValue) {
                                call = stringValue;
                            } else if (value instanceof DrawableColor drawableValue) {
                                call = drawableValue.getHex();
                            }
                            if (call == null) {
                                return;
                            }
                            ColorProperty resolved = (ColorProperty) builder.self().getProperty(key);
                            if (resolved != null) resolved.set(call);
                        })
                        .setIcon(MaterialIcons.TEXT_FIELDS);

            }

            subMenu.addClickableEntry("input_via_color_picker", Component.translatable("fancymenu.context_menu.entries.color.input_via_color_picker"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                List<? extends PropertyHolder> selectedObjects = stack.getObjects();
                                String presetRaw = null;
                                List<String> targetValuesOfSelected = new ArrayList<>();
                                for (PropertyHolder holder : selectedObjects) {
                                    ColorProperty resolved = (ColorProperty) holder.getProperty(key);
                                    String value = (resolved != null) ? resolved.get() : property.getDefault();
                                    targetValuesOfSelected.add(value);
                                }
                                if (!stack.isStacked() || ListUtils.allInListEqual(targetValuesOfSelected)) {
                                    ColorProperty resolved = (ColorProperty) builder.self().getProperty(key);
                                    presetRaw = (resolved != null) ? resolved.get() : property.getDefault();
                                } else {
                                    presetRaw = property.getDefault();
                                }
                                DrawableColor preset = null;
                                if (presetRaw != null) {
                                    DrawableColor parsed = DrawableColor.of(presetRaw);
                                    if (parsed != DrawableColor.EMPTY) {
                                        preset = parsed;
                                    }
                                }
                                ColorPickerWindowBody picker = new ColorPickerWindowBody(preset,
                                        drawable -> { // onColorUpdate
                                            if (drawable != null) {
                                                builder.applyStackAppliers(entry, drawable);
                                            }
                                        },
                                        drawable -> { // onDone
                                            builder.saveSnapshot();
                                            if (drawable != null) {
                                                builder.applyStackAppliers(entry, drawable);
                                            }
                                        },
                                        drawable -> { // onCancel
                                            if (drawable != null) {
                                                builder.applyStackAppliers(entry, drawable);
                                            }
                                        });
                                int centerX = (Minecraft.getInstance().screen != null) ? (Minecraft.getInstance().screen.width / 2) : 100;
                                int centerY = (Minecraft.getInstance().screen != null) ? (Minecraft.getInstance().screen.height / 2) : 100;
                                contextMenu.closeMenuChain();
                                PiPWindow window = new PiPWindow(Component.translatable("fancymenu.ui.color_picker.title"))
                                        .setScreen(picker)
                                        .setForceFancyMenuUiScale(true)
                                        .setMinSize(ColorPickerWindowBody.PIP_WINDOW_WIDTH, ColorPickerWindowBody.PIP_WINDOW_HEIGHT)
                                        .setSize(ColorPickerWindowBody.PIP_WINDOW_WIDTH, ColorPickerWindowBody.PIP_WINDOW_HEIGHT);
                                PiPWindowHandler.INSTANCE.openWindowCentered(window, null);
                            })
                    .setStackable(true)
                    .setStackApplier((stackEntry, value) -> {
                        String call = null;
                        if (value instanceof String stringValue) {
                            call = stringValue;
                        } else if (value instanceof DrawableColor drawableValue) {
                            call = drawableValue.getHex();
                        }
                        if (call == null) {
                            return;
                        }
                        ColorProperty resolved = (ColorProperty) builder.self().getProperty(key);
                        if (resolved != null) resolved.set(call);
                    })
                    .setIcon(MaterialIcons.DROPPER_EYE);

            subMenu.addSeparatorEntry("separator_before_reset");

            subMenu.addClickableEntry("reset_to_default", Component.translatable("fancymenu.common_components.reset"),
                            (contextMenu, entry) -> {
                                ContextMenuBuilder.StackContext<? extends PropertyHolder> stack = builder.stack(entry, consumes -> consumes.getProperty(key) != null);
                                if (!stack.isPrimary() || stack.isEmpty()) {
                                    return;
                                }
                                builder.saveSnapshot();
                                builder.applyStackAppliers(entry, null);
                            })
                    .setStackable(true)
                    .setStackApplier((stackEntry, value) -> {
                        ColorProperty resolved = (ColorProperty) builder.self().getProperty(key);
                        if (resolved != null) resolved.set(property.getDefault());
                    })
                    .setIcon(MaterialIcons.UNDO);

            subMenu.addSeparatorEntry("separator_before_current_value_display")
                    .addIsVisibleSupplier((contextMenu, entry) -> builder.stack(entry, consumes -> consumes.getProperty(key) != null).getObjects().size() == 1);
            subMenu.addClickableEntry("current_value_display", Component.empty(), (contextMenu, entry) -> {})
                    .setLabelSupplier((contextMenu, entry) -> {
                        List<? extends PropertyHolder> selectedObjects = builder.stack(entry, consumes -> consumes.getProperty(key) != null).getObjects();
                        if (selectedObjects.size() == 1) {
                            Component valueComponent;
                            ColorProperty resolved = (ColorProperty) selectedObjects.get(0).getProperty(key);
                            String val = (resolved != null) ? resolved.get() : property.getDefault();
                            if (val == null) {
                                valueComponent = Component.literal("---").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().error_text_color.getColorInt()));
                            } else {
                                if (Minecraft.getInstance().font.width(val) > 150) {
                                    val = new StringBuilder(val).reverse().toString();
                                    val = Minecraft.getInstance().font.plainSubstrByWidth(val, 150);
                                    val = new StringBuilder(val).reverse().toString();
                                    val = ".." + val;
                                }
                                valueComponent = Component.literal(val).setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_text_color.getColorInt()));
                            }
                            return Component.translatable("fancymenu.context_menu.entries.choose_or_set.current", valueComponent);
                        }
                        return Component.empty();
                    })
                    .setClickSoundEnabled(false)
                    .setChangeBackgroundColorOnHover(false)
                    .addIsVisibleSupplier((contextMenu, entry) -> builder.stack(entry, consumes -> consumes.getProperty(key) != null).getObjects().size() == 1)
                    .setIcon(MaterialIcons.INFO);

            return new ContextMenu.SubMenuContextMenuEntry("menu_entry_" + key, menu, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), subMenu)
                    .setIcon(MaterialIcons.PALETTE)
                    .setStackable(true);
        };
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #hexColorProperty(String, String, String, boolean, String)
     */
    @NotNull
    public static ColorProperty hexColorProperty(@NotNull String key, @Nullable String defaultValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return hexColorProperty(key, defaultValue, defaultValue, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Creates a {@link RequirementContainer} property with custom serialization and a specialized
     * editor UI entry.
     * <p>
     * The context menu entry opens {@link ManageRequirementsScreen}. When used in stacked context
     * menus, it can optionally prompt the user before overwriting differing values and then applies
     * the edited container to all selected objects.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Property<RequirementContainer> requirements = Property.requirementContainerProperty(
     *     "requirements",
     *     new RequirementContainer(),
     *     "fancymenu.menu.entry.requirements"
     * );
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured requirement container property
     */
    @NotNull
    public static Property<RequirementContainer> requirementContainerProperty(@NotNull String key, @Nullable RequirementContainer defaultValue, @Nullable RequirementContainer currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<RequirementContainer> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase) {
            @Override
            public Property<RequirementContainer> deserialize(@NotNull PropertyContainer properties) {
                try {
                    String identifier = properties.getValue(this.getKey());
                    if (identifier != null) {
                        RequirementContainer container = RequirementContainer.deserializeWithIdentifier(identifier, properties);
                        this.set((container != null) ? container : this.defaultValue);
                    } else {
                        var containers = RequirementContainer.deserializeAll(properties);
                        if (containers.size() == 1) {
                            this.set(containers.getFirst());
                        } else {
                            this.set(this.defaultValue);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), ex);
                }
                return this;
            }

            @Override
            public Property<RequirementContainer> serialize(@NotNull PropertyContainer properties) {
                try {
                    RequirementContainer container = this.currentValue;
                    if (container == null) {
                        properties.putProperty(this.getKey(), null);
                        return this;
                    }
                    properties.putProperty(this.getKey(), container.identifier);
                    container.serializeToExistingPropertyContainer(properties);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
                }
                return this;
            }
        };
        p.contextMenuEntrySupplier = (property, builder, menu) -> new ContextMenu.ClickableContextMenuEntry<>("menu_entry_" + key, menu, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), (contextMenu, entry) -> {
            if (!entry.getStackMeta().isPartOfStack()) {
                Property<RequirementContainer> resolved = (Property<RequirementContainer>) builder.self().getProperty(key);
                RequirementContainer container = null;
                if (resolved != null) {
                    container = resolved.get();
                    if (container == null) {
                        container = resolved.getDefault();
                    }
                }
                if (container == null) {
                    container = new RequirementContainer();
                }
                ManageRequirementsScreen s = new ManageRequirementsScreen(container.copy(false), (call) -> {
                    if (call != null) {
                        builder.saveSnapshot();
                        if (resolved != null) {
                            resolved.set(call);
                        }
                    }
                });
                contextMenu.closeMenuChain();
                ManageRequirementsScreen.openInWindow(s);
            } else if (entry.getStackMeta().isFirstInStack()) {
                List<RequirementContainer> containers = ObjectUtils.getOfAll(RequirementContainer.class,
                        builder.getFilteredStackableObjectsList(consumes -> consumes.getProperty(key) != null),
                        consumes -> {
                            Property<RequirementContainer> resolved = (Property<RequirementContainer>) consumes.getProperty(key);
                            if (resolved == null) return null;
                            RequirementContainer value = resolved.get();
                            return (value != null) ? value : resolved.getDefault();
                        });
                RequirementContainer containerToUseInManager = new RequirementContainer();
                boolean allEqual = ListUtils.allInListEqual(containers);
                if (allEqual && !containers.isEmpty() && containers.getFirst() != null) {
                    containerToUseInManager = containers.getFirst().copy(true);
                }
                ManageRequirementsScreen s = new ManageRequirementsScreen(containerToUseInManager, (call) -> {
                    if (call != null) {
                        builder.saveSnapshot();
                        for (PropertyHolder holder : builder.getFilteredStackableObjectsList(consumes -> consumes.getProperty(key) != null)) {
                            Property<RequirementContainer> resolved = (Property<RequirementContainer>) holder.getProperty(key);
                            if (resolved != null) {
                                resolved.set(call.copy(true));
                            }
                        }
                    }
                });
                if (allEqual) {
                    contextMenu.closeMenuChain();
                    ManageRequirementsScreen.openInWindow(s);
                } else {
                    Dialogs.openMessageWithCallback(Component.translatable("fancymenu.requirements.multiselect.warning.override"), MessageDialogStyle.WARNING, call -> {
                        if (call) {
                            contextMenu.closeMenuChain();
                            ManageRequirementsScreen.openInWindow(s);
                        }
                    });
                }
            }
        }).setIcon(MaterialIcons.RULE).setStackable(true);
        p.serializationCodec = consumes -> (consumes == null) ? null : consumes.identifier;
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #requirementContainerProperty(String, RequirementContainer, RequirementContainer, String)
     */
    @NotNull
    public static Property<RequirementContainer> requirementContainerProperty(@NotNull String key, @Nullable RequirementContainer defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return requirementContainerProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Creates a {@link GenericExecutableBlock} property with custom serialization and a specialized
     * editor UI entry.
     * <p>
     * The context menu entry opens {@link ActionScriptEditorWindowBody}. When used in stacked context
     * menus, it can optionally prompt the user before overwriting differing values and then applies
     * the edited block to all selected objects.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Property<GenericExecutableBlock> onClick = Property.executableBlockProperty(
     *     "action",
     *     new GenericExecutableBlock(),
     *     "fancymenu.menu.entry.action"
     * );
     * }</pre>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     * @return a configured executable block property
     */
    @NotNull
    public static Property<GenericExecutableBlock> executableBlockProperty(@NotNull String key, @Nullable GenericExecutableBlock defaultValue, @Nullable GenericExecutableBlock currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<GenericExecutableBlock> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase) {
            @Override
            public Property<GenericExecutableBlock> deserialize(@NotNull PropertyContainer properties) {
                try {
                    String identifier = properties.getValue(this.getKey());
                    if (identifier != null) {
                        AbstractExecutableBlock block = ExecutableBlockDeserializer.deserializeWithIdentifier(properties, identifier);
                        if (block instanceof GenericExecutableBlock g) {
                            this.set(g);
                        } else {
                            this.set(this.defaultValue);
                        }
                    } else {
                        List<GenericExecutableBlock> blocks = ObjectUtils.getOfAll(GenericExecutableBlock.class, ExecutableBlockDeserializer.deserializeAll(properties), consumes -> {
                            if (consumes instanceof GenericExecutableBlock g) {
                                return g;
                            }
                            return null;
                        });
                        blocks.removeIf(Objects::isNull);
                        if (blocks.size() == 1) {
                            this.set(blocks.getFirst());
                        } else {
                            this.set(this.defaultValue);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), ex);
                }
                return this;
            }

            @Override
            public Property<GenericExecutableBlock> serialize(@NotNull PropertyContainer properties) {
                try {
                    GenericExecutableBlock block = this.currentValue;
                    if (block == null) {
                        properties.putProperty(this.getKey(), null);
                        return this;
                    }
                    properties.putProperty(this.getKey(), block.getIdentifier());
                    block.serializeToExistingPropertyContainer(properties);
                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
                }
                return this;
            }
        };
        p.contextMenuEntrySupplier = (property, builder, menu) -> new ContextMenu.ClickableContextMenuEntry<>("menu_entry_" + key, menu, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), (contextMenu, entry) -> {
            if (!entry.getStackMeta().isPartOfStack()) {
                Property<GenericExecutableBlock> resolved = (Property<GenericExecutableBlock>) builder.self().getProperty(key);
                GenericExecutableBlock block = null;
                if (resolved != null) {
                    block = resolved.get();
                    if (block == null) {
                        block = resolved.getDefault();
                    }
                }
                if (block == null) {
                    block = new GenericExecutableBlock();
                }
                ActionScriptEditorWindowBody s = new ActionScriptEditorWindowBody(block, (call) -> {
                    if (call != null) {
                        builder.saveSnapshot();
                        if (resolved != null) {
                            resolved.set(call);
                        }
                    }
                });
                contextMenu.closeMenuChain();
                ActionScriptEditorWindowBody.openInWindow(s);
            } else if (entry.getStackMeta().isFirstInStack()) {
                List<GenericExecutableBlock> blocks = ObjectUtils.getOfAll(GenericExecutableBlock.class,
                        builder.getFilteredStackableObjectsList(consumes -> consumes.getProperty(key) != null),
                        consumes -> {
                            Property<GenericExecutableBlock> resolved = (Property<GenericExecutableBlock>) consumes.getProperty(key);
                            if (resolved == null) return null;
                            GenericExecutableBlock value = resolved.get();
                            return (value != null) ? value : resolved.getDefault();
                        });
                GenericExecutableBlock blockToUseInManager = new GenericExecutableBlock();
                boolean allEqual = ListUtils.allInListEqual(blocks);
                if (allEqual && !blocks.isEmpty() && blocks.getFirst() != null) {
                    blockToUseInManager = blocks.getFirst().copy(true);
                }
                ActionScriptEditorWindowBody s = new ActionScriptEditorWindowBody(blockToUseInManager, (call) -> {
                    if (call != null) {
                        builder.saveSnapshot();
                        for (PropertyHolder holder : builder.getFilteredStackableObjectsList(consumes -> consumes.getProperty(key) != null)) {
                            Property<GenericExecutableBlock> resolved = (Property<GenericExecutableBlock>) holder.getProperty(key);
                            if (resolved != null) {
                                resolved.set(call.copy(true));
                            }
                        }
                    }
                });
                if (allEqual) {
                    contextMenu.closeMenuChain();
                    ActionScriptEditorWindowBody.openInWindow(s);
                } else {
                    Dialogs.openMessageWithCallback(Component.translatable("fancymenu.actions.multiselect.warning.override"), MessageDialogStyle.WARNING, call -> {
                        if (call) {
                            contextMenu.closeMenuChain();
                            ActionScriptEditorWindowBody.openInWindow(s);
                        }
                    });
                }
            }
        }).setIcon(MaterialIcons.CODE).setStackable(true);
        p.serializationCodec = consumes -> (consumes == null) ? null : consumes.getIdentifier();
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #executableBlockProperty(String, GenericExecutableBlock, GenericExecutableBlock, String)
     */
    @NotNull
    public static Property<GenericExecutableBlock> executableBlockProperty(@NotNull String key, @Nullable GenericExecutableBlock defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return executableBlockProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Creates a property with explicit default and current values.
     * <p>
     * Use the static factory methods for common property types; they configure codecs and context
     * menu entries automatically.
     * </p>
     *
     * @param key the unique key used for lookup and serialization
     * @param defaultValue the value used when no current value is present
     * @param currentValue the initial current value
     * @param contextMenuEntryLocalizationKeyBase translation key base for the entry label
     */
    protected Property(@NotNull String key, @Nullable T defaultValue, @Nullable T currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        this.key = Objects.requireNonNull(key);
        this.setDefault(defaultValue);
        this.set(currentValue);
        this.contextMenuEntryLocalizationKeyBase = Objects.requireNonNull(contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Creates a property where the initial current value is the default value.
     *
     * @see #Property(String, Object, Object, String)
     */
    protected Property(@NotNull String key, @Nullable T defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        this(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Returns the unique key used for lookup and serialization.
     */
    public @NotNull String getKey() {
        return key;
    }

    /**
     * Returns the default value. This may be {@code null}.
     */
    public @Nullable T getDefault() {
        return defaultValue;
    }

    /**
     * Sets the default value. This does not modify the current value.
     *
     * @param value new default value (nullable)
     * @return this property for chaining
     */
    public Property<T> setDefault(@Nullable T value) {
        this.defaultValue = value;
        return this;
    }

    /**
     * Returns {@code true} if the current value equals the default value (including both {@code null}).
     */
    public boolean isDefault() {
        return Objects.equals(this.defaultValue, this.currentValue);
    }

    /**
     * Returns the current value (after {@link #getValueGetProcessor() get-processing}, if configured).
     * This may return {@code null}.
     */
    public @Nullable T get() {
        return this.processGet(this.currentValue);
    }

    /**
     * Returns the current value if non-null; otherwise returns the default value.
     * <p>
     * If both values are {@code null}, this method throws a {@link NullPointerException}.
     * </p>
     *
     * @return a non-null value
     * @throws NullPointerException if both current and default values are {@code null}
     */
    public T tryGetNonNull() {
        var current = this.processGet(this.currentValue);
        if (current != null) return current;
        return Objects.requireNonNull(this.processGet(this.defaultValue));
    }

    /**
     * Returns the current value if non-null; otherwise returns the default value.
     * If both values are {@code null}, returns {@code elseValue}.
     *
     * @param elseValue fallback value to use if both current and default are {@code null}
     * @return a non-null value
     */
    public T tryGetNonNullElse(@NotNull T elseValue) {
        var current = this.processGet(this.currentValue);
        if (current != null) return current;
        return Objects.requireNonNullElse(this.processGet(this.defaultValue), elseValue);
    }

    /**
     * Applies the {@link #getValueGetProcessor() get-processor} if present.
     * Intended for subclasses that want to customize getter behavior further.
     */
    protected T processGet(@Nullable T get) {
        if (get == null) return null;
        if (this.valueGetProcessor != null) return this.valueGetProcessor.get(get);
        return get;
    }

    /**
     * Sets the current value, applying the {@link #getValueSetProcessor() set-processor} if present.
     * <p>
     * This processor is not applied to the default value.
     * </p>
     *
     * @param value new current value (nullable)
     * @return this property for chaining
     */
    public Property<T> set(@Nullable T value) {
        if (value != null) {
            if (this.valueSetProcessor != null) value = this.valueSetProcessor.get(value);
        }
        @Nullable T finalValue = value;
        this.valueSetListeners.forEach(listener -> listener.onSet(this.currentValue, finalValue));
        this.currentValue = value;
        return this;
    }

    /**
     * Sets the current value from an {@link Object} without compile-time type safety.
     * <p>
     * This method is useful when you already know the runtime type (for example, in generic or
     * reflective code paths) and still want to run the set-processor.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Object raw = "value";
     * property.forceSet(raw); // assumes the property type is String
     * }</pre>
     *
     * @param value new current value (nullable)
     * @return this property for chaining
     * @throws ClassCastException if the value cannot be cast to {@code T}
     */
    public Property<T> forceSet(@Nullable Object value) {
        if (value != null) {
            if (this.valueSetProcessor != null) value = this.valueSetProcessor.get((T) value);
        }
        @Nullable T finalValue = (T) value;
        this.valueSetListeners.forEach(listener -> listener.onSet(this.currentValue, finalValue));
        this.currentValue = (T) value;
        return this;
    }

    /**
     * Returns whether this property is disabled.
     * <p>
     * A disabled property should not be edited and its context menu entries are hidden by default.
     * </p>
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Enables or disables this property. Disabled properties are hidden in context menus.
     *
     * @param disabled {@code true} to disable editing and hide menu entries
     * @return this property for chaining
     */
    public Property<T> setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    /**
     * Returns the optional input validator used by built-in text/numeric context menu entries.
     */
    public @Nullable ConsumingSupplier<String, Boolean> getUserInputTextValidator() {
        return userInputTextValidator;
    }

    /**
     * Sets the input validator used by built-in text/numeric context menu entries.
     *
     * @param userInputTextValidator validator that returns {@code true} for valid input
     * @return this property for chaining
     */
    public Property<T> setUserInputTextValidator(@Nullable ConsumingSupplier<String, Boolean> userInputTextValidator) {
        this.userInputTextValidator = userInputTextValidator;
        return this;
    }

    /**
     * Returns the optional set-processor that runs when the current value is set.
     */
    public @Nullable ConsumingSupplier<T, T> getValueSetProcessor() {
        return valueSetProcessor;
    }

    /**
     * Sets the processor that runs when the current value is set.
     * <p>
     * This is useful for normalization (clamping values, trimming text, etc.). The processor does
     * <strong>not</strong> apply to the default value.
     * </p>
     *
     * @param valueSetProcessor processor that receives and returns the value to store
     * @return this property for chaining
     */
    public Property<T> setValueSetProcessor(@Nullable ConsumingSupplier<T, T> valueSetProcessor) {
        this.valueSetProcessor = valueSetProcessor;
        return this;
    }

    /**
     * Returns the optional get-processor that runs when the current value is read.
     */
    public @Nullable ConsumingSupplier<T, T> getValueGetProcessor() {
        return valueGetProcessor;
    }

    /**
     * Sets the processor that runs when the current value is read.
     * <p>
     * This is useful for lazy conversion or computed return values while still keeping the stored
     * value untouched.
     * </p>
     *
     * @param valueGetProcessor processor that receives the stored value and returns the value to expose
     * @return this property for chaining
     */
    public Property<T> setValueGetProcessor(@Nullable ConsumingSupplier<T, T> valueGetProcessor) {
        this.valueGetProcessor = valueGetProcessor;
        return this;
    }

    public Property<T> addValueSetListener(@NotNull ValueSetListener<T> listener) {
        this.valueSetListeners.add(listener);
        return this;
    }

    /**
     * Returns the codec used to deserialize string data into the property type.
     */
    public @Nullable ConsumingSupplier<String, T> getDeserializationCodec() {
        return deserializationCodec;
    }

    /**
     * Sets the codec used to deserialize string data into the property type.
     *
     * @param deserializationCodec codec that converts a serialized string into a value
     * @return this property for chaining
     */
    public Property<T> setDeserializationCodec(@NotNull ConsumingSupplier<String, T> deserializationCodec) {
        this.deserializationCodec = deserializationCodec;
        return this;
    }

    /**
     * Returns the codec used to serialize the property value into a string.
     */
    public @Nullable ConsumingSupplier<T, String> getSerializationCodec() {
        return serializationCodec;
    }

    /**
     * Sets the codec used to serialize the property value into a string.
     *
     * @param serializationCodec codec that converts a value into a string
     * @return this property for chaining
     */
    public Property<T> setSerializationCodec(@NotNull ConsumingSupplier<T, String> serializationCodec) {
        this.serializationCodec = serializationCodec;
        return this;
    }

    /**
     * Deserializes this property from a {@link PropertyContainer}.
     * <p>
     * If the container does not contain the property key, the default value is used.
     * Any exceptions are logged and the property remains unchanged.
     * </p>
     *
     * @param properties container to read from
     * @return this property for chaining
     */
    public Property<T> deserialize(@NotNull PropertyContainer properties) {
        if (this.deserializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), new NullPointerException("No deserialization codec found for: " + this.getKey()));
            return this;
        }
        try {
            String serialized = properties.getValue(this.getKey());
            this.set((serialized != null) ? this.deserializationCodec.get(serialized) : this.defaultValue);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), ex);
        }
        return this;
    }

    /**
     * Serializes this property into a {@link PropertyContainer}.
     * <p>
     * The current value is serialized; if it is {@code null} the property is removed from the
     * container. Any exceptions are logged.
     * </p>
     *
     * @param properties container to write to
     * @return this property for chaining
     */
    public Property<T> serialize(@NotNull PropertyContainer properties) {
        if (this.serializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), new NullPointerException("No serialization codec found for: " + this.getKey()));
            return this;
        }
        try {
            properties.putProperty(this.getKey(), (this.currentValue == null) ? null : this.serializationCodec.get(this.currentValue));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
        }
        return this;
    }

    /**
     * Returns the translation key base used for context menu entry labels.
     */
    public @NotNull String getContextMenuEntryLocalizationKeyBase() {
        return contextMenuEntryLocalizationKeyBase;
    }

    /**
     * Sets the translation key base used for context menu entry labels.
     *
     * @param contextMenuEntryLocalizationKeyBase translation key base (non-null)
     * @return this property for chaining
     */
    public Property<T> setContextMenuEntryLocalizationKeyBase(@NotNull String contextMenuEntryLocalizationKeyBase) {
        this.contextMenuEntryLocalizationKeyBase = contextMenuEntryLocalizationKeyBase;
        return this;
    }

    /**
     * Sets the {@link ContextMenuEntrySupplier} used to build the context menu entry for this property.
     * <p>
     * For stack-aware behavior, use {@link ContextMenuBuilder} helper methods inside the supplier
     * implementation.
     * </p>
     *
     * @param contextMenuEntrySupplier supplier used to build a menu entry
     * @return this property for chaining
     */
    public Property<T> setContextMenuEntrySupplier(@NotNull ContextMenuEntrySupplier<? extends PropertyHolder, T> contextMenuEntrySupplier) {
        this.contextMenuEntrySupplier = contextMenuEntrySupplier;
        return this;
    }

    /**
     * Builds this property's context menu entry using the configured {@link ContextMenuEntrySupplier}.
     * <p>
     * The returned entry is automatically hidden when {@link #isDisabled()} is {@code true}.
     * </p>
     *
     * @param contextMenuBuilder builder supplying menu utilities and stack context
     * @param parentContextMenu parent menu that owns the entry
     * @param <H> the property holder type used by the builder
     * @return the built clickable entry
     * @throws NullPointerException if no supplier is configured
     */
    @SuppressWarnings("unchecked")
    public <H extends PropertyHolder> ContextMenu.ClickableContextMenuEntry<?> buildContextMenuEntry(@NotNull ContextMenuBuilder<H> contextMenuBuilder, @NotNull ContextMenu parentContextMenu) {
        Objects.requireNonNull(this.contextMenuEntrySupplier, "ContextMenuEntrySupplier is null! Can't build entry!");
        ContextMenuEntrySupplier<H, T> supplier = (ContextMenuEntrySupplier<H, T>) this.contextMenuEntrySupplier;
        ContextMenu.ClickableContextMenuEntry<?> entry = supplier.get(this, Objects.requireNonNull(contextMenuBuilder), Objects.requireNonNull(parentContextMenu));
        if (entry != null) entry.addIsVisibleSupplier((menu, entry1) -> !this.disabled);
        return entry;
    }

    /**
     * Builds this property's context menu entry and adds it to the given menu.
     *
     * @param addTo menu to add the entry to
     * @param contextMenuBuilder builder supplying menu utilities and stack context
     * @param <H> the property holder type used by the builder
     * @return the created entry (already added to the menu)
     */
    public <H extends PropertyHolder> ContextMenu.ClickableContextMenuEntry<?> buildContextMenuEntryAndAddTo(@NotNull ContextMenu addTo, @NotNull ContextMenuBuilder<H> contextMenuBuilder) {
        return addTo.addEntry(this.buildContextMenuEntry(contextMenuBuilder, addTo));
    }

    /**
     * Serializes the current value to a string using the configured codec.
     * <p>
     * This method mirrors {@link #serialize(PropertyContainer)} behavior but returns the serialized
     * string instead of writing to a container.
     * </p>
     *
     * @return serialized string or {@code null} if the current value is {@code null} or serialization fails
     */
    @Override
    @Nullable
    public String toString() {
        if (this.serializationCodec == null) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), new NullPointerException("No serialization codec found for: " + this.getKey()));
            return null;
        }
        try {
            return (this.currentValue == null) ? null : this.serializationCodec.get(this.currentValue);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
        }
        return null;
    }

    /**
     * Functional interface for building a context menu entry for a property.
     * <p>
     * Implementations can access the {@link ContextMenuBuilder} to construct stack-aware entries and
     * use {@link ContextMenuBuilder#getFilteredStackableObjectsList} for multi-select operations.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * property.setContextMenuEntrySupplier((prop, builder, menu) ->
     *     builder.buildGenericToggleContextMenuEntry(
     *         menu,
     *         "menu_entry_" + prop.getKey(),
     *         holder -> holder.getProperty(prop.getKey()) != null,
     *         holder -> ((Property<Boolean>) holder.getProperty(prop.getKey())).get(),
     *         (holder, value) -> ((Property<Boolean>) holder.getProperty(prop.getKey())).set(value),
     *         prop.getContextMenuEntryLocalizationKeyBase()
     *     )
     * );
     * }</pre>
     *
     * @param <H> the property holder type used by the menu builder
     * @param <T> the property value type
     */
    @FunctionalInterface
    public interface ContextMenuEntrySupplier<H extends PropertyHolder, T> {
        /**
         * Builds a clickable context menu entry for the given property.
         *
         * @param property the property being edited
         * @param contextMenuBuilder builder supplying menu utilities and stack context
         * @param parentContextMenu parent menu that owns the entry
         * @return a clickable entry (never {@code null})
         */
        @NotNull
        ContextMenu.ClickableContextMenuEntry<?> get(@NotNull Property<T> property, @NotNull ContextMenuBuilder<H> contextMenuBuilder, @NotNull ContextMenu parentContextMenu);
    }

    @FunctionalInterface
    public interface ValueSetListener<T> {
        void onSet(@Nullable T oldValue, @Nullable T newValue);
    }

    public static class ColorProperty extends Property<String> {

        @Nullable
        private DrawableColor cachedDrawable = null;
        @Nullable
        private String lastParsedHex = null;

        protected ColorProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        }

        protected ColorProperty(@NotNull String key, @Nullable String defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, contextMenuEntryLocalizationKeyBase);
        }

        @NotNull
        public String getHex() {
            String c = PlaceholderParser.replacePlaceholders(this.tryGetNonNullElse("#FFFFFF"));
            if (!Objects.equals(c, this.lastParsedHex) || (this.cachedDrawable == null)) {
                this.lastParsedHex = c;
                this.cachedDrawable = DrawableColor.of(c);
            }
            return c;
        }

        @NotNull
        public DrawableColor getDrawable() {
            this.getHex();
            return Objects.requireNonNullElse(this.cachedDrawable, DrawableColor.WHITE);
        }

    }

    public static class StringProperty extends Property<String> {

        @Nullable
        private String lastParsed = null;

        protected StringProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        }

        protected StringProperty(@NotNull String key, @Nullable String defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, contextMenuEntryLocalizationKeyBase);
        }

        @Nullable
        public String getString() {
            var s = this.get();
            if (s == null) return null;
            String parsed = PlaceholderParser.replacePlaceholders(s);
            if (!Objects.equals(parsed, this.lastParsed)) {
                this.lastParsed = parsed;
            }
            return parsed;
        }

    }

    protected abstract static class ManualInputProperty<T> extends Property<T> {

        @Nullable
        protected String manualInput = null;

        protected ManualInputProperty(@NotNull String key, @Nullable T defaultValue, @Nullable T currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        }

        protected ManualInputProperty(@NotNull String key, @Nullable T defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, contextMenuEntryLocalizationKeyBase);
        }

        @Nullable
        public String getManualInput() {
            return this.manualInput;
        }

        public boolean hasManualInput() {
            return (this.manualInput != null) && !this.manualInput.isEmpty();
        }

        @NotNull
        public ManualInputProperty<T> setManualInput(@Nullable String manualInput) {
            T oldValue = this.get();
            if ((manualInput == null) || manualInput.isEmpty()) {
                this.manualInput = null;
            } else {
                this.manualInput = manualInput;
            }
            T newValue = this.get();
            this.valueSetListeners.forEach(listener -> listener.onSet(oldValue, newValue));
            return this;
        }

        @NotNull
        public ManualInputProperty<T> clearManualInput() {
            return this.setManualInput(null);
        }

        protected void restoreSnapshot(@Nullable String manualInput, @Nullable T currentValue) {
            T oldValue = this.get();
            if ((manualInput == null) || manualInput.isEmpty()) {
                this.manualInput = null;
            } else {
                this.manualInput = manualInput;
            }
            this.currentValue = currentValue;
            T newValue = this.get();
            this.valueSetListeners.forEach(listener -> listener.onSet(oldValue, newValue));
        }

        @Nullable
        public String getRawInputOrFormattedValue() {
            if (this.manualInput != null) {
                return this.manualInput;
            }
            T value = super.get();
            if (value == null) value = this.getDefault();
            return (value != null) ? this.formatValue(value) : null;
        }

        @Nullable
        public String getAsString() {
            if (this.manualInput != null) {
                return PlaceholderParser.replacePlaceholders(this.manualInput);
            }
            T value = super.get();
            if (value == null) value = this.getDefault();
            return (value != null) ? this.formatValue(value) : null;
        }

        @Nullable
        protected T resolveManualValue() {
            if (this.manualInput == null) return null;
            String parsed = PlaceholderParser.replacePlaceholders(this.manualInput);
            return this.parseInput(parsed);
        }

        @Nullable
        protected abstract T parseInput(@NotNull String input);

        protected abstract boolean isSerializedValueValid(@NotNull String value);

        @Nullable
        protected abstract T parseSerializedValue(@NotNull String value);

        @NotNull
        protected String formatValue(@NotNull T value) {
            return value.toString();
        }

        @Override
        public @Nullable T get() {
            T manual = this.resolveManualValue();
            if (manual != null) return manual;
            return super.get();
        }

        @Override
        public T tryGetNonNull() {
            T manual = this.resolveManualValue();
            if (manual != null) return manual;
            return super.tryGetNonNull();
        }

        @Override
        public T tryGetNonNullElse(@NotNull T elseValue) {
            T manual = this.resolveManualValue();
            if (manual != null) return manual;
            return super.tryGetNonNullElse(elseValue);
        }

        @Override
        public boolean isDefault() {
            if (this.manualInput != null) return false;
            return super.isDefault();
        }

        @Override
        public Property<T> set(@Nullable T value) {
            this.manualInput = null;
            return super.set(value);
        }

        @Override
        public Property<T> deserialize(@NotNull PropertyContainer properties) {
            try {
                String serialized = properties.getValue(this.getKey());
                if (serialized == null) {
                    this.manualInput = null;
                    super.set(this.defaultValue);
                    return this;
                }
                if (this.isSerializedValueValid(serialized)) {
                    this.manualInput = null;
                    super.set(this.parseSerializedValue(serialized));
                } else {
                    this.manualInput = serialized;
                    super.set(this.defaultValue);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to deserialize property: " + this.getKey(), ex);
            }
            return this;
        }

        @Override
        public Property<T> serialize(@NotNull PropertyContainer properties) {
            if (this.serializationCodec == null) {
                LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), new NullPointerException("No serialization codec found for: " + this.getKey()));
                return this;
            }
            try {
                if (this.manualInput != null) {
                    properties.putProperty(this.getKey(), this.manualInput);
                } else {
                    properties.putProperty(this.getKey(), (this.currentValue == null) ? null : this.serializationCodec.get(this.currentValue));
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to serialize property: " + this.getKey(), ex);
            }
            return this;
        }

    }

    /**
     * {@link Property} optimized for {@link Integer}s with optional manual input.
     */
    public static class IntegerProperty extends ManualInputProperty<Integer> {

        protected IntegerProperty(@NotNull String key, @Nullable Integer defaultValue, @Nullable Integer currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        }

        protected IntegerProperty(@NotNull String key, @Nullable Integer defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, contextMenuEntryLocalizationKeyBase);
        }

        public int getInteger() {
            Integer value = this.get();
            if (value != null) return value;
            Integer def = this.getDefault();
            if (def != null) return def;
            return 1;
        }

        @Override
        protected @Nullable Integer parseInput(@NotNull String input) {
            if (MathUtils.isInteger(input)) return Integer.parseInt(input);
            return null;
        }

        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            return MathUtils.isInteger(value);
        }

        @Override
        protected @Nullable Integer parseSerializedValue(@NotNull String value) {
            return Integer.valueOf(value);
        }

    }

    /**
     * {@link Property} optimized for {@link Long}s with optional manual input.
     */
    public static class LongProperty extends ManualInputProperty<Long> {

        protected LongProperty(@NotNull String key, @Nullable Long defaultValue, @Nullable Long currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        }

        protected LongProperty(@NotNull String key, @Nullable Long defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, contextMenuEntryLocalizationKeyBase);
        }

        public long getLong() {
            Long value = this.get();
            if (value != null) return value;
            Long def = this.getDefault();
            if (def != null) return def;
            return 1L;
        }

        @Override
        protected @Nullable Long parseInput(@NotNull String input) {
            if (MathUtils.isLong(input)) return Long.parseLong(input);
            return null;
        }

        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            return MathUtils.isLong(value);
        }

        @Override
        protected @Nullable Long parseSerializedValue(@NotNull String value) {
            return Long.valueOf(value);
        }

    }

    /**
     * {@link Property} optimized for {@link Float}s with optional manual input.
     */
    public static class FloatProperty extends ManualInputProperty<Float> {

        protected FloatProperty(@NotNull String key, @Nullable Float defaultValue, @Nullable Float currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        }

        protected FloatProperty(@NotNull String key, @Nullable Float defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, contextMenuEntryLocalizationKeyBase);
        }

        public float getFloat() {
            Float value = this.get();
            if (value != null) return value;
            Float def = this.getDefault();
            if (def != null) return def;
            return 1.0F;
        }

        @Override
        protected @Nullable Float parseInput(@NotNull String input) {
            if (MathUtils.isFloat(input)) return Float.parseFloat(input);
            return null;
        }

        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            return MathUtils.isFloat(value);
        }

        @Override
        protected @Nullable Float parseSerializedValue(@NotNull String value) {
            return Float.valueOf(value);
        }

    }

    /**
     * {@link Property} optimized for {@link Double}s with optional manual input.
     */
    public static class DoubleProperty extends ManualInputProperty<Double> {

        protected DoubleProperty(@NotNull String key, @Nullable Double defaultValue, @Nullable Double currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        }

        protected DoubleProperty(@NotNull String key, @Nullable Double defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, contextMenuEntryLocalizationKeyBase);
        }

        public double getDouble() {
            Double value = this.get();
            if (value != null) return value;
            Double def = this.getDefault();
            if (def != null) return def;
            return 1.0D;
        }

        @Override
        protected @Nullable Double parseInput(@NotNull String input) {
            if (MathUtils.isDouble(input)) return Double.parseDouble(input);
            return null;
        }

        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            return MathUtils.isDouble(value);
        }

        @Override
        protected @Nullable Double parseSerializedValue(@NotNull String value) {
            return Double.valueOf(value);
        }

    }

    /**
     * {@link Property} optimized for {@link Boolean}s with optional manual input.
     */
    public static class BooleanProperty extends ManualInputProperty<Boolean> {

        protected BooleanProperty(@NotNull String key, @Nullable Boolean defaultValue, @Nullable Boolean currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        }

        protected BooleanProperty(@NotNull String key, @Nullable Boolean defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
            super(key, defaultValue, contextMenuEntryLocalizationKeyBase);
        }

        public boolean getBoolean() {
            Boolean value = this.get();
            if (value != null) return value;
            Boolean def = this.getDefault();
            if (def != null) return def;
            return false;
        }

        @Override
        protected @Nullable Boolean parseInput(@NotNull String input) {
            String trimmed = input.trim();
            if ("true".equalsIgnoreCase(trimmed)) return true;
            if ("false".equalsIgnoreCase(trimmed)) return false;
            return null;
        }

        @Override
        protected boolean isSerializedValueValid(@NotNull String value) {
            String trimmed = value.trim();
            return "true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed);
        }

        @Override
        protected @Nullable Boolean parseSerializedValue(@NotNull String value) {
            String trimmed = value.trim();
            if ("true".equalsIgnoreCase(trimmed)) return true;
            if ("false".equalsIgnoreCase(trimmed)) return false;
            return null;
        }

    }

}

package de.keksuccino.fancymenu.util.properties;

import de.keksuccino.fancymenu.customization.action.blocks.AbstractExecutableBlock;
import de.keksuccino.fancymenu.customization.action.blocks.ExecutableBlockDeserializer;
import de.keksuccino.fancymenu.customization.action.blocks.GenericExecutableBlock;
import de.keksuccino.fancymenu.customization.action.ui.ActionScriptEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.requirement.internal.RequirementContainer;
import de.keksuccino.fancymenu.customization.requirement.ui.ManageRequirementsScreen;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.ObjectUtils;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ColorPickerScreen;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
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
                null, multiLine, placeholders, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, property.getDefault(), property.userInputTextValidator, null);
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

    /**
     * Creates an integer {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Integer#valueOf(String)} and uses a numeric
     * input entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Property<Integer> width = Property.integerProperty(
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
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue, int currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Integer> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Integer::valueOf;
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            Integer defaultVal = property.getDefault();
            int resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            return builder.buildGenericIntegerInputContextMenuEntry(menu, "menu_entry_" + key,
                    consumes -> consumes.getProperty(key) != null,
                    consumes -> {
                        Property<Integer> resolved = (Property<Integer>) consumes.getProperty(key);
                        return (resolved != null) ? resolved.get() : defaultVal;
                    },
                    (b, value) -> {
                        Property<Integer> resolved = (Property<Integer>) b.getProperty(key);
                        if (resolved != null) resolved.set(value);
                    },
                    Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, resolvedDefault, property.userInputTextValidator, null);
        };
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #integerProperty(String, int, int, String)
     */
    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return integerProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Creates a double {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Double#valueOf(String)} and uses a numeric
     * input entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Property<Double> opacity = Property.doubleProperty(
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
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue, double currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Double> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Double::valueOf;
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            Double defaultVal = property.getDefault();
            double resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            return builder.buildGenericDoubleInputContextMenuEntry(menu, "menu_entry_" + key,
                    consumes -> consumes.getProperty(key) != null,
                    consumes -> {
                        Property<Double> resolved = (Property<Double>) consumes.getProperty(key);
                        return (resolved != null) ? resolved.get() : defaultVal;
                    },
                    (b, value) -> {
                        Property<Double> resolved = (Property<Double>) b.getProperty(key);
                        if (resolved != null) resolved.set(value);
                    },
                    Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, resolvedDefault, property.userInputTextValidator, null);
        };
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #doubleProperty(String, double, double, String)
     */
    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return doubleProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Creates a long {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Long#valueOf(String)} and uses a numeric
     * input entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Property<Long> seed = Property.longProperty(
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
    public static Property<Long> longProperty(@NotNull String key, long defaultValue, long currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Long> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Long::valueOf;
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            Long defaultVal = property.getDefault();
            long resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            return builder.buildGenericLongInputContextMenuEntry(menu, "menu_entry_" + key,
                    consumes -> consumes.getProperty(key) != null,
                    consumes -> {
                        Property<Long> resolved = (Property<Long>) consumes.getProperty(key);
                        return (resolved != null) ? resolved.get() : defaultVal;
                    },
                    (b, value) -> {
                        Property<Long> resolved = (Property<Long>) b.getProperty(key);
                        if (resolved != null) resolved.set(value);
                    },
                    Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, resolvedDefault, property.userInputTextValidator, null);
        };
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #longProperty(String, long, long, String)
     */
    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return longProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    /**
     * Creates a float {@link Property} with an explicit default and current value.
     * <p>
     * The property serializes/deserializes using {@link Float#valueOf(String)} and uses a numeric
     * input entry in context menus (stack-aware).
     * </p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * Property<Float> scale = Property.floatProperty(
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
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue, float currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Float> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Float::valueOf;
        p.contextMenuEntrySupplier = (property, builder, menu) -> {
            Float defaultVal = property.getDefault();
            float resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            return builder.buildGenericFloatInputContextMenuEntry(menu, "menu_entry_" + key,
                    consumes -> consumes.getProperty(key) != null,
                    consumes -> {
                        Property<Float> resolved = (Property<Float>) consumes.getProperty(key);
                        return (resolved != null) ? resolved.get() : defaultVal;
                    },
                    (b, value) -> {
                        Property<Float> resolved = (Property<Float>) b.getProperty(key);
                        if (resolved != null) resolved.set(value);
                    },
                    Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, resolvedDefault, property.userInputTextValidator, null);
        };
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #floatProperty(String, float, float, String)
     */
    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return floatProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
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
     * Property<Boolean> enabled = Property.booleanProperty(
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
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue, boolean currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Boolean> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Boolean::valueOf;
        p.contextMenuEntrySupplier = (property, builder, menu) -> builder.buildGenericToggleContextMenuEntry(menu, "menu_entry_" + key,
                consumes -> consumes.getProperty(key) != null,
                consumes -> {
                    Property<Boolean> resolved = (Property<Boolean>) consumes.getProperty(key);
                    Boolean value = (resolved != null) ? resolved.get() : property.getDefault();
                    return (value != null) ? value : property.getDefault();
                },
                (b, value) -> {
                    Property<Boolean> resolved = (Property<Boolean>) b.getProperty(key);
                    if (resolved != null) {
                        resolved.set(value);
                    }
                },
                property.getContextMenuEntryLocalizationKeyBase());
        return p;
    }

    /**
     * Convenience overload that uses {@code defaultValue} as the initial current value.
     *
     * @see #booleanProperty(String, boolean, boolean, String)
     */
    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
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
                    () -> ResourceChooserScreen.generic(null, null, file -> {}),
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
                    Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb);
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
                        Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileFilter, allowLocation, allowLocal, allowWeb);
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
                        Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileFilter, allowLocation, allowLocal, allowWeb);
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
                        Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileFilter, allowLocation, allowLocal, allowWeb);
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
                        Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), true, fileFilter, allowLocation, allowLocal, allowWeb);
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
                                    TextEditorScreen s = new TextEditorScreen(Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), null, call -> {
                                        if (call != null) {
                                            builder.saveSnapshot();
                                            builder.applyStackAppliers(entry, call);
                                        }
                                    });
                                    s.setText(defaultText);
                                    s.setMultilineMode(false);
                                    s.setPlaceholdersAllowed(true);
                                    contextMenu.closeMenuChain();
                                    Dialogs.openGeneric(s, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), ContextMenu.IconFactory.getIcon("text"), TextEditorScreen.PIP_WINDOW_WIDTH, TextEditorScreen.PIP_WINDOW_HEIGHT);
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
                        .setIcon(ContextMenu.IconFactory.getIcon("text"));

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
                                ColorPickerScreen picker = new ColorPickerScreen(preset,
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
                                        .setMinSize(ColorPickerScreen.PIP_WINDOW_WIDTH, ColorPickerScreen.PIP_WINDOW_HEIGHT)
                                        .setSize(ColorPickerScreen.PIP_WINDOW_WIDTH, ColorPickerScreen.PIP_WINDOW_HEIGHT);
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
                    .setIcon(ContextMenu.IconFactory.getIcon("pipette"));

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
                    .setIcon(ContextMenu.IconFactory.getIcon("undo"));

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
                    .setIcon(ContextMenu.IconFactory.getIcon("info"));

            return new ContextMenu.SubMenuContextMenuEntry("menu_entry_" + key, menu, Component.translatable(property.getContextMenuEntryLocalizationKeyBase()), subMenu)
                    .setIcon(ContextMenu.IconFactory.getIcon("color_palette"))
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
                    builder.openContextMenuScreen(builder.getContextMenuCallbackScreen());
                });
                builder.openContextMenuScreen(s);
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
                    builder.openContextMenuScreen(builder.getContextMenuCallbackScreen());
                });
                if (allEqual) {
                    builder.openContextMenuScreen(s);
                } else {
                    Dialogs.openMessageWithCallback(Component.translatable("fancymenu.requirements.multiselect.warning.override"), MessageDialogStyle.WARNING, call -> {
                        if (call) {
                            builder.openContextMenuScreen(s);
                        }
                    });
                }
            }
        }).setStackable(true);
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
     * The context menu entry opens {@link ActionScriptEditorScreen}. When used in stacked context
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
                ActionScriptEditorScreen s = new ActionScriptEditorScreen(block, (call) -> {
                    if (call != null) {
                        builder.saveSnapshot();
                        if (resolved != null) {
                            resolved.set(call);
                        }
                    }
                });
                contextMenu.closeMenuChain();
                ActionScriptEditorScreen.openInWindow(s);
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
                ActionScriptEditorScreen s = new ActionScriptEditorScreen(blockToUseInManager, (call) -> {
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
                    ActionScriptEditorScreen.openInWindow(s);
                } else {
                    Dialogs.openMessageWithCallback(Component.translatable("fancymenu.actions.multiselect.warning.override"), MessageDialogStyle.WARNING, call -> {
                        if (call) {
                            contextMenu.closeMenuChain();
                            ActionScriptEditorScreen.openInWindow(s);
                        }
                    });
                }
            }
        }).setStackable(true);
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

}

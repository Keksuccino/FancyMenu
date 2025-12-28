package de.keksuccino.fancymenu.util.properties;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.file.FileFilter;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.FileType;
import de.keksuccino.fancymenu.util.file.type.groups.FileTypeGroup;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenuBuilder;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu.ContextMenuEntry;
import de.keksuccino.fancymenu.util.rendering.ui.screen.resource.ResourceChooserScreen;
import de.keksuccino.fancymenu.util.resource.Resource;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.audio.IAudio;
import de.keksuccino.fancymenu.util.resource.resources.text.IText;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.video.IVideo;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

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
    protected boolean disabled = false;
    @Nullable
    protected ConsumingSupplier<String, Boolean> userInputTextValidator = null;
    @Nullable
    protected ConsumingSupplier<T, T> valueSetProcessor = null;
    @Nullable
    protected ConsumingSupplier<T, T> valueGetProcessor = null;

    @NotNull
    public static Property<String> stringProperty(@NotNull String key, @Nullable String defaultValue, @Nullable String currentValue, boolean multiLine, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<String> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = consumes -> consumes;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            return builder.buildStringInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<String> resolved = (Property<String>) consumes.getProperty(key);
                if (resolved != null) return resolved.get();
                return defaultValue;
            }, (b, s) -> {
                Property<String> resolved = (Property<String>) b.getProperty(key);
                if (resolved != null) resolved.set(s);
            }, null, multiLine, placeholders, Component.translatable(contextMenuEntryLocalizationKeyBase), true, property.getDefault(), property.userInputTextValidator, null);
        };
        return p;
    }

    @NotNull
    public static Property<String> stringProperty(@NotNull String key, @Nullable String defaultValue, boolean multiLine, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return stringProperty(key, defaultValue, defaultValue, multiLine, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue, int currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Integer> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Integer::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            Integer defaultVal = property.getDefault();
            int resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            return builder.buildIntegerInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<Integer> resolved = (Property<Integer>) consumes.getProperty(key);
                return (resolved != null) ? resolved.get() : defaultVal;
            }, (b, value) -> {
                Property<Integer> resolved = (Property<Integer>) b.getProperty(key);
                if (resolved != null) resolved.set(value);
            }, Component.translatable(contextMenuEntryLocalizationKeyBase), true, resolvedDefault, property.userInputTextValidator, null);
        };
        return p;
    }

    @NotNull
    public static Property<Integer> integerProperty(@NotNull String key, int defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return integerProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue, double currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Double> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Double::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            Double defaultVal = property.getDefault();
            double resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            return builder.buildDoubleInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<Double> resolved = (Property<Double>) consumes.getProperty(key);
                return (resolved != null) ? resolved.get() : defaultVal;
            }, (b, value) -> {
                Property<Double> resolved = (Property<Double>) b.getProperty(key);
                if (resolved != null) resolved.set(value);
            }, Component.translatable(contextMenuEntryLocalizationKeyBase), true, resolvedDefault, property.userInputTextValidator, null);
        };
        return p;
    }

    @NotNull
    public static Property<Double> doubleProperty(@NotNull String key, double defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return doubleProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue, long currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Long> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Long::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            Long defaultVal = property.getDefault();
            long resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            return builder.buildLongInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<Long> resolved = (Property<Long>) consumes.getProperty(key);
                return (resolved != null) ? resolved.get() : defaultVal;
            }, (b, value) -> {
                Property<Long> resolved = (Property<Long>) b.getProperty(key);
                if (resolved != null) resolved.set(value);
            }, Component.translatable(contextMenuEntryLocalizationKeyBase), true, resolvedDefault, property.userInputTextValidator, null);
        };
        return p;
    }

    @NotNull
    public static Property<Long> longProperty(@NotNull String key, long defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return longProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue, float currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Float> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Float::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            Float defaultVal = property.getDefault();
            float resolvedDefault = (defaultVal != null) ? defaultVal : defaultValue;
            return builder.buildFloatInputContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
                Property<Float> resolved = (Property<Float>) consumes.getProperty(key);
                return (resolved != null) ? resolved.get() : defaultVal;
            }, (b, value) -> {
                Property<Float> resolved = (Property<Float>) b.getProperty(key);
                if (resolved != null) resolved.set(value);
            }, Component.translatable(contextMenuEntryLocalizationKeyBase), true, resolvedDefault, property.userInputTextValidator, null);
        };
        return p;
    }

    @NotNull
    public static Property<Float> floatProperty(@NotNull String key, float defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return floatProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue, boolean currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<Boolean> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = Boolean::valueOf;
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> builder.buildToggleContextMenuEntry(menu, "menu_entry_" + key, type, consumes -> {
            Property<Boolean> resolved = (Property<Boolean>) consumes.getProperty(key);
            return (resolved != null) ? resolved.get() : property.getDefault();
        }, (b, value) -> {
            Property<Boolean> resolved = (Property<Boolean>) b.getProperty(key);
            if (resolved != null) resolved.set(value);
        }, contextMenuEntryLocalizationKeyBase);
        return p;
    }

    @NotNull
    public static Property<Boolean> booleanProperty(@NotNull String key, boolean defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return booleanProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue, @Nullable ResourceSource currentValue, @NotNull String contextMenuEntryLocalizationKeyBase, boolean allowLocal, boolean allowWeb, boolean allowLocation, @Nullable FileFilter fileFilter, @Nullable FileTypeGroup<FileType<Resource>> fileTypes, @NotNull FileMediaType fileMediaType) {
        Property<ResourceSource> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = ResourceSource::of;
        p.serializationCodec = consumes -> {
            if (consumes == null) return null;
            return consumes.getSerializationSource();
        };
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            ResourceSupplier<Resource> defaultSupplier = null;
            ResourceSource defaultSource = property.getDefault();
            if (defaultSource != null) {
                defaultSupplier = new ResourceSupplier<>(Resource.class, fileMediaType, defaultSource.getSourceWithPrefix());
            }
            return builder.buildGenericResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
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
                    Component.translatable(contextMenuEntryLocalizationKeyBase), true, fileTypes, fileFilter, allowLocation, allowLocal, allowWeb);
        };
        return p;
    }

    @NotNull
    public static Property<ResourceSource> resourceSourceProperty(@NotNull String key, @Nullable ResourceSource defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase, boolean allowLocal, boolean allowWeb, boolean allowLocation, @Nullable FileFilter fileFilter, @Nullable FileTypeGroup<FileType<Resource>> fileTypes, @NotNull FileMediaType fileMediaType) {
        return resourceSourceProperty(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, allowLocal, allowWeb, allowLocation, fileFilter, fileTypes, fileMediaType);
    }

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
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> {
            if (ITexture.class.isAssignableFrom(resourceType)) {
                return builder.buildImageResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
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
                        Component.translatable(contextMenuEntryLocalizationKeyBase), true, fileFilter, allowLocation, allowLocal, allowWeb);
            }
            if (IAudio.class.isAssignableFrom(resourceType)) {
                return builder.buildAudioResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
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
                        Component.translatable(contextMenuEntryLocalizationKeyBase), true, fileFilter, allowLocation, allowLocal, allowWeb);
            }
            if (IVideo.class.isAssignableFrom(resourceType)) {
                return builder.buildVideoResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
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
                        Component.translatable(contextMenuEntryLocalizationKeyBase), true, fileFilter, allowLocation, allowLocal, allowWeb);
            }
            if (IText.class.isAssignableFrom(resourceType)) {
                return builder.buildTextResourceChooserContextMenuEntry(menu, "menu_entry_" + key, type,
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
                        Component.translatable(contextMenuEntryLocalizationKeyBase), true, fileFilter, allowLocation, allowLocal, allowWeb);
            }
            throw new IllegalArgumentException("Unknown resource format! Unable to build ResourceSupplier context menu entry!");
        };
        return p;
    }

    @NotNull
    public static <R extends Resource> Property<ResourceSupplier<R>> resourceSupplierProperty(@NotNull Class<R> resourceType, @NotNull String key, @Nullable ResourceSupplier<R> defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase, boolean allowLocal, boolean allowWeb, boolean allowLocation, @Nullable FileFilter fileFilter) {
        return resourceSupplierProperty(resourceType, key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase, allowLocal, allowWeb, allowLocation, fileFilter);
    }

    @NotNull
    public static Property<DrawableColor> drawableColorProperty(@NotNull String key, @Nullable DrawableColor defaultValue, @Nullable DrawableColor currentValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        Property<DrawableColor> p = new Property<>(key, defaultValue, currentValue, contextMenuEntryLocalizationKeyBase);
        p.deserializationCodec = DrawableColor::of;
        p.serializationCodec = consumes -> {
            if (consumes == null) return null;
            return consumes.getHex();
        };
        p.contextMenuEntrySupplier = (type, property, builder, menu) -> builder.buildStringInputContextMenuEntry(menu, "menu_entry_" + key, type,
                consumes -> {
                    Property<DrawableColor> resolved = (Property<DrawableColor>) consumes.getProperty(key);
                    DrawableColor value = (resolved != null) ? resolved.get() : property.getDefault();
                    return (value != null) ? value.getHex() : null;
                },
                (b, s) -> {
                    if (s == null) return;
                    DrawableColor parsed = DrawableColor.of(s);
                    if (parsed != DrawableColor.EMPTY) {
                        Property<DrawableColor> resolved = (Property<DrawableColor>) b.getProperty(key);
                        if (resolved != null) resolved.set(parsed);
                    }
                },
                null, false, placeholders, Component.translatable(contextMenuEntryLocalizationKeyBase), true,
                (property.getDefault() != null) ? property.getDefault().getHex() : null,
                (!placeholders) ? TextValidators.HEX_COLOR_TEXT_VALIDATOR : null, null);
        return p;
    }

    @NotNull
    public static Property<DrawableColor> drawableColorProperty(@NotNull String key, @Nullable DrawableColor defaultValue, boolean placeholders, @NotNull String contextMenuEntryLocalizationKeyBase) {
        return drawableColorProperty(key, defaultValue, defaultValue, placeholders, contextMenuEntryLocalizationKeyBase);
    }

    protected Property(@NotNull String key, @Nullable T defaultValue, @Nullable T currentValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        this.key = Objects.requireNonNull(key);
        this.setDefault(defaultValue);
        this.set(currentValue);
        this.contextMenuEntryLocalizationKeyBase = Objects.requireNonNull(contextMenuEntryLocalizationKeyBase);
    }

    protected Property(@NotNull String key, @Nullable T defaultValue, @NotNull String contextMenuEntryLocalizationKeyBase) {
        this(key, defaultValue, defaultValue, contextMenuEntryLocalizationKeyBase);
    }

    public @NotNull String getKey() {
        return key;
    }

    public @Nullable T getDefault() {
        return defaultValue;
    }

    public Property<T> setDefault(@Nullable T value) {
        this.defaultValue = value;
        return this;
    }

    public boolean isDefault() {
        return Objects.equals(this.defaultValue, this.currentValue);
    }

    public @Nullable T get() {
        return this.processGet(this.currentValue);
    }

    /**
     * First tries to return the current value if it is not null, else will try to return the default value, and will throw an error if both are null.
     */
    public T tryGetNonNull() {
        var current = this.processGet(this.currentValue);
        if (current != null) return current;
        return Objects.requireNonNull(this.processGet(this.defaultValue));
    }

    /**
     * First tries to return the current value if it is not null, then tries to return the default value. If both are null, it will return {@code elseValue}.
     */
    public T tryGetNonNullElse(@NotNull T elseValue) {
        var current = this.processGet(this.currentValue);
        if (current != null) return current;
        return Objects.requireNonNullElse(this.processGet(this.defaultValue), elseValue);
    }

    protected T processGet(@Nullable T get) {
        if (get == null) return null;
        if (this.valueGetProcessor != null) return this.valueGetProcessor.get(get);
        return get;
    }

    public Property<T> set(@Nullable T value) {
        if (value != null) {
            if (this.valueSetProcessor != null) value = this.valueSetProcessor.get(value);
        }
        this.currentValue = value;
        return this;
    }

    public Property<T> forceSet(@Nullable Object value) {
        if (value != null) {
            if (this.valueSetProcessor != null) value = this.valueSetProcessor.get((T) value);
        }
        this.currentValue = (T) value;
        return this;
    }

    /**
     * If this property is set to disabled, which means you should not update its value, wire it in UIs or make any other changes to it.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Disabling the property causes {@link ContextMenuEntry}s of the property to be invisible in {@link ContextMenu}s, so users can't edit its value anymore.
     */
    public Property<T> setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public @Nullable ConsumingSupplier<String, Boolean> getUserInputTextValidator() {
        return userInputTextValidator;
    }

    public Property<T> setUserInputTextValidator(@Nullable ConsumingSupplier<String, Boolean> userInputTextValidator) {
        this.userInputTextValidator = userInputTextValidator;
        return this;
    }

    public @Nullable ConsumingSupplier<T, T> getValueSetProcessor() {
        return valueSetProcessor;
    }

    /**
     * When the value of this property gets set, it goes through this processor if one is set. This makes it possible to, well, process the value before it actually gets set.<br>
     * Does NOT get applied to default values set via {@link Property#setDefault(Object)}!
     */
    public Property<T> setValueSetProcessor(@Nullable ConsumingSupplier<T, T> valueSetProcessor) {
        this.valueSetProcessor = valueSetProcessor;
        return this;
    }

    public @Nullable ConsumingSupplier<T, T> getValueGetProcessor() {
        return valueGetProcessor;
    }

    /**
     * When the value of this property is returned by the getters, it goes through this processor if one is set. This makes it possible to, well, process the value before it actually gets returned.
     */
    public Property<T> setValueGetProcessor(@Nullable ConsumingSupplier<T, T> valueGetProcessor) {
        this.valueGetProcessor = valueGetProcessor;
        return this;
    }

    public @Nullable ConsumingSupplier<String, T> getDeserializationCodec() {
        return deserializationCodec;
    }

    public Property<T> setDeserializationCodec(@NotNull ConsumingSupplier<String, T> deserializationCodec) {
        this.deserializationCodec = deserializationCodec;
        return this;
    }

    public @Nullable ConsumingSupplier<T, String> getSerializationCodec() {
        return serializationCodec;
    }

    public Property<T> setSerializationCodec(@NotNull ConsumingSupplier<T, String> serializationCodec) {
        this.serializationCodec = serializationCodec;
        return this;
    }

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

    public @NotNull String getContextMenuEntryLocalizationKeyBase() {
        return contextMenuEntryLocalizationKeyBase;
    }

    public Property<T> setContextMenuEntryLocalizationKeyBase(@NotNull String contextMenuEntryLocalizationKeyBase) {
        this.contextMenuEntryLocalizationKeyBase = contextMenuEntryLocalizationKeyBase;
        return this;
    }

    public Property<T> setContextMenuEntrySupplier(@NotNull ContextMenuEntrySupplier<? extends PropertyHolder, T> contextMenuEntrySupplier) {
        this.contextMenuEntrySupplier = contextMenuEntrySupplier;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <H extends PropertyHolder> ContextMenu.ClickableContextMenuEntry<?> buildContextMenuEntry(@NotNull Class<H> propertyHolderType, @NotNull ContextMenuBuilder<H> contextMenuBuilder, @NotNull ContextMenu parentContextMenu) {
        Objects.requireNonNull(this.contextMenuEntrySupplier, "ContextMenuEntrySupplier is null! Can't build entry!");
        ContextMenuEntrySupplier<H, T> supplier = (ContextMenuEntrySupplier<H, T>) this.contextMenuEntrySupplier;
        ContextMenu.ClickableContextMenuEntry<?> entry = supplier.get(Objects.requireNonNull(propertyHolderType), this, Objects.requireNonNull(contextMenuBuilder), Objects.requireNonNull(parentContextMenu));
        if (entry != null) entry.addIsVisibleSupplier((menu, entry1) -> !this.disabled);
        return entry;
    }

    public <H extends PropertyHolder> ContextMenu.ClickableContextMenuEntry<?> buildContextMenuEntryAndAddTo(@NotNull ContextMenu addTo, @NotNull Class<H> propertyHolderType, @NotNull ContextMenuBuilder<H> contextMenuBuilder) {
        return addTo.addEntry(this.buildContextMenuEntry(propertyHolderType, contextMenuBuilder, addTo));
    }

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

    @FunctionalInterface
    public interface ContextMenuEntrySupplier<H extends PropertyHolder, T> {
        @NotNull
        ContextMenu.ClickableContextMenuEntry<?> get(@NotNull Class<H> propertyHolderType, @NotNull Property<T> property, @NotNull ContextMenuBuilder<H> contextMenuBuilder, @NotNull ContextMenu parentContextMenu);
    }

}
